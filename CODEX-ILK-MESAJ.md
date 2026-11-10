You are finishing the Gallerypro Android app (Kotlin + Jetpack Compose) in this repository. Claude wrote everything so far; you continue alone.

Before doing anything, read AGENTS.md and DEVAM-NOTU.md from start to finish. They are in Turkish and they are the single source of truth for the current state, rules and known pitfalls.

## Rules
- Reply to me in Turkish. Commit messages in Turkish. Every on-screen text goes to res/values (Turkish) and res/values-en (English); no hardcoded strings.
- Compose foundation only (no Material3). Glass only via Modifier.glass; colors, sizes and motion only from designsystem/Tokens.kt; icons in designsystem/icons/GalleryIcons.kt.
- Never add the INTERNET permission; no data leaves the phone.
- Versions only in gradle/libs.versions.toml. No new libraries and no version upgrades unless the task below allows it.
- Do not touch the CLIP model files, scripts/models/ or the GitHub workflows unless the task requires it.
- Clamp animated Dp/Float values before padding/size/alpha (coerceAtLeast(0.dp), coerceIn(0f, 1f)); spring overshoot has crashed the app before.
- Heavy work (indexing, decoding) never on the main thread. Keep scrolling smooth.
- You probably cannot build Android locally. GitHub Actions builds and tests every push; a task is done only when Actions is green.

## Workflow for every task
1. Start from the latest main, create a branch named as given below.
2. Implement. Put pure logic in plain Kotlin and add unit tests in app/src/test/.
3. Push, wait for Actions. If red, fix only the failing lines.
4. Bump versionCode +1 and versionName (given below) in app/build.gradle.kts.
5. Update DEVAM-NOTU.md: status table, what was done, what was postponed, next task.
6. Open a pull request to main, give me the APK link (https://github.com/mmustafatasoy/Gallerypro/releases/download/<branch>/Gallerypro.apk), then STOP and wait. I test, merge and write "devam". Then start the next task.

If a decision is not covered here (license, new library, design change), stop, give me two options in Turkish and wait.

## Tasks, in this order

### 1. Similar Photos — branch `asama-5b2-benzer`, 0.6.1
In the viewer's info panel (feature/viewer/InfoPanel.kt) add a "Benzer Fotoğraflar / Similar Photos" card: a horizontal strip of up to 12 thumbnails whose CLIP vectors (IndexStore.vectors) are closest to the open item, excluding itself, cosine ≥ 0.80, best first. Tapping one opens it in the viewer. Hide the card when the open item has no vector or nothing passes the threshold. Hidden and trashed items never appear. Add `VectorMath.similar(...)` with tests.

### 2. Smart Labels card — branch `asama-5b3-etiketler`, 0.6.2
In the info panel add an "Akıllı Etiketler / Smart Labels" card: the item's labels as chips (Turkish names via LabelDictionary when the UI is Turkish) and the first ~120 characters of recognized text. Tapping a chip opens search with that word: extend the search launcher (feature/search/SearchLauncher.kt) so it can open with an initial query. Hide the card when there is no index entry.

### 3. Video keyframes — branch `asama-5c1-video`, 0.6.3
Index videos from 3 frames (10%, 50%, 90% of duration, MediaMetadataRetriever.getFrameAtTime, scaled to ≤640 px): labels = union of the frames (keep highest confidence order, max 8), no OCR, CLIP vector = normalized mean of the 3 frame vectors. Re-index only existing video entries (a video-only version check); photos must not be re-processed. Recycle bitmaps; skip a video on any error.

### 4. Places — branch `asama-5c2-yerler`, 0.7.0
- Offline city lookup: allowed data source is GeoNames cities15000 (CC BY 4.0). Download it in apk.yml at build time (like fonts/models; never commit it), convert to a compact asset (name, Turkish name if available from alternatenames is optional, country code, lat, lon), and add the attribution to assets/licenses and README.
- The indexer reads GPS from EXIF the same way data/media/MediaDetails.kt does (only when ACCESS_MEDIA_LOCATION is granted; MediaStore.setRequireOriginal) and stores lat, lon and nearest city + country in the index (new table or columns; bump the index.db version with a non-destructive upgrade).
- Search: city and country names become searchable (score like album names). "İstanbul", "istanbul", "Antalya 2024" must work.
- Collections tab: a "Yerler / Places" section listing cities with photo count and cover, newest first; tapping opens the existing collection grid. No map tiles (no internet).

### 5. Memories — branch `asama-5c3-anilar`, 0.7.1
Collections tab: an "Anılar / Memories" section with cards built on device by pure Kotlin logic (`MemoryBuilder`, tested):
- Trips: consecutive days with ≥ 8 photos in a city that is not the user's home city (home = most frequent city). Title "Antalya · Temmuz 2025".
- "Bu gün / On this day": photos from today's date in previous years (≥ 3 photos).
- Year highlights: one card per past year with its best ~30 photos (favorites first, then evenly spread over the year, no screenshots, no near-duplicates by CLIP cosine ≥ 0.95).
Cover = favorite or first photo. Tapping opens the grid. No slideshow yet.

### 6. People — DO NOT START, ask me first
Face recognition needs a face-embedding model, which is a license decision. Prepare two options (with license names and APK size impact) and wait for my choice. If I say "atla", skip it.

### 7. Polish and release prep — branch `asama-6-cilalama`, 0.8.0
- Performance: count glass surfaces on each screen; the design doc says at most 4 at once. Reduce or disable refraction where there are more. Check that scrolling the library with 10,000 items stays smooth on a release APK.
- Settings / About screen (from the Library "⋯" menu): version, open-source licenses (show every file in assets/licenses), "Re-index" button, index status.
- Clean up `files/originals` backups of deleted photos.
- Fix anything from DEVAM-NOTU "ertelenenler" lists only if it is small; list the rest in the notes.

### 8. Play Store prep — ask me before starting
The app name, the icon and the real release key are my decisions; ask me. Then: add a release-key signing setup that reads the keystore from GitHub secrets (I add the secrets myself), build an AAB, remove the arm64-only abiFilters for the AAB, and move the CLIP models into an install-time asset pack (Play Asset Delivery) because Play limits the base download size. Assets in an install-time pack are still read through AssetManager; verify ClipAssets still finds them.
