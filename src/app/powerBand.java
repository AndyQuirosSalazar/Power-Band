package app;

import app.ui.MainMenu;
import app.ui.DeviceTestWindow;
import app.ui.RecordingPlaybackDialog;
import app.ui.RecordingWindow;
import app.util.IConstants;
import app.supercollider.BandClient;
import app.logic.GlfwGamepadRuntime;
import app.logic.RecordingPlayer;
import app.storage.RecordingStorage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class powerBand {

    private powerBand() {
    }

    public static void main(String[] pArgs) {
        GlfwGamepadRuntime gamepadRuntime = new GlfwGamepadRuntime();
        AtomicBoolean applicationRunning = new AtomicBoolean(true);
        AtomicReference<DeviceTestWindow> deviceTestWindow = new AtomicReference<>();
        AtomicReference<RecordingWindow> recordingWindow = new AtomicReference<>();
        AtomicReference<RecordingPlaybackDialog> playbackDialog = new AtomicReference<>();
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame(IConstants.WINDOW_TITLE);
            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.setContentPane(new MainMenu(
                    window::dispose,
                    () -> openDeviceTest(window, gamepadRuntime, deviceTestWindow),
                    () -> openRecording(window, gamepadRuntime, recordingWindow),
                    () -> openPlayback(window, playbackDialog)));
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent pEvent) {
                    DeviceTestWindow activeWindow = deviceTestWindow.getAndSet(null);
                    if (activeWindow != null) {
                        activeWindow.dispose();
                    }
                    RecordingWindow activeRecordingWindow = recordingWindow.getAndSet(null);
                    if (activeRecordingWindow != null) {
                        activeRecordingWindow.dispose();
                    }
                    RecordingPlaybackDialog activePlaybackDialog = playbackDialog.getAndSet(null);
                    if (activePlaybackDialog != null) {
                        activePlaybackDialog.dispose();
                    }
                    applicationRunning.set(false);
                }

            });
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
        gamepadRuntime.runWhile(applicationRunning::get);
        gamepadRuntime.close();
    }

    private static void openPlayback(
            JFrame pOwner,
            AtomicReference<RecordingPlaybackDialog> pPlaybackDialog) {
        RecordingPlaybackDialog existingDialog = pPlaybackDialog.get();
        if (existingDialog != null && existingDialog.isDisplayable()) {
            existingDialog.toFront();
            return;
        }
        RecordingPlaybackDialog dialog = new RecordingPlaybackDialog(
                pOwner,
                new RecordingPlayer(new RecordingStorage(Path.of("."))));
        pPlaybackDialog.set(dialog);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent pEvent) {
                pPlaybackDialog.compareAndSet(dialog, null);
            }
        });
        dialog.setVisible(true);
    }

    private static void openRecording(
            JFrame pOwner,
            GlfwGamepadRuntime pGamepadRuntime,
            AtomicReference<RecordingWindow> pRecordingWindow) {
        RecordingWindow existingWindow = pRecordingWindow.get();
        if (existingWindow != null && existingWindow.isDisplayable()) {
            existingWindow.toFront();
            return;
        }
        try {
            RecordingWindow window = new RecordingWindow(
                    pGamepadRuntime,
                    new RecordingStorage(Path.of(".")));
            pRecordingWindow.set(window);
            window.setLocationRelativeTo(pOwner);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent pEvent) {
                    pRecordingWindow.compareAndSet(window, null);
                }
            });
            window.setVisible(true);
        } catch (IOException pError) {
            JOptionPane.showMessageDialog(
                    pOwner,
                    "No se pudo iniciar la grabación: " + pError.getMessage(),
                    IConstants.BUTTON_RECORD,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void openDeviceTest(
            JFrame pOwner,
            GlfwGamepadRuntime pGamepadRuntime,
            AtomicReference<DeviceTestWindow> pDeviceTestWindow) {
        DeviceTestWindow existingWindow = pDeviceTestWindow.get();
        if (existingWindow != null && existingWindow.isDisplayable()) {
            existingWindow.toFront();
            return;
        }
        try {
            DeviceTestWindow testWindow = new DeviceTestWindow(new BandClient(), pGamepadRuntime);
            pDeviceTestWindow.set(testWindow);
            testWindow.setLocationRelativeTo(pOwner);
            testWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent pEvent) {
                    pDeviceTestWindow.compareAndSet(testWindow, null);
                }
            });
            testWindow.setVisible(true);
        } catch (IOException pError) {
            JOptionPane.showMessageDialog(
                    pOwner,
                    "No se pudo iniciar la prueba de dispositivos: " + pError.getMessage(),
                    IConstants.BUTTON_TEST,
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
