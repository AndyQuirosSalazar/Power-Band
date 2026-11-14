package app.ui;

import app.bridge.commands.SynthCommand;
import app.logic.DeviceTestSession;
import app.logic.GlfwGamepadInputSource;
import app.logic.GlfwGamepadRuntime;
import app.logic.JavaSoundMicrophoneInputSource;
import app.model.TabletInput;
import app.supercollider.BandClient;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.Objects;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public final class DeviceTestWindow extends JFrame {

    private final BandClient bandClient;
    private final DeviceTestSession session;
    private final JLabel gamepadStatus = new JLabel("Mando: no iniciado");
    private final JLabel tabletStatus = new JLabel("Tableta: use el lápiz en el área de prueba");
    private final JLabel microphoneStatus = new JLabel("Micrófono: no iniciado");
    private final JLabel synthStatus = new JLabel("Synths: listos para probar");

    public DeviceTestWindow(BandClient pBandClient, GlfwGamepadRuntime pGamepadRuntime) {
        super("Probar dispositivos");
        bandClient = Objects.requireNonNull(pBandClient, "pBandClient");
        session = new DeviceTestSession(
                new GlfwGamepadInputSource(pGamepadRuntime),
                new JavaSoundMicrophoneInputSource(),
                this::sendCommand,
                this::showStatus);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel statuses = new JPanel(new GridLayout(0, 1, 4, 4));
        statuses.add(gamepadStatus);
        statuses.add(tabletStatus);
        statuses.add(microphoneStatus);
        statuses.add(synthStatus);
        add(statuses, BorderLayout.NORTH);
        add(new TabletInputPanel(this::acceptTabletInput), BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent pEvent) {
                session.start();
            }

            @Override
            public void windowClosed(WindowEvent pEvent) {
                session.close();
                synchronized (bandClient) {
                    bandClient.close();
                }
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    private void acceptTabletInput(TabletInput pInput) {
        session.acceptTabletInput(pInput);
        tabletStatus.setText(pInput.isTouching()
                ? "Tableta: tocando"
                : "Tableta: flotando");
    }

    private void sendCommand(SynthCommand pCommand) {
        synchronized (bandClient) {
            try {
                pCommand.execute(bandClient);
            } catch (IOException pError) {
                showStatus("Synths: error enviando comando: " + pError.getMessage());
            }
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
