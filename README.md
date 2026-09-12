⏭ Ad Skipper

A tiny Android app that automatically taps the Skip button on skippableYouTube ads — so you never have to.

Built for personal use. Requests zero permissions (not even Internet),stores nothing outside your device, and does exactly one job.

✨ Features

    Automatic skipping — detects the skip button in the YouTube app themoment it becomes active and taps it
    Stats dashboard — ads skipped, estimated time saved, tracking-since date
    Zero permissions — no Internet, no storage access, no tracking; thecounter lives in local SharedPreferences and nowhere else
    No root required — uses Android's public AccessibilityService API
    Lightweight — ~1 MB APK, no dependencies, no ads (obviously)

🛠 How it works

    An AccessibilityService listens for screen changes — but only insidethe YouTube app (see res/xml/skip_ad_service_config.xml).
    On each change it looks for the skip button: first by known YouTube viewIDs (fast, language-independent), then by button text as a fallback("Skip", "Skip Ad", …).
    It clicks the button — via ACTION_CLICK on the node or its clickableparent, or by dispatching a synthetic tap as a last resort.

Only skippable ads are affected. Non-skippable ads show no button at all,so there is nothing to press — this app leaves them alone.

📲 Install
Option A — download the APK

    Go to Releases
    Download the latest .apk on your phone
    Open it → allow installs from this source → install
    Play Protect will warn (normal for any sideloaded accessibility app):More details → Install anyway

Option B — build it yourself, no Android Studio needed

    Push this project to your own GitHub account (fork or copy the files)
    Go to the Actions tab — a build starts automatically
    When it's green, open the run → Artifacts → download the APK

⚙️ Enable it

    Open Ad Skipper → tap Open Accessibility Settings
    Android 13+ only, if the toggle is greyed out:Settings → Apps → Ad Skipper → ⋮ → Allow restricted settings
    Settings → Accessibility → Downloaded apps → Ad Skipper → On
    Play any YouTube video — skippable ads get skipped automatically

🔧 Customization
What	Where
Button text for other languages	SKIP_TEXTS in SkipAdService.kt
More apps (Netflix "Skip Intro", browsers, …)	android:packageNames in res/xml/skip_ad_service_config.xml
Time-saved estimate per ad	EST_SECONDS_PER_AD in MainActivity.kt
YouTube view IDs after an app update	findAndClickSkip() in SkipAdService.kt

🔒 Privacy

    No permissions requested — verify yourself: Settings → Apps → Ad Skipper → Permissions
    No Internet access — nothing can be sent anywhere
    The accessibility service is restricted to com.google.android.youtube
    Stats (a counter and a date) are stored only on your device

🧰 Troubleshooting
Problem	Fix
Play Protect blocks install	More details → Install anyway
Toggle greyed out (Android 13+)	Settings → Apps → Ad Skipper → ⋮ → Allow restricted settings
Service stops working after a while	Disable battery optimization for the app
Doesn't skip in another language	Add your word to SKIP_TEXTS and rebuild
Stopped working after YouTube update	View IDs changed — text fallback usually survives; update the ID list

📁 Project structure

app/src/main/├── java/com/example/adskipper/│   ├── SkipAdService.kt        # The accessibility service doing the work│   └── MainActivity.kt          # Stats dashboard + service status└── res/    ├── layout/activity_main.xml    ├── values/  (strings, styles)    ├── xml/skip_ad_service_config.xml    └── drawable/ + mipmap-anydpi/  # UI + app icon.github/workflows/build.yml       # CI — builds the APK on every push

⚠️ Disclaimer

This is a personal-use project and is not affiliated with Google or YouTube.Automating ad interactions may conflict with YouTube's Terms of Service —use at your own discretion. If a creator's work matters to you, considerwatching their ads or subscribing to YouTube Premium.
📄 License

MIT


    
     
