# Contributing to Canvas Note & AI Tutor

Thank you for your interest in contributing!

## Code Architecture Guidelines
1. **Coordinate Separation**: Keep page coordinates and screen coordinates distinct. Never store physical screen pixels in database entities or protocol payloads.
2. **Transform Single Source of Truth**: All drawing components (`rulemeter`, background, PDF bitmaps, and dry ink strokes) must be rendered inside a synchronized `canvas.concat(transform.toPageToScreenAndroidMatrix())` block.
3. **Jetpack Ink Encapsulation**: Keep `androidx.ink` dependencies isolated within the `:ink` module. Higher-level modules should interface solely via the `:model` contracts.
4. **Automated Testing**: Any changes to transform logic or coordinate calculation must be accompanied by round-trip invariance tests in `ViewportTransformComprehensiveTest.kt`.

## Pull Request Process
1. Ensure both the Android unit test suite (`./gradlew testDebugUnitTest`) and the Python backend tests (`pytest -v`) pass locally.
2. Provide a clear description of changes and attach test logs or physical device verification recordings where applicable.
