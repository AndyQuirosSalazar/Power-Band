package app.logic;

import app.model.GamepadDirection;
import java.util.function.Consumer;

public interface GamepadInputSource extends AutoCloseable {

    void start(Consumer<GamepadDirection> pInputConsumer, Consumer<String> pStatusConsumer);

    @Override
    void close();
}
