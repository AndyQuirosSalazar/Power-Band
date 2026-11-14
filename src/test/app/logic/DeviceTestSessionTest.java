package app.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import app.bridge.commands.BassCommand;
import app.bridge.commands.OrganCommand;
import app.bridge.commands.PianoCommand;
import app.bridge.commands.SynthCommand;
import app.model.GamepadDirection;
import app.model.TabletInput;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class DeviceTestSessionTest {

    @Test
    void routesEachSourceToOnlyItsAssignedSynthAndClosesSources() {
        FakeGamepadSource gamepadSource = new FakeGamepadSource();
        FakeMicrophoneSource microphoneSource = new FakeMicrophoneSource();
        List<SynthCommand> commands = new ArrayList<>();
        DeviceTestSession session = new DeviceTestSession(
                gamepadSource,
                microphoneSource,
                commands::add,
                pStatus -> { });

        session.start();
        gamepadSource.inputConsumer.accept(GamepadDirection.RIGHT);
        microphoneSource.volumeConsumer.accept(0.5f);
        session.acceptTabletInput(new TabletInput(0.5f, 0.25f, true));

        assertEquals(3, commands.size());
        assertEquals(43, assertInstanceOf(BassCommand.class, commands.get(0)).getNote());
        assertEquals(0.5f, assertInstanceOf(OrganCommand.class, commands.get(1)).getVolume());
        assertEquals(60, assertInstanceOf(PianoCommand.class, commands.get(2)).getNote());

        session.close();
        assertEquals(1, gamepadSource.closeCalls);
        assertEquals(1, microphoneSource.closeCalls);
    }

    private static final class FakeGamepadSource implements GamepadInputSource {

        private Consumer<GamepadDirection> inputConsumer;
        private int closeCalls;

        @Override
        public void start(
                Consumer<GamepadDirection> pInputConsumer,
                Consumer<String> pStatusConsumer) {
            inputConsumer = pInputConsumer;
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }

    private static final class FakeMicrophoneSource implements MicrophoneInputSource {

        private Consumer<Float> volumeConsumer;
        private int closeCalls;

        @Override
        public void start(Consumer<Float> pVolumeConsumer, Consumer<String> pStatusConsumer) {
            volumeConsumer = pVolumeConsumer;
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }
}
