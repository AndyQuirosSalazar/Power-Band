package app.bridge.instruments;

import app.bridge.commands.BassCommand;
import app.bridge.commands.SynthCommand;
import app.model.GamepadDirection;
import java.util.Optional;

public final class GamepadInstrument extends Instrument<GamepadDirection> {

    private static final float DIRECTION_THRESHOLD = 0.55f;
    private static final int NOTE_DURATION_MILLIS = 250;

    private GamepadDirection lastDirection = GamepadDirection.NONE;

    public static GamepadDirection directionFromAxes(float pHorizontal, float pVertical) {
        if (!Float.isFinite(pHorizontal) || !Float.isFinite(pVertical)
                || Math.abs(pHorizontal) > 1f || Math.abs(pVertical) > 1f) {
            throw new IllegalArgumentException("gamepad axes must be between -1 and 1");
        }

        float horizontalMagnitude = Math.abs(pHorizontal);
        float verticalMagnitude = Math.abs(pVertical);
        if (horizontalMagnitude < DIRECTION_THRESHOLD && verticalMagnitude < DIRECTION_THRESHOLD) {
            return GamepadDirection.NONE;
        }
        if (horizontalMagnitude >= verticalMagnitude) {
            return pHorizontal < 0 ? GamepadDirection.LEFT : GamepadDirection.RIGHT;
        }
        return pVertical < 0 ? GamepadDirection.UP : GamepadDirection.DOWN;
    }

    @Override
    public Optional<SynthCommand> translate(GamepadDirection pDirection) {
        if (pDirection == null) {
            throw new NullPointerException("direction must not be null");
        }
        if (pDirection == GamepadDirection.NONE) {
            lastDirection = GamepadDirection.NONE;
            return Optional.empty();
        }
        if (pDirection == lastDirection) {
            return Optional.empty();
        }

        lastDirection = pDirection;
        return Optional.of(new BassCommand(noteFor(pDirection), NOTE_DURATION_MILLIS));
    }

    private static int noteFor(GamepadDirection pDirection) {
        return switch (pDirection) {
            case LEFT -> 36;
            case RIGHT -> 43;
            case UP -> 45;
            case DOWN -> 41;
            case NONE -> throw new IllegalArgumentException("NONE has no bass note");
        };
    }
}
