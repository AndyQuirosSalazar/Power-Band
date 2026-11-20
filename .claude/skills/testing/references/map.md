# Testing

After you change this layer, update this file so the next run stays current.

Source: `src/test/app`

## Test classes
### RecordingSavePanelTest
Verifies save-panel validation, partial-library cancellation, user cancellation and visible storage errors. `src/test/app/ui/RecordingSavePanelTest.java`
- `validatesNamesKeepsTheRecordingForCorrectionsAndSavesOnlyAfterSuccess()` — checks trimming, duplicate and invalid names, and confirmed success.
- `decliningPartialInitializationDoesNotWrite()` — checks cancelling the partial-library prompt.
- `cancelButtonDoesNotInitializeOrWrite()` — checks that cancelling before save does not access storage.
- `reportsStorageErrorsWithoutShowingSuccess()` — checks explicit failure reporting.

### RecordingCommandCaptureTest
Verifies command conversion and relative timestamps. `src/test/app/logic/RecordingCommandCaptureTest.java`
- `capturesSynthCommandsWithRelativeTimesAndStopsAfterFinishing()` — checks timed commands and that capture stops on finish.

### RecordingSaveServiceTest
Verifies save validation and initialization decisions using a fake library. `src/test/app/logic/RecordingSaveServiceTest.java`
- `trimsNamesValidatesPrintableAsciiAndKeepsCaseSensitiveDuplicatesDistinct()` — checks valid/invalid names and case sensitivity.
- `warnsForDuplicateAndRequiresAChoiceBeforeChangingPartialLibrary()` — checks duplicate rejection and partial-library outcomes.

### RecordingStorageTest
Verifies binary persistence, disk search, initialization choices, and malformed/incomplete data. `src/test/app/storage/RecordingStorageTest.java`
- `initializesEmptyLibraryAndPersistsBinaryAndChronologicalRecords()` — checks header offsets, command bytes, index record, and list timestamp.
- `searchesSortedIndexAndRejectsDuplicateWithoutChangingAnyFile()` — checks sorting, case sensitivity, search, and duplicate immutability.
- `createsOnlyMissingFilesWhenSelected()` — checks preservation of existing files.
- `resetsOnlyRecordingFilesWhenSelected()` — checks reset scope.
- `cancelsPartialInitializationWithoutChangingFiles()` — checks cancellation leaves the partial set unchanged.
- `rejectsMisalignedMusicAndIndexFilesWithoutRepairingThem()` — checks corrupt binary-file sizes.
- `rejectsInvalidNamesBeforeChangingFiles()` — checks invalid storage names.
- `rejectsIncompleteListLineBeforeChangingBinaryFiles()` — checks incomplete list entries.
- `listsChronologicallyAndReadsOnlyTheSelectedRecordingWithoutChangingFiles()` — checks read order, lookup and immutability.
- `readOnlyQueriesHandleAbsentAndPartialFileSetsWithoutCreatingOrChangingFiles()` — checks empty and incomplete libraries.
- `rejectsInvalidSelectedHeaderRangeWithoutRepairingFiles()` — checks invalid playback offsets.

### RecordingPlayerTest
Verifies playback mapping and timeline behavior with an injected session and clock. `src/test/app/logic/RecordingPlayerTest.java`
- `mapsCommandsAndUsesAbsoluteDeadlinesWithoutReorderingEqualTimestamps()` — checks synth mapping, tie order, and drift-free scheduling.
- `returnsNotFoundWithoutOpeningAPlaybackSession()` — checks explicit missing-name behavior.
- `propagatesTheFirstSendFailureAndClosesThePlaybackSession()` — checks failure propagation and resource cleanup.

### CommandRecordTest
Verifies fixed-width serialization and validation. `src/test/app/model/CommandRecordTest.java`
- `serializesAndReadsFixedWidthBigEndianRecord()` — checks the 48-byte layout and round trip.
- `rejectsInvalidRecordLengthAndNonzeroReservedBytes()` — checks malformed record bytes.
- `rejectsInvalidCommandFields()` — checks invalid field values.

