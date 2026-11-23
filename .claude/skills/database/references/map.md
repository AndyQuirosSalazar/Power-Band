# Database

After you change this layer, update this file so the next run stays current.

Source: `src/app/storage`, `src/app/sorting`, `src/app/exceptions`, `src/app/model`, `src/app/util`

## Implemented
- Fixed-width music headers and command records, a sorted binary index, and a chronological text list.
- Disk-based binary search, duplicate rejection, and partial-library initialization choices.
- Physical deletion compacts the music file, adjusts index offsets, and filters the chronological list.
- External Balanced Merge Sort for index records.

## Classes
### RecordingLibrary
Persistence contract used by recording workflows. `src/app/storage/RecordingLibrary.java`
- `initialize(PartialFilesDecision pDecision)` — creates or initializes the recording file set.
- `recordingExists(String pName)` — checks a name in the sorted index.
- `addRecording(String pName, Instant pCreatedAt, List<CommandRecord> pCommands)` — appends a recording and updates the library.
- `deleteRecording(String pName)` — removes a recording and updates all three files, or returns `false` when absent.
- `listRecordings()` — returns immutable recording metadata in `recordings.list` order.
- `readRecording(String pName)` — returns the selected command sequence or an empty result if absent.

### RecordingStorage
Implements the recording file formats and initialization. `src/app/storage/RecordingStorage.java`
- `RecordingStorage(Path pDirectory)` — uses the supplied directory for recording files.
- `initialize(PartialFilesDecision pDecision)` — creates files or applies a partial-library decision.
- `recordingExists(String pName)` — searches the index using disk seeks.
- `addRecording(String pName, Instant pCreatedAt, List<CommandRecord> pCommands)` — appends music, index and list entries.
- `deleteRecording(String pName)` — validates the library, stages compacted replacements, and removes the selected recording.
- `listRecordings()` — reads validated metadata without creating or changing files.
- `readRecording(String pName)` — searches the index by binary seek and reads the selected header range.

### RecordingMetadata
Immutable recording name and creation timestamp. `src/app/model/RecordingMetadata.java`
- `RecordingMetadata(String pName, Instant pCreatedAt)` — stores the display metadata.
- `getName()` — returns the recording name.
- `getCreatedAt()` — returns the creation timestamp.

### InitializationResult
Initialization outcomes. `src/app/storage/InitializationResult.java`
- Values: `READY`, `DECISION_REQUIRED`, `CANCELLED`.

### PartialFilesDecision
Actions for a partial recording library. `src/app/storage/PartialFilesDecision.java`
- Values: `RESET`, `CREATE_MISSING`, `CANCEL`.

### RecordingStorageException
Checked storage error. `src/app/exceptions/RecordingStorageException.java`
- `RecordingStorageException(String pMessage)` — creates an error with a message.
- `RecordingStorageException(String pMessage, Throwable pCause)` — creates an error with its cause.

### BalancedMergeSort
Sorts fixed-width index records using external files. `src/app/sorting/BalancedMergeSort.java`
- `sort(Path pIndexFile)` — externally sorts an index file.
- `compareIndexNames(byte[] pRecordA, byte[] pRecordB)` — compares the fixed-width ASCII names.

### CommandRecord
Serializes a synth command into its fixed-width binary layout. `src/app/model/CommandRecord.java`
- `toByteArray()` — serializes a 48-byte big-endian record.
- `fromByteArray(byte[] pBytes)` — reads and validates a record.

## Constants
- `RECORDINGS_MUSIC_FILE_NAME`, `RECORDINGS_INDEX_FILE_NAME`, `RECORDINGS_LIST_FILE_NAME` — recording file names.
- `RECORDING_NAME_BYTES`, `RECORDING_NAME_MAX_LENGTH`, `RECORDING_HEADER_BYTES`, `RECORDING_INDEX_RECORD_BYTES`, `COMMAND_RECORD_BYTES` — file format widths.
- Constants are declared in `src/app/util/IConstants.java`.
