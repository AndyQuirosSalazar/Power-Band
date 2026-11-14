package app.bridge.instruments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bridge.commands.BassCommand;
import app.bridge.commands.OrganCommand;
import app.bridge.commands.PianoCommand;
import app.bridge.commands.SynthCommand;
import app.model.GamepadDirection;
import app.model.TabletInput;
import org.junit.jupiter.api.Test;

class DeviceInstrumentTest {

    @Test
    void mapsOnlyDirectionalGamepadChangesToBassNotes() {
        GamepadInstrument instrument = new GamepadInstrument();

        assertEquals(GamepadDirection.LEFT, GamepadInstrument.directionFromAxes(-0.8f, 0f));
        assertEquals(GamepadDirection.DOWN, GamepadInstrument.directionFromAxes(0f, 0.9f));
        assertEquals(GamepadDirection.NONE, GamepadInstrument.directionFromAxes(0.2f, -0.3f));
        assertEquals(36, assertInstanceOf(BassCommand.class, instrument.translate(GamepadDirection.LEFT)
                .orElseThrow()).getNote());
        assertTrue(instrument.translate(GamepadDirection.LEFT).isEmpty());
        assertTrue(instrument.translate(GamepadDirection.NONE).isEmpty());
        assertEquals(41, assertInstanceOf(BassCommand.class, instrument.translate(GamepadDirection.DOWN)
                .orElseThrow()).getNote());
    }

    @Test
    void mapsTabletCoordinatesAndPlaysOnlyWhileTouching() {
        TabletInstrument instrument = new TabletInstrument();

        SynthCommand command = instrument.translate(new TabletInput(1f, 0.2f, true)).orElseThrow();
        PianoCommand pianoCommand = assertInstanceOf(PianoCommand.class, command);
        assertEquals(72, pianoCommand.getNote());
        assertTrue(pianoCommand.isMajor());
        assertTrue(instrument.translate(new TabletInput(1f, 0.2f, true)).isEmpty());
        assertTrue(instrument.translate(new TabletInput(1f, 0.2f, false)).isEmpty());

        PianoCommand minorCommand = assertInstanceOf(
                PianoCommand.class,
                instrument.translate(new TabletInput(0f, 0.8f, true)).orElseThrow());
        assertEquals(48, minorCommand.getNote());
        assertFalse(minorCommand.isMajor());
    }

    @Test
    void mapsMicrophoneVolumeToQuantizedOrganAmplitudeAndSilence() {
        MicrophoneInstrument instrument = new MicrophoneInstrument();

        assertTrue(instrument.translate(0.01f).isEmpty());
        OrganCommand activeCommand = assertInstanceOf(
                OrganCommand.class,
                instrument.translate(0.5f).orElseThrow());
        assertEquals(0.5f, activeCommand.getVolume());
        assertTrue(instrument.translate(0.51f).isEmpty());

        OrganCommand silenceCommand = assertInstanceOf(
                OrganCommand.class,
                instrument.translate(0f).orElseThrow());
        assertEquals(0f, silenceCommand.getVolume());
    }
}
