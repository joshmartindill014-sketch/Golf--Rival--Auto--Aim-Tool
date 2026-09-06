# Golf Rival Auto Aim — prototype

This is an Android Studio project intended for a quick on-screen wind reader.

What it currently does:
- requests Android MediaProjection screen capture
- uses an overlay over other apps
- OCRs the upper-right wind area based on the supplied Golf Rival screenshot
- extracts a wind value and displays a large quick-answer overlay

Important:
- The current prototype displays a fixed left aim direction after reading the wind. It is intentionally a safe calibration stage: direction recognition and club/elevation compensation should be calibrated against real screenshots before trusting it for shots.
- ML Kit Text Recognition is downloaded by Gradle/Android Studio.
- Build/install with Android Studio on an Android phone.
