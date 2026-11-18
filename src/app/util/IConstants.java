package app.util;

import java.awt.Color;

public interface IConstants {

    public static final String WINDOW_TITLE = "Power Band";
    public static final String BUTTON_TEST = "Probar";
    public static final String BUTTON_RECORD = "Grabar";
    public static final String BUTTON_PLAY = "Reproducir";
    public static final String BUTTON_EXIT = "Salir";
    public static final String MICROPHONE_DEVICE_NAME = "";
    public static final Color TITLE_COLOR = new Color(0x2C4EE6);
    public static final int SYNTH_ID_BASS = 1;
    public static final int SYNTH_ID_PIANO = 2;
    public static final int SYNTH_ID_ORGAN = 3;
    public static final int COMMAND_RECORD_BYTES = 48;
    public static final int RECORDING_NAME_BYTES = 32;
    public static final int RECORDING_INDEX_RECORD_BYTES = 40;
}
