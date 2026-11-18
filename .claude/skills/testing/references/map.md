# Testing

After you change this layer, update this file so the next run stays current.

Source: `src/test/app`

## Test classes
### RecordingStorageTest
Verifies binary persistence, disk-based search, initialization decisions, and malformed/incomplete data handling. `src/test/app/storage/RecordingStorageTest.java`
- `initializesEmptyLibraryAndPersistsBinaryAndChronologicalRecords()` — checks file creation, header offsets, command bytes, index record, and list timestamp.
- `searchesSortedIndexAndRejectsDuplicateWithoutChangingAnyFile()` — checks external index ordering, case sensitivity, search, and duplicate immutability.
- `createsOnlyMissingFilesWhenSelected()` — checks preservation of existing files when creating missing ones.
- `resetsOnlyRecordingFilesWhenSelected()` — checks reset of the three recording files only.
- `cancelsPartialInitializationWithoutChangingFiles()` — checks cancellation leaves the partial set unchanged.
- `rejectsMisalignedMusicAndIndexFilesWithoutRepairingThem()` — checks corrupt binary file sizes are rejected without repair.
- `rejectsInvalidNamesBeforeChangingFiles()` — checks invalid ASCII names fail without writes.
- `rejectsIncompleteListLineBeforeChangingBinaryFiles()` — checks incomplete list entries fail before binary data changes.

### CommandRecordTest
Verifies fixed-width big-endian serialization, reading, and record validation. `src/test/app/model/CommandRecordTest.java`
- `serializesAndReadsFixedWidthBigEndianRecord()` — checks the 48-byte layout, zero padding, and round-trip.
- `rejectsInvalidRecordLengthAndNonzeroReservedBytes()` — checks malformed record bytes.
- `rejectsInvalidCommandFields()` — checks invalid time, synth ID, and major flag.

### BalancedMergeSortTest
Verifies external sorting of fixed-width index records and temporary-file handling. `src/test/app/sorting/BalancedMergeSortTest.java`
- `sortsSeveralDiskRunsAndPreservesOffsets()` — checks ordering over multiple runs and retained offsets.
- `handlesEmptyAndSingleRecordIndexes()` — checks zero- and one-entry files.
- `rejectsMisalignedIndexWithoutChangingItOrLeavingTemporaryFiles()` — checks corruption handling.

### SynthCommandSerializationTest
Verifies each Bridge synth command converts itself to the correct recording record. `src/test/app/bridge/commands/SynthCommandSerializationTest.java`
- `createsRecordsForEachSynthWithoutTypeInspection()` — checks synth IDs and command values.
- `rejectsNegativeRelativeTimeForEveryCommand()` — checks invalid relative-time rejection.

### DeviceInstrumentTest
Verifies direction, tablet, and microphone-to-command mappings. `src/test/app/bridge/instruments/DeviceInstrumentTest.java`
- `mapsOnlyDirectionalGamepadChangesToBassNotes()` — checks directional filtering and bass notes.
- `mapsTabletCoordinatesAndPlaysOnlyWhileTouching()` — checks normalized position mapping, chord quality, and hover silence.
- `mapsMicrophoneVolumeToQuantizedOrganAmplitudeAndSilence()` — checks volume quantization and note-off at silence.

### BandClientTest
Verifies OSC synth routing and note lifecycle with a UDP loopback receiver. `src/test/app/supercollider/BandClientTest.java`
- `bassCommandSendsStringBassWithTheRequestedNoteAndDuration()` — checks the bass synth, note, and duration.
- `pianoCommandSendsClassicPianoAndTheRequestedChordQuality()` — checks piano synth controls and chord quality.
- `organCommandSendsOrganControlsAndStopsItsActiveNoteAtSilence()` — checks organ partials, amplitudes, and note-off.
- `closingClientStopsActiveOrganAndClosesItsSocket()` — checks organ cleanup and that a closed client cannot send.

### DeviceTestSessionTest
Verifies device routing, source cleanup, and reported input levels. `src/test/app/logic/DeviceTestSessionTest.java`
- `routesEachSourceToOnlyItsAssignedSynthAndClosesSources()` — feeds fake inputs and checks emitted commands.

### GlfwGamepadRuntimeTest
Verifies generic joystick direction mapping and rejects incomplete axis data. `src/test/app/logic/GlfwGamepadRuntimeTest.java`
- `mapsTheFirstTwoAxesOfAnUnmappedJoystick()` — maps the first two axes to a directional input.
- `rejectsJoysticksWithoutTwoAxes()` — rejects axis buffers with fewer than two axes.

### MainMenuTest
Verifies menu title, button order, visibility, and actions. `src/test/app/ui/MainMenuTest.java`
- `showsTheTitleAndButtonsInTheRequiredOrder()` — checks menu presentation.
- `testButtonRunsItsActionAndExitRunsItsAction()` — checks `Probar` and `Salir` callbacks.

### TabletInputPanelTest
Verifies local AWT pointer normalization and touch state. `src/test/app/ui/TabletInputPanelTest.java`
- `convertsLocalPointerEventsToNormalizedPenPositionAndTouchState()` — checks press/release inputs.

## Test command
- `mvn test` — runs the JUnit 5 suite defined by `pom.xml`.
