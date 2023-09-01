package dataclasses

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema
import org.apache.flink.util.Collector
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.log4j.Logger

data class AlertData(val id: String, val timestamp: Long, val message: String, val type: String) {
    fun isError(): Boolean {
        return type == "ERROR"
    }
}

class AlertDataDeserializer : KafkaRecordDeserializationSchema<AlertData> {

    private val logger: Logger = Logger.getLogger(AlertData::class.java)

    private val objectMapper: ObjectMapper = JsonMapper.builder()
        .addModule(JavaTimeModule())
        .addModule(KotlinModule.Builder().build())
        .build()

    override fun getProducedType(): TypeInformation<AlertData> {
        return TypeInformation.of(AlertData::class.java)
    }

    override fun deserialize(record: ConsumerRecord<ByteArray, ByteArray>, out: Collector<AlertData>) {
        try {
            val event = objectMapper.readValue(record.value(), AlertData::class.java)
            out.collect(event)
        } catch (e: Exception) {
            logger.error("Caught an error during deserialization", e)
        }
    }
}
