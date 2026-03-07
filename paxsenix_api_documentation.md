# Paxsenix Apple Music Lyrics API Documentation

This document details the structure and behavior of the Paxsenix Apple Music lyrics API based on testing conducted on March 7, 2026.

## API Endpoints

### 1. Song Search
**Endpoint:** `https://lyrics.paxsenix.org/apple-music/search?q=<QUERY>`  
**Description:** Searches for songs and returns a list of metadata objects including the `id` required for fetching lyrics.

### 2. Fetch Lyrics
**Endpoint:** `https://lyrics.paxsenix.org/apple-music/lyrics?id=<SONG_ID>`  
**Description:** Fetches detailed lyrics data, including word-by-word timing and metadata.

---

## Response Structure

The API returns a JSON object with the following primary fields:

| Field | Type | Description |
| :--- | :--- | :--- |
| `type` | String | Sync level. Returns `"Syllable"` for word-level synced lyrics. |
| `metadata` | Object | Contains `songwriters` (array of strings). |
| `content` | Array | Objects containing line-level timing and an array of word-level timing. |
| `elrc` | String | Enhanced LRC format with word-level tags. |
| `elrcMultiPerson`| String | Enhanced LRC with vocalist markers (e.g., `v1:`, `v2:`). |
| `plain` | String | Plain text lyrics without timing data. |
| `ttmlContent` | String | Raw TTML XML source from Apple Music. |
| `track` | Object | Detailed song metadata (artwork, duration, genres, audio traits). |

---

## Timing Data Formats

### 1. JSON (`content` array)
Each line in the `content` array contains a `text` array. Each object in `text` represents a word or syllable:

```json
{
  "text": "Christmas",
  "timestamp": 15788,
  "endtime": 16302,
  "duration": 514,
  "part": false
}
```
*   **Timestamp**: Start time in absolute milliseconds.
*   **Endtime**: End time in absolute milliseconds.
*   **Part**: Boolean indicating if the entry is a fragment of a word.

### 2. Enhanced LRC (`elrc` / `elrcMultiPerson`)
The raw format for a line in `elrcMultiPerson` looks like this:
`[00:14.945]v1: <00:14.945>We <00:15.249>could <00:15.465>leave <00:15.633>the <00:15.788>Christmas <00:16.302>lights <00:16.884>up <00:17.201>till <00:18.020>January<00:19.161>`

*   **Line Marker**: Standard LRC `[MM:SS.mmm]` at the start.
*   **Vocalist Marker**: `v1:` or `v2:` follows the line timestamp.
*   **Word Timing**: Each word is prefixed by its start time in `<MM:SS.mmm>` format.
*   **Explicit End**: The final word in a line is followed by an explicit end timestamp (e.g., `<00:19.161>`).

---

## Technical Observations

*   **Word-by-Word Timing**: Supported for most major tracks. The `type` field accurately identifies this as `"Syllable"`.
*   **Duets/Multi-Vocalist**: Correctly handled via the `elrcMultiPerson` field and `ttm:agent` attributes in the `ttmlContent`.
*   **End Timestamps**: Unlike standard LRC which only provides start times, this API provides **BOTH** start and end timestamps for every word, allowing for precise highlighting and progress tracking.
*   **Timestamp Format**: Uses standard `MM:SS.mmm` (minutes:seconds.milliseconds) within the ELRC strings.
