# Frontend

After you change this layer, update this file so the next run stays current.

Source: `src/app/ui`

## Implemented
- Swing menu and device-test window: `src/app/ui/MainMenu.java`, `src/app/ui/DeviceTestWindow.java`
- Device-test window displays each gamepad direction received: `src/app/ui/DeviceTestWindow.java`
- Device-test window displays the live normalized microphone signal as a level meter: `src/app/ui/DeviceTestWindow.java`
- Device-test window displays tablet contact state and normalized X/Y coordinates: `src/app/ui/DeviceTestWindow.java`
- Tablet input area, using local AWT mouse events as pen input: `src/app/ui/TabletInputPanel.java`

## Entrypoints
- `src/app/powerBand.java` — creates the menu and runs the GLFW polling loop on the main thread.

## Classes
### MainMenu
Displays the title and four buttons. `src/app/ui/MainMenu.java`
- `MainMenu(Runnable pExitAction)` — builds the menu with no test callback.
- `MainMenu(Runnable pExitAction, Runnable pTestAction)` — wires `Probar` and `Salir` actions.

### DeviceTestWindow
Shows device statuses, a live microphone level meter, tablet contact state and normalized X/Y coordinates, and the tablet input area. `src/app/ui/DeviceTestWindow.java`
- `DeviceTestWindow(BandClient pBandClient, GlfwGamepadRuntime pGamepadRuntime)` — starts a device-test session when shown and closes its resources when disposed.

### TabletInputPanel
Captures local pointer position and press/release state for tablet testing. `src/app/ui/TabletInputPanel.java`
- `TabletInputPanel(Consumer<TabletInput> pInputConsumer)` — reports normalized pointer inputs; it does not install global input capture.

### powerBand
Application entrypoint. `src/app/powerBand.java`
- `main(String[] pArgs)` — starts Swing and runs GLFW polling on the creating main thread until the main window closes.

### IConstants
Menu labels, UI colors, and configurable microphone mixer name. `src/app/util/IConstants.java`

## Constants
- `MICROPHONE_DEVICE_NAME` — exact mixer name to select; an empty value uses the system default recording input.
