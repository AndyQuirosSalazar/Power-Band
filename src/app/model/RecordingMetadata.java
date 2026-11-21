package app.model;

import java.time.Instant;
import java.util.Objects;

public final class RecordingMetadata {

    private final String name;
    private final Instant createdAt;

    public RecordingMetadata(String pName, Instant pCreatedAt) {
        name = Objects.requireNonNull(pName, "pName");
        createdAt = Objects.requireNonNull(pCreatedAt, "pCreatedAt");
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
