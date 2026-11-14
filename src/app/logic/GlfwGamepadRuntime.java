package app.logic;

import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_X;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y;
import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_1;
import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_LAST;
import static org.lwjgl.glfw.GLFW.glfwGetGamepadName;
import static org.lwjgl.glfw.GLFW.glfwGetGamepadState;
import static org.lwjgl.glfw.GLFW.glfwGetJoystickAxes;
import static org.lwjgl.glfw.GLFW.glfwGetJoystickName;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwJoystickIsGamepad;
import static org.lwjgl.glfw.GLFW.glfwJoystickPresent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import app.bridge.instruments.GamepadInstrument;
import app.model.GamepadDirection;
import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWGamepadState;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class GlfwGamepadRuntime implements AutoCloseable {

    private static final long POLL_INTERVAL_MILLIS = 20;
    private static final long REDISCOVERY_INTERVAL_NANOS = 1_000_000_000L;

    private final Thread ownerThread;
    private final ConcurrentLinkedQueue<Runnable> actions = new ConcurrentLinkedQueue<>();
    private final AtomicLong nextSourceId = new AtomicLong();

    private SourceListener activeListener;
    private GLFWErrorCallback errorCallback;
    private boolean errorCallbackInstalled;
    private boolean initialized;
    private boolean reportedNoGamepad;
    private int joystickId = -1;
    private boolean joystickIsGamepad;
    private long nextDiscoveryTimeNanos;

    public GlfwGamepadRuntime() {
        ownerThread = Thread.currentThread();
    }

    public long registerSource(
            Consumer<GamepadDirection> pInputConsumer,
            Consumer<String> pStatusConsumer) {
        Objects.requireNonNull(pInputConsumer, "pInputConsumer");
        Objects.requireNonNull(pStatusConsumer, "pStatusConsumer");
        long sourceId = nextSourceId.incrementAndGet();
        SourceListener listener = new SourceListener(sourceId, pInputConsumer, pStatusConsumer);
        actions.add(() -> attach(listener));
        return sourceId;
    }

    public void unregisterSource(long pSourceId) {
        actions.add(() -> {
            if (activeListener != null && activeListener.sourceId == pSourceId) {
                activeListener = null;
                joystickId = -1;
            }
        });
    }

    public void runWhile(BooleanSupplier pKeepRunning) {
        requireOwnerThread();
        while (pKeepRunning.getAsBoolean()) {
            processActions();
            if (activeListener != null) {
                pollGamepad();
            }
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException pInterrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        processActions();
    }

    private void attach(SourceListener pListener) {
        if (activeListener != null) {
            pListener.statusConsumer.accept("Ya hay una prueba de mando activa.");
            return;
        }
        activeListener = pListener;
        pListener.statusConsumer.accept("Buscando mando compatible.");
        if (!initialize(pListener)) {
            return;
        }
        discoverGamepad(pListener);
    }

    private boolean initialize(SourceListener pListener) {
        if (initialized) {
            return true;
        }
        try {
            errorCallback = GLFWErrorCallback.create((pErrorCode, pDescription) -> {
                SourceListener listener = activeListener;
                if (listener != null) {
                    listener.statusConsumer.accept(
                            "GLFW: " + pErrorCode + " " + MemoryUtil.memUTF8(pDescription));
                }
            });
            errorCallback.set();
            errorCallbackInstalled = true;
            if (!glfwInit()) {
                pListener.statusConsumer.accept("No se pudo iniciar GLFW.");
                releaseNativeResources();
                return false;
            }
            initialized = true;
            return true;
        } catch (IllegalCallerException | IllegalStateException | UnsatisfiedLinkError pError) {
            pListener.statusConsumer.accept("No se pudo iniciar GLFW: " + pError.getMessage());
            releaseNativeResources();
            return false;
        }
    }

    private void discoverGamepad(SourceListener pListener) {
        joystickId = -1;
        int joystickWithAxes = -1;
        for (int joystick = GLFW_JOYSTICK_1; joystick <= GLFW_JOYSTICK_LAST; joystick++) {
            if (!glfwJoystickPresent(joystick)) {
                continue;
            }
            if (glfwJoystickIsGamepad(joystick)) {
                joystickId = joystick;
                joystickIsGamepad = true;
                String name = glfwGetGamepadName(joystick);
                if (name == null) {
                    name = glfwGetJoystickName(joystick);
                }
                pListener.statusConsumer.accept("Conectado: " + name);
                reportedNoGamepad = false;
                return;
            }
            if (joystickWithAxes < 0 && hasDirectionalAxes(glfwGetJoystickAxes(joystick))) {
                joystickWithAxes = joystick;
            }
        }
        if (joystickWithAxes >= 0) {
            joystickId = joystickWithAxes;
            joystickIsGamepad = false;
            pListener.statusConsumer.accept(
                    "Conectado: " + glfwGetJoystickName(joystickWithAxes) + " (stick genérico)");
            reportedNoGamepad = false;
            return;
        }
        if (!reportedNoGamepad) {
            pListener.statusConsumer.accept("No se detectó un mando o stick compatible.");
            reportedNoGamepad = true;
        }
        nextDiscoveryTimeNanos = System.nanoTime() + REDISCOVERY_INTERVAL_NANOS;
    }

    private void pollGamepad() {
        if (!initialized) {
            return;
        }
        try {
            glfwPollEvents();
            if (joystickId < 0) {
                if (System.nanoTime() >= nextDiscoveryTimeNanos) {
                    discoverGamepad(activeListener);
                }
                return;
            }
            if (!glfwJoystickPresent(joystickId)
                    || (joystickIsGamepad && !glfwJoystickIsGamepad(joystickId))) {
                activeListener.statusConsumer.accept("Mando desconectado.");
                activeListener.inputConsumer.accept(GamepadDirection.NONE);
                joystickId = -1;
                joystickIsGamepad = false;
                reportedNoGamepad = false;
                nextDiscoveryTimeNanos = System.nanoTime();
                return;
            }

            GamepadDirection direction;
            if (joystickIsGamepad) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    GLFWGamepadState state = GLFWGamepadState.malloc(stack);
                    if (!glfwGetGamepadState(joystickId, state)) {
                        activeListener.statusConsumer.accept("No se pudo leer el estado del mando.");
                        return;
                    }
                    direction = GamepadInstrument.directionFromAxes(
                            state.axes(GLFW_GAMEPAD_AXIS_LEFT_X),
                            state.axes(GLFW_GAMEPAD_AXIS_LEFT_Y));
                }
            } else {
                FloatBuffer axes = glfwGetJoystickAxes(joystickId);
                if (!hasDirectionalAxes(axes)) {
                    activeListener.statusConsumer.accept("No se pudieron leer los ejes del stick.");
                    return;
                }
                direction = directionFromJoystickAxes(axes);
            }
            activeListener.inputConsumer.accept(direction);
        } catch (IllegalCallerException | IllegalStateException | UnsatisfiedLinkError pError) {
            activeListener.statusConsumer.accept("Error leyendo el mando: " + pError.getMessage());
            joystickId = -1;
            activeListener = null;
            releaseNativeResources();
        }
    }

    static GamepadDirection directionFromJoystickAxes(FloatBuffer pAxes) {
        if (!hasDirectionalAxes(pAxes)) {
            throw new IllegalArgumentException("joystick must expose at least two axes");
        }
        int firstAxis = pAxes.position();
        return GamepadInstrument.directionFromAxes(
                pAxes.get(firstAxis),
                pAxes.get(firstAxis + 1));
    }

    private static boolean hasDirectionalAxes(FloatBuffer pAxes) {
        return pAxes != null && pAxes.remaining() >= 2;
    }

    private void processActions() {
        Runnable action;
        while ((action = actions.poll()) != null) {
            action.run();
        }
    }

    private void requireOwnerThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("GLFW runtime must run on its creating thread");
        }
    }

    @Override
    public void close() {
        requireOwnerThread();
        activeListener = null;
        joystickId = -1;
        releaseNativeResources();
    }

    private void releaseNativeResources() {
        if (initialized) {
            glfwTerminate();
            initialized = false;
        }
        if (errorCallback != null) {
            if (errorCallbackInstalled) {
                glfwSetErrorCallback(null);
            }
            errorCallback.free();
            errorCallback = null;
            errorCallbackInstalled = false;
        }
    }

    private static final class SourceListener {

        private final long sourceId;
        private final Consumer<GamepadDirection> inputConsumer;
        private final Consumer<String> statusConsumer;

        private SourceListener(
                long pSourceId,
                Consumer<GamepadDirection> pInputConsumer,
                Consumer<String> pStatusConsumer) {
            sourceId = pSourceId;
            inputConsumer = pInputConsumer;
            statusConsumer = pStatusConsumer;
        }
    }
}
