package app.bridge.commands;

import app.model.CommandRecord;
import app.supercollider.BandClient;
import java.io.IOException;

public interface SynthCommand {

    CommandRecord toCommandRecord(long pRelativeTimeMillis);

    void execute(BandClient pBandClient) throws IOException;
}
