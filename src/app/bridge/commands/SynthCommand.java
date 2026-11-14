package app.bridge.commands;

import app.supercollider.BandClient;
import java.io.IOException;

public interface SynthCommand {

    void execute(BandClient pBandClient) throws IOException;
}
