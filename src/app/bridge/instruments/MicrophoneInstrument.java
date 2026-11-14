package app.bridge.instruments;

import app.bridge.commands.OrganCommand;
import app.bridge.commands.SynthCommand;
import java.util.Optional;

public final class MicrophoneInstrument extends Instrument<Float> {

    private static final float SILENCE_THRESHOLD = 0.02f;
    private static final int VOLUME_LEVELS = 8;

    private int lastVolumeLevel;

    @Override
    public Optional<SynthCommand> translate(Float pVolume) {
        if (pVolume == null || !Float.isFinite(pVolume) || pVolume < 0f || pVolume > 1f) {
            throw new IllegalArgumentException("volume must be between 0 and 1");
        }

        int volumeLevel = pVolume < SILENCE_THRESHOLD
                ? 0
                : Math.max(1, Math.min(VOLUME_LEVELS, Math.round(pVolume * VOLUME_LEVELS)));
        if (volumeLevel == lastVolumeLevel) {
            return Optional.empty();
        }

        lastVolumeLevel = volumeLevel;
        return Optional.of(new OrganCommand((float) volumeLevel / VOLUME_LEVELS));
    }
}
