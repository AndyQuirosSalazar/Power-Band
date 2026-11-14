package app.bridge.commands;

import app.supercollider.BandClient;
import java.io.IOException;

public final class PianoCommand implements SynthCommand {

    private final int note;
    private final int durationMillis;
    private final boolean major;

    public PianoCommand(int pNote, int pDurationMillis, boolean pMajor) {
        if (pNote < 0 || pNote > 127) {
            throw new IllegalArgumentException("note must be in the MIDI range");
        }
        if (pDurationMillis <= 0) {
            throw new IllegalArgumentException("duration must be positive");
        }
        note = pNote;
        durationMillis = pDurationMillis;
        major = pMajor;
    }

    public int getNote() {
        return note;
    }

    public int getDurationMillis() {
        return durationMillis;
    }

    public boolean isMajor() {
        return major;
    }

    @Override
    public void execute(BandClient pBandClient) throws IOException {
        pBandClient.playPiano(note, durationMillis, major);
    }
}
