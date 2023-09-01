package streams

import dataclasses.AlertData
import dataclasses.AlertDataDeserializer
import functions.AlertDataProcessFunction
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.datastream.DataStreamSource
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import java.time.Duration

open class AlertProcessingStream {

    private fun buildKafkaSourceStream(env: StreamExecutionEnvironment): DataStreamSource<AlertData> {
        val kafkaSource = KafkaSource.builder<AlertData>()
            .setBootstrapServers("localhost:9092")
            .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
            .setDeserializer(AlertDataDeserializer())
            .build()
        WatermarkStrategy.forMonotonousTimestamps<>()
        val watermarkStrategy = WatermarkStrategy.forBoundedOutOfOrderness<AlertData>(Duration.ofMinutes(1))
            .withTimestampAssigner { event, _ -> event.timestamp }
        return env.fromSource(kafkaSource, watermarkStrategy, "source")
    }

    private fun filterStream(stream: DataStreamSource<AlertData>): SingleOutputStreamOperator<AlertData> {
        return stream.filter { it.timestamp > 0 }
    }

    fun handleStream(stream: SingleOutputStreamOperator<AlertData>): SingleOutputStreamOperator<AlertData> {
        val keyed = stream.keyBy({ it.id }, TypeInformation.of(String::class.java))
        return keyed.process(AlertDataProcessFunction())
    }

    fun runStream(env: StreamExecutionEnvironment) {
        val stream = buildKafkaSourceStream(env)
        val filtered = filterStream(stream)
        val alerts = handleStream(filtered)
        alerts.print()
    }
}
