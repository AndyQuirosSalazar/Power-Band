package app.util;

import java.awt.Color;

public interface IConstants {

    public static final String WINDOW_TITLE = "Power Band";
    public static final String BUTTON_TEST = "Probar";
    public static final String BUTTON_RECORD = "Grabar";
    public static final String BUTTON_FINISH_RECORDING = "Finalizar grabación";
    public static final String BUTTON_SAVE = "Guardar";
    public static final String BUTTON_CANCEL = "Cancelar";
    public static final String BUTTON_PLAY = "Reproducir";
    public static final String BUTTON_EXIT = "Salir";
    public static final String PLAYBACK_DIALOG_TITLE = "Reproducir grabación";
    public static final String PLAYBACK_LOADING_MESSAGE = "Cargando grabaciones...";
    public static final String PLAYBACK_SELECT_MESSAGE = "Selecciona una grabación para reproducir.";
    public static final String PLAYBACK_ACTIVE_MESSAGE = "Reproduciendo...";
    public static final String PLAYBACK_COMPLETED_MESSAGE = "Reproducción finalizada.";
    public static final String PLAYBACK_CANCELLED_MESSAGE = "Reproducción cancelada.";
    public static final String PLAYBACK_EMPTY_MESSAGE = "Todavía no hay grabaciones guardadas.";
    public static final String PLAYBACK_ERROR_MESSAGE_PREFIX = "No se pudo reproducir la grabación: ";
    public static final String PLAYBACK_COMPLETED_MESSAGE_PREFIX = "La reproducción de \"";
    public static final String PLAYBACK_COMPLETED_MESSAGE_SUFFIX = "\" terminó.";
    public static final String PLAYBACK_NOT_FOUND_MESSAGE_PREFIX = "La grabación \"";
    public static final String PLAYBACK_NOT_FOUND_MESSAGE_SUFFIX = "\" ya no está disponible.";
    public static final String MICROPHONE_DEVICE_NAME = "";
    public static final Color TITLE_COLOR = new Color(0x2C4EE6);
    public static final int SYNTH_ID_BASS = 1;
    public static final int SYNTH_ID_PIANO = 2;
    public static final int SYNTH_ID_ORGAN = 3;
    public static final int COMMAND_RECORD_BYTES = 48;
    public static final int RECORDING_NAME_BYTES = 32;
    public static final int RECORDING_NAME_MAX_LENGTH = 31;
    public static final int RECORDING_HEADER_BYTES = 48;
    public static final int RECORDING_INDEX_RECORD_BYTES = 40;
    public static final String RECORDINGS_MUSIC_FILE_NAME = "recordings.music";
    public static final String RECORDINGS_INDEX_FILE_NAME = "recordings.index";
    public static final String RECORDINGS_LIST_FILE_NAME = "recordings.list";
}
