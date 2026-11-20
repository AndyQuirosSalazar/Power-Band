# Backend

After you change this layer, update this file so the next run stays current.

Source: `src/app/bridge`, `src/app/logic`, `src/app/model`, `src/app/supercollider`, `src/app/util`

## Implemented
- Bridge instruments translate allowed device input into synth commands.
- Input sessions route gamepad, microphone-level and tablet signals.
- Recording capture timestamps successful synth commands; save service validates and persists completed sessions.
- Recording playback maps stored synth records to Bridge commands and dispatches them against monotonic deadlines.
- `BandClient` sends OSC messages to SuperCollider.

## Classes
### SynthCommand
Bridge command contract. `src/app/bridge/commands/SynthCommand.java`
- `toCommandRecord(long pRelativeTimeMillis)` — converts a synth command to a recording record.
- `execute(BandClient pBandClient)` — sends the command to its synth.

### BassCommand
Timed bass note. `src/app/bridge/commands/BassCommand.java`
- `BassCommand(int pNote, int pDurationMillis)` — creates a bass note command.
- `getNote()` — returns its MIDI note.
- `getDurationMillis()` — returns its duration.
- `toCommandRecord(long pRelativeTimeMillis)` — serializes its values to a recording record.
- `execute(BandClient pBandClient)` — plays the bass note.

### PianoCommand
Timed piano chord. `src/app/bridge/commands/PianoCommand.java`
- `PianoCommand(int pNote, int pDurationMillis, boolean pMajor)` — creates a chord command.
- `getNote()` — returns its MIDI note.
- `getDurationMillis()` — returns its duration.
- `isMajor()` — returns its chord quality.
- `toCommandRecord(long pRelativeTimeMillis)` — serializes its values to a recording record.
- `execute(BandClient pBandClient)` — plays the piano chord.

### OrganCommand
Volume-controlled organ note. `src/app/bridge/commands/OrganCommand.java`
- `OrganCommand(float pVolume)` — creates an organ command.
- `getVolume()` — returns its normalized volume.
- `toCommandRecord(long pRelativeTimeMillis)` — serializes its values to a recording record.
- `execute(BandClient pBandClient)` — starts or stops the organ note.

### Instrument<T>
Bridge abstraction for translating input. `src/app/bridge/instruments/Instrument.java`
- `translate(T pInput)` — returns an optional synth command.

### GamepadInstrument
Maps directional changes to bass notes. `src/app/bridge/instruments/GamepadInstrument.java`
- `directionFromAxes(float pHorizontal, float pVertical)` — returns the dominant direction above threshold.
- `translate(GamepadDirection pDirection)` — emits a command for a new direction.

### TabletInstrument
Maps tablet coordinates and touch to piano chords. `src/app/bridge/instruments/TabletInstrument.java`
- `translate(TabletInput pInput)` — emits a chord only while touching.

### MicrophoneInstrument
Quantizes volume into organ commands. `src/app/bridge/instruments/MicrophoneInstrument.java`
- `translate(Float pVolume)` — emits a command when its quantized level changes.

### CommandRecord
Validated fixed-width synth command and relative time. `src/app/model/CommandRecord.java`
- `CommandRecord(long pRelativeTimeMillis, int pSynthId, int pNote, int pDurationMillis, int pMajor, float pVolume)` — validates recording fields.
- `getRelativeTimeMillis()` — returns the event time.
- `getSynthId()` — returns the synth identifier.
- `getNote()` — returns the MIDI note.
- `getDurationMillis()` — returns the command duration.
- `getMajor()` — returns the chord-quality flag.
- `getVolume()` — returns normalized volume.
- `toByteArray()` — writes the 48-byte big-endian record.
- `fromByteArray(byte[] pBytes)` — reads and validates a record.

### GamepadDirection
Directional axis states, including `NONE`. `src/app/model/GamepadDirection.java`

### TabletInput
Normalized pointer coordinates and contact state. `src/app/model/TabletInput.java`
- `TabletInput(float pX, float pY, boolean pTouching)` — validates normalized coordinates.
- `getX()` — returns normalized X.
- `getY()` — returns normalized Y.
- `isTouching()` — returns contact state.

### DeviceTestSession
Routes device events through instruments to a command consumer. `src/app/logic/DeviceTestSession.java`
- `DeviceTestSession(GamepadInputSource pGamepadSource, MicrophoneInputSource pMicrophoneSource, Consumer<SynthCommand> pCommandConsumer, Consumer<Float> pMicrophoneLevelConsumer, Consumer<String> pStatusConsumer)` — configures input sources and callbacks.
- `start()` — starts input sources and reports status.
- `acceptTabletInput(TabletInput pInput)` — routes tablet input.
- `close()` — closes input sources and ignores later commands.

