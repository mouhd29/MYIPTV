# MY IPTV — Fire TV landscape beta

The build extracts the original branded Android project and overlays the editable source in `tv/`. The earlier text-replacement scripts are no longer used.

## Version 0.3.0

- Fixed landscape home and player; dark navy and teal theme, four-column artwork grid, clear D-pad focus borders, category sidebar, search, and 60-title pages.
- Separate Connections dialog: Xtream Codes server / username / password, M3U playlist URL, or a direct HTTP(S) TS / HLS / MP4 link.
- Xtream Live TV, Movies, Series, category names, and season / episode selection. Series are fetched when opened.
- English / Arabic interface and RTL layout. Provider titles are preserved.
- Hold OK on a card to toggle Favorites. Recently Added contains up to 100 newly discovered titles compared with the in-memory previous import; the first load samples the last 100 catalog entries. It does not represent provider release dates.
- Saved connection is encrypted with an Android Keystore AES-GCM key; passwords are masked. Connections replace the active catalog only after successful import. API errors do not print credential-bearing URLs.
- Media3 HLS, progressive TS/MP4, and RTSP playback support; retry and error code display. Actual codec and provider compatibility depends on the device and source. DRM and provider-specific custom headers are not configured.

## Build

GitHub Actions runs parser unit tests and builds the debug beta APK. Branch builds upload `MYiptv-FireTV-Beta` under the run's Artifacts. Main builds also update the beta release. The APK is a debug beta, not a store-signed release. The GitHub runner's debug key may differ from your installed beta; if Android rejects an update, uninstall the previous beta first (this removes its saved settings).

## Fire Stick acceptance check

Install the Actions APK, confirm landscape layout, navigate all tabs / categories / cards with D-pad and Back, and check search, pagination, English / Arabic and favorite toggling. Connect a permitted test provider and verify Live TV TS, HLS, movie seeking, series episode selection, wrong credentials, offline retry, app background / resume, and reconnection after restart. No provider credentials are bundled. A successful compilation is not a physical-device playback test.
