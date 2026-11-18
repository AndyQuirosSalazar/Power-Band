package app.bridge.commands;

import app.model.CommandRecord;
import app.supercollider.BandClient;
import app.util.IConstants;
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
    public CommandRecord toCommandRecord(long pRelativeTimeMillis) {
        return new CommandRecord(
                pRelativeTimeMillis,
                IConstants.SYNTH_ID_PIANO,
                note,
                durationMillis,
                major ? 1 : 0,
                0f);
    }

    @Override
    public void execute(BandClient pBandClient) throws IOException {
        pBandClient.playPiano(note, durationMillis, major);
    }
}
