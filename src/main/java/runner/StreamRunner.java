package runner;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import streams.AlertProcessingStream;
import util.ConfigManager;

public class StreamRunner {
    public static void main(String[] args) throws RuntimeException {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        ConfigManager.Companion.configureEnvironment(env);
        AlertProcessingStream stream = new AlertProcessingStream();
        stream.runStream(env);
    }
}
