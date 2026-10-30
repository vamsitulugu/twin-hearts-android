# Memories: photos/videos disappear a few seconds after adding

Unzip over the project root, Sync, Run. Only the Memories feature changed.

Two causes fixed
1. Reading the album: the app expected the server to answer { "state": { ... } }, but the server (routes/data.js) answers the saved document itself. Read that way, the album looked empty on every load, so the new photos (shown from the save result) were replaced by an empty list a few seconds later when a load finished. It now reads both shapes.
2. Old answer wins: a load that started before you added photos (the free server can take many seconds) could finish after the upload and put the old list back. Every add / delete / favourite now bumps a counter and a load that began earlier is ignored.

Files: memories/data/remote/MemoriesApi.kt, memories/data/MemoriesRepository.kt, memories/ui/MemoriesViewModel.kt

Tests
1. Open Memories, add 3 photos and 1 video at once, wait 60 s: all four stay.
2. Close and reopen the app: they are still there (and in the web app).
3. Add photos right after opening the page (while it still shows the spinner): they stay.
4. Delete one, mark one favourite: nothing else disappears.
