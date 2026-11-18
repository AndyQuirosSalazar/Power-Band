package app.model;

import app.util.IConstants;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public final class CommandRecord {

    private final long relativeTimeMillis;
    private final int synthId;
    private final int note;
    private final int durationMillis;
    private final int major;
    private final float volume;

    public CommandRecord(
            long pRelativeTimeMillis,
            int pSynthId,
            int pNote,
            int pDurationMillis,
            int pMajor,
            float pVolume) {
        validate(pRelativeTimeMillis, pSynthId, pNote, pDurationMillis, pMajor, pVolume);
        relativeTimeMillis = pRelativeTimeMillis;
        synthId = pSynthId;
        note = pNote;
        durationMillis = pDurationMillis;
        major = pMajor;
        volume = pVolume;
    }

    public long getRelativeTimeMillis() {
        return relativeTimeMillis;
    }

    public int getSynthId() {
        return synthId;
    }

    public int getNote() {
        return note;
    }

    public int getDurationMillis() {
        return durationMillis;
    }

    public int getMajor() {
        return major;
    }

    public float getVolume() {
        return volume;
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(IConstants.COMMAND_RECORD_BYTES)
                .order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(relativeTimeMillis);
        buffer.putInt(synthId);
        buffer.putInt(note);
        buffer.putInt(durationMillis);
        buffer.put((byte) major);
        buffer.putFloat(volume);
        while (buffer.hasRemaining()) {
            buffer.put((byte) 0);
        }
        return buffer.array();
    }

    public static CommandRecord fromByteArray(byte[] pBytes) {
        Objects.requireNonNull(pBytes, "pBytes");
        if (pBytes.length != IConstants.COMMAND_RECORD_BYTES) {
            throw new IllegalArgumentException(
                    "command record must be exactly " + IConstants.COMMAND_RECORD_BYTES + " bytes");
        }

        ByteBuffer buffer = ByteBuffer.wrap(pBytes).order(ByteOrder.BIG_ENDIAN);
        long relativeTimeMillis = buffer.getLong();
        int synthId = buffer.getInt();
        int note = buffer.getInt();
        int durationMillis = buffer.getInt();
        int major = Byte.toUnsignedInt(buffer.get());
        float volume = buffer.getFloat();
        while (buffer.hasRemaining()) {
            if (buffer.get() != 0) {
                throw new IllegalArgumentException("reserved command record bytes must be zero");
            }
        }
        return new CommandRecord(relativeTimeMillis, synthId, note, durationMillis, major, volume);
    }

    private static void validate(
            long pRelativeTimeMillis,
            int pSynthId,
            int pNote,
            int pDurationMillis,
            int pMajor,
            float pVolume) {
        if (pRelativeTimeMillis < 0) {
            throw new IllegalArgumentException("relative time must not be negative");
        }
        if (pSynthId != IConstants.SYNTH_ID_BASS
                && pSynthId != IConstants.SYNTH_ID_PIANO
                && pSynthId != IConstants.SYNTH_ID_ORGAN) {
            throw new IllegalArgumentException("unknown synth id");
        }
        if (pNote < 0 || pNote > 127) {
            throw new IllegalArgumentException("note must be in the MIDI range");
        }
        if (pDurationMillis < 0) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        if (pSynthId != IConstants.SYNTH_ID_ORGAN && pDurationMillis == 0) {
            throw new IllegalArgumentException("note duration must be positive");
        }
        if (pMajor != 0 && pMajor != 1) {
            throw new IllegalArgumentException("major must be 0 or 1");
        }
        if (!Float.isFinite(pVolume) || pVolume < 0f || pVolume > 1f) {
            throw new IllegalArgumentException("volume must be between 0 and 1");
        }
        if (pSynthId == IConstants.SYNTH_ID_BASS && (pMajor != 0 || pVolume != 0f)) {
            throw new IllegalArgumentException("bass records must zero unused fields");
        }
        if (pSynthId == IConstants.SYNTH_ID_PIANO && pVolume != 0f) {
            throw new IllegalArgumentException("piano records must zero unused fields");
        }
        if (pSynthId == IConstants.SYNTH_ID_ORGAN && (pMajor != 0 || pDurationMillis != 0)) {
            throw new IllegalArgumentException("organ records must zero unused fields");
        }
    }
}
