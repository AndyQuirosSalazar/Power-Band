# Database

After you change this layer, update this file so the next run stays current.

Source: `src/app/storage`, `src/app/sorting`, `src/app/util`

## Implemented
- Fixed-width recording headers and command records in `recordings.music`, fixed-width name/offset records in `recordings.index`, and chronological entries in `recordings.list`: `src/app/storage/RecordingStorage.java`
- Disk-based binary search and duplicate rejection, plus recording-library initialization choices: `src/app/storage/RecordingStorage.java`
- External Balanced Merge Sort for fixed-width recording index entries: `src/app/sorting/BalancedMergeSort.java`

## Classes
### RecordingStorage
Creates/validates recording files, stores recording data, searches names on disk, and handles partial-library initialization. `src/app/storage/RecordingStorage.java`
- `RecordingStorage(Path pDirectory)` — uses the supplied directory for the three recording files.
- `initialize(PartialFilesDecision pDecision)` — creates missing files or reports/applies the selected partial-file decision.
- `recordingExists(String pName)` — searches the sorted index by binary seek.
- `addRecording(String pName, Instant pCreatedAt, List<CommandRecord> pCommands)` — appends music data, sorts the index, then appends the chronological list entry.

### InitializationResult
Reports whether storage is ready, needs a partial-file decision, or was cancelled. `src/app/storage/InitializationResult.java`
- Values: `READY`, `DECISION_REQUIRED`, `CANCELLED`.

### PartialFilesDecision
The available actions for a partially present recording library. `src/app/storage/PartialFilesDecision.java`
- Values: `RESET`, `CREATE_MISSING`, `CANCEL`.

### RecordingStorageException
Explicit checked error for invalid recording data or recording-file structure. `src/app/exceptions/RecordingStorageException.java`
- `RecordingStorageException(String pMessage)` — creates an error with a message.
- `RecordingStorageException(String pMessage, Throwable pCause)` — creates an error with its cause.

### BalancedMergeSort
Sorts 40-byte index records by their 32-byte ASCII name using alternating disk runs and temporary files. `src/app/sorting/BalancedMergeSort.java`
- `sort(Path pIndexFile)` — validates alignment, externally sorts the index, and cleans temporary files.
- `compareIndexNames(byte[] pRecordA, byte[] pRecordB)` — compares fixed-width names using the sort order.

## Constants
- `RECORDINGS_MUSIC_FILE_NAME`, `RECORDINGS_INDEX_FILE_NAME`, `RECORDINGS_LIST_FILE_NAME` — recording file names.
- `RECORDING_NAME_BYTES`, `RECORDING_NAME_MAX_LENGTH`, `RECORDING_HEADER_BYTES`, `RECORDING_INDEX_RECORD_BYTES`, `COMMAND_RECORD_BYTES` — recording field and record widths.
- Constants are declared in `src/app/util/IConstants.java`.
