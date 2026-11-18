package app.bridge.commands;

import app.model.CommandRecord;
import app.supercollider.BandClient;
import app.util.IConstants;
import java.io.IOException;

public final class BassCommand implements SynthCommand {

    private final int note;
    private final int durationMillis;

    public BassCommand(int pNote, int pDurationMillis) {
        if (pNote < 0 || pNote > 127) {
            throw new IllegalArgumentException("note must be in the MIDI range");
        }
        if (pDurationMillis <= 0) {
            throw new IllegalArgumentException("duration must be positive");
        }
        note = pNote;
        durationMillis = pDurationMillis;
    }

    public int getNote() {
        return note;
    }

    public int getDurationMillis() {
        return durationMillis;
    }

    @Override
    public CommandRecord toCommandRecord(long pRelativeTimeMillis) {
        return new CommandRecord(
                pRelativeTimeMillis,
                IConstants.SYNTH_ID_BASS,
                note,
                durationMillis,
                0,
                0f);
    }

    @Override
    public void execute(BandClient pBandClient) throws IOException {
        pBandClient.playBass(note, durationMillis);
    }
}
