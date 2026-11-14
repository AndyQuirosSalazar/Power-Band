package app.bridge.commands;

import app.supercollider.BandClient;
import java.io.IOException;

public final class OrganCommand implements SynthCommand {

    private static final int MIDI_NOTE = 60;
    private static final float SILENCE_THRESHOLD = 0.02f;
    private static final float[] RATIOS = { 1f, 2f, 3f };

    private final float volume;

    public OrganCommand(float pVolume) {
        if (!Float.isFinite(pVolume) || pVolume < 0f || pVolume > 1f) {
            throw new IllegalArgumentException("volume must be between 0 and 1");
        }
        volume = pVolume;
    }

    public float getVolume() {
        return volume;
    }

    @Override
    public void execute(BandClient pBandClient) throws IOException {
        if (volume < SILENCE_THRESHOLD) {
            pBandClient.noteOff(MIDI_NOTE);
            return;
        }
        pBandClient.noteOn(
                MIDI_NOTE,
                RATIOS,
                new float[] { volume, volume * 0.5f, volume * 0.25f });
    }
}
