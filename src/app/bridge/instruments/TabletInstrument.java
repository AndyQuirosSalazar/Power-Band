package app.bridge.instruments;

import app.bridge.commands.PianoCommand;
import app.bridge.commands.SynthCommand;
import app.model.TabletInput;
import java.util.Optional;

public final class TabletInstrument extends Instrument<TabletInput> {

    private static final int FIRST_NOTE = 48;
    private static final int NOTE_RANGE = 24;
    private static final int NOTE_DURATION_MILLIS = 250;

    private boolean lastTouching;
    private int lastNote = -1;
    private boolean lastMajor;

    @Override
    public Optional<SynthCommand> translate(TabletInput pInput) {
        if (pInput == null) {
            throw new NullPointerException("tablet input must not be null");
        }

        int note = FIRST_NOTE + Math.round(pInput.getX() * NOTE_RANGE);
        boolean major = pInput.getY() < 0.5f;
        if (!pInput.isTouching()) {
            lastTouching = false;
            return Optional.empty();
        }
        if (lastTouching && note == lastNote && major == lastMajor) {
            return Optional.empty();
        }

        lastTouching = true;
        lastNote = note;
        lastMajor = major;
        return Optional.of(new PianoCommand(note, NOTE_DURATION_MILLIS, major));
    }
}
