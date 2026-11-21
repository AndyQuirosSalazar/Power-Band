package app.ui;

import app.bridge.commands.SynthCommand;
import app.logic.DeviceTestSession;
import app.logic.GlfwGamepadInputSource;
import app.logic.GlfwGamepadRuntime;
import app.logic.JavaSoundMicrophoneInputSource;
import app.logic.RecordingCommandCapture;
import app.model.TabletInput;
import app.storage.RecordingLibrary;
import app.supercollider.BandClient;
import app.util.IConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public final class RecordingWindow extends JFrame {

    private final BandClient bandClient;
    private final DeviceTestSession session;
    private final RecordingLibrary recordingLibrary;
    private final RecordingCommandCapture capture = new RecordingCommandCapture();
    private final AtomicBoolean resourcesClosed = new AtomicBoolean();
    private final JLabel gamepadStatus = new JLabel("Mando: no iniciado");
    private final JLabel tabletStatus = new JLabel("Tableta: use el lápiz en el área de prueba");
    private final JLabel microphoneStatus = new JLabel("Micrófono: no iniciado");
    private final JLabel synthStatus = new JLabel("Synths: listos para grabar");
    private final JProgressBar microphoneLevel = new JProgressBar(0, 100);
    private final JButton finishButton = new JButton(IConstants.BUTTON_FINISH_RECORDING);

    public RecordingWindow(GlfwGamepadRuntime pGamepadRuntime, RecordingLibrary pRecordingLibrary)
            throws IOException {
        this(new BandClient(), pGamepadRuntime, pRecordingLibrary);
    }

    public RecordingWindow(
            BandClient pBandClient,
            GlfwGamepadRuntime pGamepadRuntime,
            RecordingLibrary pRecordingLibrary) {
        super("Grabar música");
        bandClient = Objects.requireNonNull(pBandClient, "pBandClient");
        recordingLibrary = Objects.requireNonNull(pRecordingLibrary, "pRecordingLibrary");
        Objects.requireNonNull(pGamepadRuntime, "pGamepadRuntime");
        session = new DeviceTestSession(
                new GlfwGamepadInputSource(pGamepadRuntime),
                new JavaSoundMicrophoneInputSource(),
                this::sendAndCaptureCommand,
                this::showMicrophoneLevel,
                this::showStatus);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel statuses = new JPanel(new GridLayout(0, 1, 4, 4));
        microphoneLevel.setStringPainted(true);
        microphoneLevel.setString("Señal: 0%");
        microphoneLevel.getAccessibleContext().setAccessibleName("Nivel de señal del micrófono");
        statuses.add(gamepadStatus);
        statuses.add(tabletStatus);
        statuses.add(microphoneStatus);
        statuses.add(microphoneLevel);
        statuses.add(synthStatus);
        add(statuses, BorderLayout.NORTH);
        add(new TabletInputPanel(this::acceptTabletInput), BorderLayout.CENTER);
        add(finishButton, BorderLayout.SOUTH);

        finishButton.addActionListener(pEvent -> finishRecording());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent pEvent) {
                capture.start();
                session.start();
            }

            @Override
            public void windowClosed(WindowEvent pEvent) {
                closeResources();
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    private void acceptTabletInput(TabletInput pInput) {
        session.acceptTabletInput(pInput);
        String contactStatus = pInput.isTouching() ? "tocando" : "flotando";
        tabletStatus.setText(String.format(
                java.util.Locale.ROOT,
                "Tableta: %s | X: %.2f | Y: %.2f",
                contactStatus,
                pInput.getX(),
                pInput.getY()));
    }

    private void sendAndCaptureCommand(SynthCommand pCommand) {
        synchronized (bandClient) {
            try {
                pCommand.execute(bandClient);
                capture.record(pCommand);
            } catch (IOException pError) {
                showStatus("Synths: error enviando comando: " + pError.getMessage());
            }
        }
    }

    private void finishRecording() {
        finishButton.setEnabled(false);
        closeResources();
        RecordingSaveDialog.showDialog(this, recordingLibrary, capture.finish(), Instant.now());
        dispose();
    }

    private void closeResources() {
        if (resourcesClosed.compareAndSet(false, true)) {
            session.close();
            synchronized (bandClient) {
                bandClient.close();
            }
        }
    }

    private void showMicrophoneLevel(float pLevel) {
        int percentage = Math.round(pLevel * 100f);
        Runnable update = () -> {
            microphoneLevel.setValue(percentage);
            microphoneLevel.setString("Señal: " + percentage + "%");
        };
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        } else {
            SwingUtilities.invokeLater(update);
        }
    }

    private void showStatus(String pStatus) {
        Runnable update = () -> {
            if (pStatus.startsWith("Mando:")) {
                gamepadStatus.setText(pStatus);
            } else if (pStatus.startsWith("Micrófono:")) {
                microphoneStatus.setText(pStatus);
            } else if (pStatus.startsWith("Tableta:")) {
                tabletStatus.setText(pStatus);
            } else {
                synthStatus.setText(pStatus);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        } else {
            SwingUtilities.invokeLater(update);
        }
    }
}
