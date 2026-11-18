# Database

After you change this layer, update this file so the next run stays current.

Source: `src/app/sorting`

## Implemented
- External Balanced Merge Sort for fixed-width recording index entries: `src/app/sorting/BalancedMergeSort.java`

## Classes
### BalancedMergeSort
Sorts 40-byte index records by their 32-byte ASCII name using alternating disk runs and temporary files. `src/app/sorting/BalancedMergeSort.java`
- `sort(Path pIndexFile)` — validates record alignment, externally sorts the file, and cleans temporary files.

## Constants
- `RECORDING_NAME_BYTES` and `RECORDING_INDEX_RECORD_BYTES` — fixed index-field and record widths declared in `src/app/util/IConstants.java`.
