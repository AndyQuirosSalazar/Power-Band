package app.logic;

import app.bridge.commands.SynthCommand;
import app.bridge.instruments.GamepadInstrument;
import app.bridge.instruments.MicrophoneInstrument;
import app.bridge.instruments.TabletInstrument;
import app.model.GamepadDirection;
import app.model.TabletInput;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class DeviceTestSession implements AutoCloseable {

    private final GamepadInputSource gamepadSource;
    private final MicrophoneInputSource microphoneSource;
    private final Consumer<SynthCommand> commandConsumer;
    private final Consumer<Float> microphoneLevelConsumer;
    private final Consumer<String> statusConsumer;
    private final GamepadInstrument gamepadInstrument = new GamepadInstrument();
    private final TabletInstrument tabletInstrument = new TabletInstrument();
    private final MicrophoneInstrument microphoneInstrument = new MicrophoneInstrument();

    private boolean started;
    private GamepadDirection lastReportedGamepadDirection = GamepadDirection.NONE;
    private volatile boolean closed;

    public DeviceTestSession(
            GamepadInputSource pGamepadSource,
            MicrophoneInputSource pMicrophoneSource,
            Consumer<SynthCommand> pCommandConsumer,
            Consumer<Float> pMicrophoneLevelConsumer,
            Consumer<String> pStatusConsumer) {
        gamepadSource = Objects.requireNonNull(pGamepadSource, "pGamepadSource");
        microphoneSource = Objects.requireNonNull(pMicrophoneSource, "pMicrophoneSource");
        commandConsumer = Objects.requireNonNull(pCommandConsumer, "pCommandConsumer");
        microphoneLevelConsumer = Objects.requireNonNull(pMicrophoneLevelConsumer, "pMicrophoneLevelConsumer");
        statusConsumer = Objects.requireNonNull(pStatusConsumer, "pStatusConsumer");
    }

    public void start() {
        if (started) {
            throw new IllegalStateException("device test session has already been started");
        }
        started = true;
        statusConsumer.accept("Mando: buscando dispositivo compatible");
        gamepadSource.start(
                this::acceptGamepadDirection,
                pStatus -> statusConsumer.accept("Mando: " + pStatus));
        statusConsumer.accept("Micrófono: abriendo entrada predeterminada");
        microphoneSource.start(
                this::acceptMicrophoneVolume,
                pStatus -> statusConsumer.accept("Micrófono: " + pStatus));
        statusConsumer.accept("Tableta: use el lápiz en el área de prueba");
    }

    private void acceptMicrophoneVolume(Float pVolume) {
        Optional<SynthCommand> command = microphoneInstrument.translate(pVolume);
        if (closed) {
            return;
        }
        microphoneLevelConsumer.accept(pVolume);
        dispatch(command);
    }

    private void acceptGamepadDirection(GamepadDirection pDirection) {
        Optional<SynthCommand> command = gamepadInstrument.translate(pDirection);
        if (closed) {
            return;
        }
        if (pDirection != lastReportedGamepadDirection) {
            lastReportedGamepadDirection = pDirection;
            statusConsumer.accept("Mando: " + directionLabel(pDirection));
        }
        dispatch(command);
    }

    private static String directionLabel(GamepadDirection pDirection) {
        return switch (pDirection) {
            case LEFT -> "izquierda";
            case RIGHT -> "derecha";
            case UP -> "arriba";
            case DOWN -> "abajo";
            case NONE -> "centrado";
        };
    }

    public void acceptTabletInput(TabletInput pInput) {
        dispatch(tabletInstrument.translate(pInput));
    }

    private void dispatch(Optional<SynthCommand> pCommand) {
        if (!closed) {
            pCommand.ifPresent(commandConsumer);
        }
    }

    @Override
    public void close() {
        closed = true;
        gamepadSource.close();
        microphoneSource.close();
    }
}
