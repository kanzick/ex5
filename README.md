## MockAPI setup

1. Create a project at https://mockapi.io/
2. Add two resources:
   - `categories`
   - `courses`
3. Create at least 3 categories and 10 courses matching the fields from the assignment.
4. Update the base URL in the app:
   - File: `androidApp/src/main/kotlin/org/example/ex5/MockApiService.kt`
   - Set `MockApiConfig.BASE_URL` to your project endpoint, for example:
     `https://<your-project-id>.mockapi.io/api/v1`

## Run the app

```bash
./gradlew :androidApp:assembleDebug
```

You can also run it from Android Studio using the Android app run configuration.

## Notes

- The app loads category names from the API instead of hardcoding them.
- Course and category operations use the REST endpoints from the assignment.
- For the trailer URL, use a direct MP4 link when possible so it plays inside the app or browser.
