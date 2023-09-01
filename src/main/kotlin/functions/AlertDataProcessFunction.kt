package functions

import dataclasses.AlertData
import org.apache.flink.api.common.state.ValueState
import org.apache.flink.api.common.state.ValueStateDescriptor
import org.apache.flink.api.common.time.Time
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

class AlertDataProcessFunction : KeyedProcessFunction<String, AlertData, AlertData>() {

    private lateinit var state: ValueState<AlertData?>
    private val timeoutMillis = Time.minutes(10).toMilliseconds()

    override fun open(parameters: Configuration) {
        val descriptor = ValueStateDescriptor("alertDataValueState", AlertData::class.java)
        state = runtimeContext.getState(descriptor)
    }

    override fun processElement(value: AlertData, ctx: Context, out: Collector<AlertData>) {
        val currentValue = state.value()
        if (currentValue != null) {
            if (!value.isError()) {
                state.clear()
                ctx.timerService().deleteEventTimeTimer(currentValue.timestamp + timeoutMillis)
            }
        } else if (value.isError()) {
            state.update(value)
            ctx.timerService().registerEventTimeTimer(value.timestamp + timeoutMillis)
        }
    }

    override fun onTimer(timestamp: Long, ctx: OnTimerContext, out: Collector<AlertData>) {
        val currentValue = state.value()
        if (currentValue != null) {
            out.collect(currentValue)
        }
        state.clear()
    }
}
