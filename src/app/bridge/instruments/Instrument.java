package app.bridge.instruments;

import app.bridge.commands.SynthCommand;
import java.util.Optional;

public abstract class Instrument<T> {

    public abstract Optional<SynthCommand> translate(T pInput);
}
