# MY IPTV 0.7.1 — Logo Reveal and Modern Home

- Plays the supplied ten-second logo-reveal clip offline when opening the app. Skip Intro and remote Back continue immediately. Playback errors and a fifteen-second watchdog also continue into the app.
- Replay Logo on the home screen shows the same reveal and returns to the existing home without resetting the active playlist.
- A new blue/green home layout includes vector icons, live library counts, an active-playlist label, Continue Watching / Favorites / Recently Added shortcuts, website, photo backgrounds and language controls. The body scrolls on smaller screens.
- Add Playlist is a prominent direct action in the home and library headers. My Playlists opens saved lists for switching, renaming or removal. Playlist types remain Xtream Codes, M3U / M3U8 and direct streams.
- Existing full-screen playback, picture sizing, named playlists, resume and watch marks remain available.

The embedded clip is re-encoded from the supplied MP4 as H.264 Main / AAC LC for broad Android and Fire TV compatibility. No network request is needed for the reveal. LogoRevealActivity is the launcher; MainActivity is internal. The reveal preserves progress when the app backgrounds and guards the handoff against duplicate navigation.

Validation: compile and existing parser / playlist / resume tests in GitHub Actions; inspect the packaged raw video and launcher manifest. Physical Fire Stick acceptance should include cold start, Skip Intro, Back, replay, background/resume, Add Playlist and saved-playlist switching. This is a debug beta, so Android may reject installation over an APK signed with a different debug key; uninstalling the previous beta removes its local settings.