### GamepadInputSource
Gamepad source contract. `src/app/logic/GamepadInputSource.java`
- `start(Consumer<GamepadDirection> pInputConsumer, Consumer<String> pStatusConsumer)` — starts direction reporting.
- `close()` — releases the source.

### GlfwGamepadInputSource
Adapter between the input contract and GLFW runtime. `src/app/logic/GlfwGamepadInputSource.java`
- `GlfwGamepadInputSource(GlfwGamepadRuntime pRuntime)` — binds the source to the runtime.
- `start(Consumer<GamepadDirection> pInputConsumer, Consumer<String> pStatusConsumer)` — registers callbacks.
- `close()` — unregisters the source.

### GlfwGamepadRuntime
Polls GLFW on its creating thread. `src/app/logic/GlfwGamepadRuntime.java`
- `GlfwGamepadRuntime()` — records the owner thread.
- `registerSource(Consumer<GamepadDirection> pInputConsumer, Consumer<String> pStatusConsumer)` — registers a listener.
- `unregisterSource(long pSourceId)` — removes a listener.
- `runWhile(BooleanSupplier pKeepRunning)` — polls until the application stops.
- `close()` — releases GLFW resources.

### MicrophoneInputSource
Microphone level-source contract. `src/app/logic/MicrophoneInputSource.java`
- `start(Consumer<Float> pVolumeConsumer, Consumer<String> pStatusConsumer)` — starts normalized volume reporting.
- `close()` — stops capture.

### JavaSoundMicrophoneInputSource
Reports RMS from temporary PCM buffers. `src/app/logic/JavaSoundMicrophoneInputSource.java`
- `start(Consumer<Float> pVolumeConsumer, Consumer<String> pStatusConsumer)` — starts microphone capture on a daemon thread.
- `close()` — stops and closes the audio line.

### RecordingCommandCapture
Converts dispatched synth commands into an ordered timed recording. `src/app/logic/RecordingCommandCapture.java`
- `start()` — starts a new capture period.
- `record(SynthCommand pCommand)` — records a command with its relative time.
- `finish()` — stops capture and returns an immutable command list.

### RecordingSaveService
Validates a name and coordinates library initialization and storage. `src/app/logic/RecordingSaveService.java`
- `RecordingSaveService(RecordingLibrary pLibrary)` — configures the persistence dependency.
- `save(String pRawName, List<CommandRecord> pCommands, Instant pCreatedAt, PartialFilesDecision pPartialFilesDecision)` — validates and saves, or returns the required user decision.
- `normalizeName(String pRawName)` — removes outer ASCII spaces.

### RecordingPlayer
Reads and schedules a selected recording through an injectable playback session. `src/app/logic/RecordingPlayer.java`
- `RecordingPlayer(RecordingLibrary pRecordingLibrary)` — configures storage with a real SuperCollider session and monotonic clock.
- `RecordingPlayer(RecordingLibrary pRecordingLibrary, PlaybackSessionFactory pSessionFactory, LongSupplier pNanoTime, Sleeper pSleeper)` — injects the session and clock functions.
- `listRecordings()` — returns library metadata for the selector.
- `play(String pName)` — dispatches recorded commands on absolute timeline deadlines.
- `Result` — reports `COMPLETED`, `NOT_FOUND`, or `CANCELLED`.
- `PlaybackSession.execute(SynthCommand pCommand)` — sends one mapped command.
- `PlaybackSession.close()` — closes the playback output session.
- `PlaybackSessionFactory.open()` — opens a playback session.
- `Sleeper.sleep(long pNanoseconds)` — waits for a timeline interval.

### BandClient
Sends synth control messages to SuperCollider. `src/app/supercollider/BandClient.java`
- `BandClient()` — connects to the default host and port.
- `BandClient(String host, int port)` — connects to the supplied host and port.
- `playPiano(int note, int durMs)` — plays a major piano chord.
- `playPiano(int note, int durMs, boolean major)` — plays a major or minor chord.
- `playBass(int note, int durMs)` — plays a bass note.
- `noteOn(int note)` — starts an organ note with default partials.
- `noteOn(int note, float[] ratios, float[] amps)` — starts an organ note with supplied partials and amplitudes.
- `noteOff(int note)` — stops an active organ note.
- `allNotesOff()` — stops all active organ notes.
- `close()` — releases the client socket and active organ notes.
- `main(String[] args)` — demonstrates synth playback.

### IConstants
Shared UI labels and synth/recording format values. `src/app/util/IConstants.java`
- `BUTTON_RECORD`, `BUTTON_FINISH_RECORDING`, `BUTTON_SAVE`, `BUTTON_CANCEL` — labels used by the recording workflow.
- `SYNTH_ID_BASS`, `SYNTH_ID_PIANO`, `SYNTH_ID_ORGAN` — synth identifiers stored in command records.
- `PLAYBACK_*` — playback dialog labels and status messages.