### BalancedMergeSortTest
Verifies external index sorting and temporary-file handling. `src/test/app/sorting/BalancedMergeSortTest.java`
- `sortsSeveralDiskRunsAndPreservesOffsets()` — checks ordering over multiple runs.
- `handlesEmptyAndSingleRecordIndexes()` — checks zero- and one-entry indexes.
- `rejectsMisalignedIndexWithoutChangingItOrLeavingTemporaryFiles()` — checks corruption handling.

### SynthCommandSerializationTest
Verifies Bridge command conversion. `src/test/app/bridge/commands/SynthCommandSerializationTest.java`
- `createsRecordsForEachSynthWithoutTypeInspection()` — checks synth IDs and command values.
- `rejectsNegativeRelativeTimeForEveryCommand()` — checks invalid times.

### DeviceInstrumentTest
Verifies input-to-command mappings. `src/test/app/bridge/instruments/DeviceInstrumentTest.java`
- `mapsOnlyDirectionalGamepadChangesToBassNotes()` — checks directional filtering.
- `mapsTabletCoordinatesAndPlaysOnlyWhileTouching()` — checks tablet mapping and hover behavior.
- `mapsMicrophoneVolumeToQuantizedOrganAmplitudeAndSilence()` — checks volume quantization and note-off.

### BandClientTest
Verifies OSC routing and note lifecycle with a UDP loopback receiver. `src/test/app/supercollider/BandClientTest.java`
- `bassCommandSendsStringBassWithTheRequestedNoteAndDuration()` — checks bass controls.
- `pianoCommandSendsClassicPianoAndTheRequestedChordQuality()` — checks piano controls.
- `organCommandSendsOrganControlsAndStopsItsActiveNoteAtSilence()` — checks organ controls and note-off.
- `closingClientStopsActiveOrganAndClosesItsSocket()` — checks cleanup and closed-client behavior.

### DeviceTestSessionTest
Verifies device routing, source cleanup, and reported input levels. `src/test/app/logic/DeviceTestSessionTest.java`
- `routesEachSourceToOnlyItsAssignedSynthAndClosesSources()` — checks source-to-synth routing.

### GlfwGamepadRuntimeTest
Verifies generic joystick direction mapping. `src/test/app/logic/GlfwGamepadRuntimeTest.java`
- `mapsTheFirstTwoAxesOfAnUnmappedJoystick()` — maps the first two axes.
- `rejectsJoysticksWithoutTwoAxes()` — rejects incomplete axis data.

### MainMenuTest
Verifies menu layout and actions. `src/test/app/ui/MainMenuTest.java`
- `showsTheTitleAndButtonsInTheRequiredOrder()` — checks menu presentation.
- `testButtonRunsItsActionAndExitRunsItsAction()` — checks test and exit callbacks.
- `recordButtonRunsItsAction()` — checks the recording callback.
- `playButtonRunsItsAction()` — checks the playback callback.

### RecordingPlaybackPanelTest
Verifies asynchronous playback selection, cancellation, empty libraries, and visible errors. `src/test/app/ui/RecordingPlaybackPanelTest.java`
- `loadsChronologicalChoicesOffEdtAndPlaysOnlyTheConfirmedSelection()` — checks EDT responsiveness, ordered options, and selected playback.
- `cancellingTheSelectorDoesNotStartPlayback()` — checks selection cancellation.
- `showsTheEmptyLibraryMessageWithoutOpeningPlayback()` — checks the empty state.
- `reportsLibraryReadErrorsInsteadOfShowingAnEmptySuccess()` — checks explicit read errors.

### TabletInputPanelTest
Verifies local AWT pointer normalization and touch state. `src/test/app/ui/TabletInputPanelTest.java`
- `convertsLocalPointerEventsToNormalizedPenPositionAndTouchState()` — checks local pointer events.

## Test command
- `mvn test` — runs the JUnit 5 suite defined by `pom.xml`.
