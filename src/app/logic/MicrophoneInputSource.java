package app.logic;

import java.util.function.Consumer;

public interface MicrophoneInputSource extends AutoCloseable {

    void start(Consumer<Float> pVolumeConsumer, Consumer<String> pStatusConsumer);

    @Override
    void close();
}
