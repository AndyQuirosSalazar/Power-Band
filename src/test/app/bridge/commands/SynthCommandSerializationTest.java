package app.bridge.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.model.CommandRecord;
import app.util.IConstants;
import org.junit.jupiter.api.Test;

class SynthCommandSerializationTest {

    @Test
    void createsRecordsForEachSynthWithoutTypeInspection() {
        SynthCommand bass = new BassCommand(43, 200);
        SynthCommand piano = new PianoCommand(64, 500, true);
        SynthCommand organ = new OrganCommand(0.75f);

        assertRecord(bass.toCommandRecord(25L), 25L, IConstants.SYNTH_ID_BASS, 43, 200, 0, 0f);
        assertRecord(piano.toCommandRecord(50L), 50L, IConstants.SYNTH_ID_PIANO, 64, 500, 1, 0f);
        assertRecord(new PianoCommand(65, 500, false).toCommandRecord(60L),
                60L, IConstants.SYNTH_ID_PIANO, 65, 500, 0, 0f);
        assertRecord(organ.toCommandRecord(75L), 75L, IConstants.SYNTH_ID_ORGAN, 60, 0, 0, 0.75f);
    }

    @Test
    void rejectsNegativeRelativeTimeForEveryCommand() {
        SynthCommand[] commands = {
            new BassCommand(43, 200),
            new PianoCommand(64, 500, false),
            new OrganCommand(0.5f)
        };

        for (SynthCommand command : commands) {
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> command.toCommandRecord(-1L));
        }
    }

    private static void assertRecord(
            CommandRecord pRecord,
            long pRelativeTimeMillis,
            int pSynthId,
            int pNote,
            int pDurationMillis,
            int pMajor,
            float pVolume) {
        assertEquals(pRelativeTimeMillis, pRecord.getRelativeTimeMillis());
        assertEquals(pSynthId, pRecord.getSynthId());
        assertEquals(pNote, pRecord.getNote());
        assertEquals(pDurationMillis, pRecord.getDurationMillis());
        assertEquals(pMajor, pRecord.getMajor());
        assertEquals(pVolume, pRecord.getVolume());
    }
}
