# MY IPTV 0.6.1 — Playlists and Continue Watching

Returning from an episode restores remote focus to its series card (or its episode card when opened from Continue Watching).

Movies and episodes save their position every ten seconds and when leaving the player. Reopening an unfinished title offers Resume and Start from Beginning. Continue Watching contains unfinished videos from the active playlist. Live TV does not save a seek position. Resume requires a seekable provider stream.

Cards and episode lists show ◐ with a saved time for started videos and ✓ for completed videos. Cards include a progress bar. Selecting a series scrolls its episode picker to the last episode watched. Completed titles are removed from Continue Watching; selecting them starts at the beginning. Progress is stored locally, encrypted, for up to 200 videos across playlists. A title is marked complete at playback end, or within the last minute after at least 95% has played.

Playlists replaces Connections. Add a required playlist name plus Xtream credentials, an M3U URL, or a direct stream URL. Select a saved playlist to open, rename, or remove it. Only a successful import changes the active playlist. Existing single-playlist installations migrate to My Playlist. Favorites and progress are scoped by playlist identity; switching lists does not delete them. Credentials and watch metadata are encrypted with Android Keystore.

The home screen displays www.miptv.me and the active playlist name. Background opens a device image picker or accepts an HTTP(S) image URL. Images are limited to 8 MB, sampled to TV size, copied locally, and displayed under a dark overlay. Use default background restores the original theme. A Fire Stick without a document picker can use the image URL option.

Automated validation covers progress completion and resume boundaries, playlist isolation, migration, multiple-list round trips, and M3U parsing. Compilation is not a physical Fire Stick test. Validate remote navigation, actual provider seeking, exit/restart resume, episode marks, multiple-account switching, and photo import on the target device.

This remains a debug-signed beta. If Android rejects an update because the previous beta used a different debug signing key, uninstalling the old beta permits installation but deletes its local settings and history. A stable signing setup is required before distributing production updates.
