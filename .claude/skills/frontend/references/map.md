# Frontend

After you change this layer, update this file so the next run stays current.

Source: `src/app/ui`

## Implemented
- Swing menu, device-test window, and local tablet input panel.
- Recording window captures instrument commands and opens the asynchronous save flow.
- Save panel validates names and presents storage initialization choices and results.

## Entrypoints
- `src/app/powerBand.java` — creates the main menu and opens device-test and recording windows.

## Classes
### MainMenu
Displays the title and four actions. `src/app/ui/MainMenu.java`
- `MainMenu(Runnable pExitAction)` — builds a menu with default test and recording actions.
- `MainMenu(Runnable pExitAction, Runnable pTestAction)` — wires the test action.
- `MainMenu(Runnable pExitAction, Runnable pTestAction, Runnable pRecordAction)` — wires the test and recording actions.

### DeviceTestWindow
Shows device input and synth status during device testing. `src/app/ui/DeviceTestWindow.java`
- `DeviceTestWindow(BandClient pBandClient, GlfwGamepadRuntime pGamepadRuntime)` — starts and owns a device-test session.

### RecordingWindow
Captures commands from the gamepad, tablet and microphone-driven instrument. `src/app/ui/RecordingWindow.java`
- `RecordingWindow(GlfwGamepadRuntime pGamepadRuntime, RecordingLibrary pRecordingLibrary)` — creates a session with a new OSC client.
- `RecordingWindow(BandClient pBandClient, GlfwGamepadRuntime pGamepadRuntime, RecordingLibrary pRecordingLibrary)` — creates a session with injected dependencies.

### RecordingSaveDialog
Shows the save workflow for a completed recording. `src/app/ui/RecordingSaveDialog.java`
- `showDialog(Window pOwner, RecordingLibrary pLibrary, List<CommandRecord> pCommands, Instant pCreatedAt)` — opens the modal save UI.

### RecordingSavePanel
Validates and saves a recording asynchronously. `src/app/ui/RecordingSavePanel.java`
- `RecordingSavePanel(RecordingSaveService pSaveService, List<CommandRecord> pCommands, Instant pCreatedAt, Runnable pFinishedAction)` — creates a panel with Swing dialogs.
- `RecordingSavePanel(RecordingSaveService pSaveService, List<CommandRecord> pCommands, Instant pCreatedAt, Runnable pFinishedAction, Interaction pInteraction)` — creates a panel with injected UI interactions.
- `Interaction.showWarning(Component pParent, String pMessage)` — displays a name warning.
- `Interaction.choosePartialFilesDecision(Component pParent)` — obtains the user's partial-library decision.
- `Interaction.showSaved(Component pParent, String pName)` — confirms successful storage.
- `Interaction.showError(Component pParent, Throwable pError)` — displays a storage failure.

### TabletInputPanel
Captures local pointer position and press/release state. `src/app/ui/TabletInputPanel.java`
- `TabletInputPanel(Consumer<TabletInput> pInputConsumer)` — reports normalized pointer inputs.
