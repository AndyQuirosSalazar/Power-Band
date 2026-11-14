package app;

import app.ui.MainMenu;
import app.ui.DeviceTestWindow;
import app.util.IConstants;
import app.supercollider.BandClient;
import app.logic.GlfwGamepadRuntime;
import java.io.IOException;
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
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame(IConstants.WINDOW_TITLE);
            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.setContentPane(new MainMenu(
                    window::dispose,
                    () -> openDeviceTest(window, gamepadRuntime, deviceTestWindow)));
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent pEvent) {
                    DeviceTestWindow activeWindow = deviceTestWindow.getAndSet(null);
                    if (activeWindow != null) {
                        activeWindow.dispose();
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
