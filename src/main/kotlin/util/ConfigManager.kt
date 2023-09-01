package util

import org.apache.flink.api.common.time.Time
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment


class ConfigManager {
    companion object{
        fun configureEnvironment(env: StreamExecutionEnvironment) {
            env.enableCheckpointing(Time.minutes(2).toMilliseconds())
            env.checkpointConfig.checkpointingMode = CheckpointingMode.AT_LEAST_ONCE
            env.checkpointConfig.minPauseBetweenCheckpoints = 1
            env.parallelism = 1
        }

    }
}
