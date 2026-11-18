package app.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.util.IConstants;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class CommandRecordTest {

    @Test
    void serializesAndReadsFixedWidthBigEndianRecord() {
        CommandRecord record = new CommandRecord(1234L, IConstants.SYNTH_ID_PIANO, 64, 500, 1, 0f);

        byte[] bytes = record.toByteArray();

        assertEquals(IConstants.COMMAND_RECORD_BYTES, bytes.length);
        ByteBuffer expectedFields = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        assertEquals(1234L, expectedFields.getLong());
        assertEquals(IConstants.SYNTH_ID_PIANO, expectedFields.getInt());
        assertEquals(64, expectedFields.getInt());
        assertEquals(500, expectedFields.getInt());
        assertEquals(1, Byte.toUnsignedInt(expectedFields.get()));
        assertEquals(0f, expectedFields.getFloat());
        while (expectedFields.hasRemaining()) {
            assertEquals(0, expectedFields.get());
        }

        CommandRecord restored = CommandRecord.fromByteArray(bytes);
        assertEquals(record.getRelativeTimeMillis(), restored.getRelativeTimeMillis());
        assertEquals(record.getSynthId(), restored.getSynthId());
        assertEquals(record.getNote(), restored.getNote());
        assertEquals(record.getDurationMillis(), restored.getDurationMillis());
        assertEquals(record.getMajor(), restored.getMajor());
        assertEquals(record.getVolume(), restored.getVolume());
        assertArrayEquals(bytes, restored.toByteArray());

        CommandRecord organRecord = new CommandRecord(75L, IConstants.SYNTH_ID_ORGAN, 60, 0, 0, 0.75f);
        CommandRecord restoredOrgan = CommandRecord.fromByteArray(organRecord.toByteArray());
        assertEquals(0.75f, restoredOrgan.getVolume());
    }

    @Test
    void rejectsInvalidRecordLengthAndNonzeroReservedBytes() {
        assertThrows(IllegalArgumentException.class, () -> CommandRecord.fromByteArray(new byte[47]));

        byte[] bytes = new CommandRecord(0L, IConstants.SYNTH_ID_BASS, 43, 100, 0, 0f).toByteArray();
        bytes[47] = 1;
        assertThrows(IllegalArgumentException.class, () -> CommandRecord.fromByteArray(bytes));
    }

    @Test
    void rejectsInvalidCommandFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new CommandRecord(-1L, IConstants.SYNTH_ID_BASS, 43, 100, 0, 0f));
        assertThrows(IllegalArgumentException.class,
                () -> new CommandRecord(0L, 99, 43, 100, 0, 0f));
        assertThrows(IllegalArgumentException.class,
                () -> new CommandRecord(0L, IConstants.SYNTH_ID_PIANO, 64, 100, 2, 0f));
        assertThrows(IllegalArgumentException.class,
                () -> new CommandRecord(0L, IConstants.SYNTH_ID_BASS, 43, 0, 0, 0f));
    }
}
