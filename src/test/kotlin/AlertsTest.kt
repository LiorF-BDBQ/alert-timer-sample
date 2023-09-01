import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dataclasses.AlertData
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.junit.Before
import org.junit.Test
import streams.AlertProcessingStream
import java.io.File
import java.time.Duration

internal class AlertsTest: AlertProcessingStream() {
    private lateinit var env: LocalStreamEnvironment

    @Before
    fun setUp() {
        env = LocalStreamEnvironment.createLocalEnvironment()
        env.parallelism = 1
    }


    @Test
    fun `Short alert test 1, other devices arrive`() {
        val stream = getFixtureStream("alertData1.jsonl")
        val output = handleStream(stream)
        val values = output.executeAndCollect(1)
        assert(values.size == 1)
    }

    @Test
    fun `No alert arrives`() {
        val stream = getFixtureStream("alertData2.jsonl")
        val output = handleStream(stream)
        val values = output.executeAndCollect(1)
        assert(values.size == 0)
    }

    private fun getFixtureStream(fixturesFileName: String): SingleOutputStreamOperator<AlertData> {
        val source = env.addSource(FixtureSource(fixturesFileName))
        val watermarkStrategy = WatermarkStrategy
            .forBoundedOutOfOrderness<AlertData>(Duration.ofSeconds(20))
            .withTimestampAssigner { event: AlertData, _: Long -> event.timestamp }
        return source.assignTimestampsAndWatermarks(
            watermarkStrategy
        )
    }

    class FixtureSource(private val fixturesFileName: String) : RichParallelSourceFunction<AlertData>() {
        companion object {
            val mapper: ObjectMapper = JsonMapper.builder()
            .addModule(JavaTimeModule())
            .addModule(KotlinModule.Builder().build())
            .build()
        }

        @Volatile
        private var running = true

        @Throws(Exception::class)
        override fun run(ctx: SourceFunction.SourceContext<AlertData>) {
            val resource = AlertsTest::class.java.classLoader.getResource(fixturesFileName)
            val file = File(resource!!.file)
            file.forEachLine {
                if (it.isNotEmpty()) {
                    ctx.collect(mapper.readValue(it, AlertData::class.java))
                }
            }
        }

        override fun cancel() {
            running = false
        }
    }


}
