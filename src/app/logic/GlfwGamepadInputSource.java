package app.logic;

import app.model.GamepadDirection;
import java.util.Objects;
import java.util.function.Consumer;

public final class GlfwGamepadInputSource implements GamepadInputSource {

    private final GlfwGamepadRuntime runtime;
    private long sourceId = -1;

    public GlfwGamepadInputSource(GlfwGamepadRuntime pRuntime) {
        runtime = Objects.requireNonNull(pRuntime, "pRuntime");
    }

    @Override
    public void start(
            Consumer<GamepadDirection> pInputConsumer,
            Consumer<String> pStatusConsumer) {
        if (sourceId >= 0) {
            throw new IllegalStateException("gamepad source has already been started");
        }
        sourceId = runtime.registerSource(pInputConsumer, pStatusConsumer);
    }

    @Override
    public void close() {
        if (sourceId >= 0) {
            runtime.unregisterSource(sourceId);
            sourceId = -1;
        }
    }
}
