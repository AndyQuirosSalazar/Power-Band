package app.logic;

import app.util.IConstants;
import java.util.Objects;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public final class JavaSoundMicrophoneInputSource implements MicrophoneInputSource {

    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000f, 16, 1, true, false);
    private static final DataLine.Info LINE_INFO = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
    private static final int BUFFER_SIZE = 2048;

    private volatile boolean running;
    private volatile TargetDataLine line;
    private Thread captureThread;

    @Override
    public synchronized void start(
            Consumer<Float> pVolumeConsumer,
            Consumer<String> pStatusConsumer) {
        if (captureThread != null) {
            throw new IllegalStateException("microphone source has already been started");
        }
        Objects.requireNonNull(pVolumeConsumer, "pVolumeConsumer");
        Objects.requireNonNull(pStatusConsumer, "pStatusConsumer");
        running = true;
        captureThread = new Thread(
                () -> capture(pVolumeConsumer, pStatusConsumer),
                "power-band-microphone");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void capture(
            Consumer<Float> pVolumeConsumer,
            Consumer<String> pStatusConsumer) {
        TargetDataLine captureLine = null;
        try {
            captureLine = openLine();
            line = captureLine;
            if (!running) {
                return;
            }
            captureLine.open(AUDIO_FORMAT);
            captureLine.start();
            pStatusConsumer.accept("Entrada activa: " + getSelectedDeviceName());

            byte[] buffer = new byte[BUFFER_SIZE];
            while (running) {
                int bytesRead = captureLine.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    pVolumeConsumer.accept(calculateRms(buffer, bytesRead));
                }
            }
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException pError) {
            if (running) {
                pStatusConsumer.accept("Micrófono no disponible: " + pError.getMessage());
            }
        } finally {
            if (captureLine != null) {
                if (captureLine.isRunning()) {
                    captureLine.stop();
                }
                captureLine.close();
            }
            line = null;
        }
    }

    private static TargetDataLine openLine() throws LineUnavailableException {
        String configuredName = IConstants.MICROPHONE_DEVICE_NAME;
        if (configuredName.isBlank()) {
            return (TargetDataLine) AudioSystem.getLine(LINE_INFO);
        }

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            if (configuredName.equals(mixerInfo.getName())) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (!mixer.isLineSupported(LINE_INFO)) {
                    throw new LineUnavailableException(
                            "configured mixer does not support microphone capture: " + configuredName);
                }
                return (TargetDataLine) mixer.getLine(LINE_INFO);
            }
        }
        throw new LineUnavailableException("configured microphone mixer was not found: " + configuredName);
    }

    private static String getSelectedDeviceName() {
        return IConstants.MICROPHONE_DEVICE_NAME.isBlank()
                ? "predeterminada del sistema"
                : IConstants.MICROPHONE_DEVICE_NAME;
    }

    private static float calculateRms(byte[] pBuffer, int pLength) {
        long squaredSum = 0;
        int sampleCount = pLength / 2;
        for (int index = 0; index + 1 < pLength; index += 2) {
            int sample = (short) ((pBuffer[index + 1] << 8) | (pBuffer[index] & 0xff));
            squaredSum += (long) sample * sample;
        }
        return sampleCount == 0
                ? 0f
                : (float) Math.sqrt((double) squaredSum / sampleCount) / Short.MAX_VALUE;
    }

    @Override
    public void close() {
        running = false;
        TargetDataLine currentLine = line;
        if (currentLine != null) {
            if (currentLine.isRunning()) {
                currentLine.stop();
            }
            currentLine.close();
        }
    }
}
