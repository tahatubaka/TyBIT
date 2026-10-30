# TyBIT

A personal music player for Android, streaming from YouTube Music.

## Build

```bash
JAVA_HOME=~/.jdks/jdk-17.0.20.1+1 ./gradlew :app:assembleProdRelease
```

Signing: copy `keystore.properties.example` to `keystore.properties`.
The APKs land in `app/build/outputs/apk/prod/release/`.

Install on a phone over USB, then compile it ahead of time so it starts and
scrolls without hitching:

```bash
adb install -r app/build/outputs/apk/prod/release/app-prod-arm64-v8a-release.apk
adb shell cmd package compile -m speed -f com.tybit.app
```

## Branding

The logo art and the script that cuts it into every icon size are in
`branding/`.

## License

GPL-3.0 — see [LICENSE](LICENSE). Source file headers keep the copyright
notices of the code TyBIT is built from, as the license requires.
