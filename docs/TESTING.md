# Test the Roku target

1. From a generated project, run `./gradlew packageRoku` with JDK 25.
2. Inspect the ZIP: `unzip -l build/roku/<project>.zip`. `manifest`,
   `source/main.brs`, `components/MainScene.xml`,
   `components/MainScene.brs`, and `images/icon.png` must be at the ZIP root.
3. Optionally install BrighterScript from npm and validate extracted output:

   ```sh
   mkdir -p /tmp/roku-validation
   unzip -oq build/roku/<project>.zip -d /tmp/roku-validation
   bsc --root-dir /tmp/roku-validation --create-package false --copy-to-staging false
   ```

4. Enable Developer Mode on a Roku, open its device URL in a browser, sign in
   as `rokudev`, and upload the ZIP in Development Application Installer.
5. In the Hello World example, press Down and Up to change focus, OK to open
   **About** and **Counter**, and Back to return. On Counter, press OK on
   **Increment** several times; the displayed number must increase without
   restarting the channel. Navigate away and return; the value should remain
   until the app exits.

The Gradle build and BrighterScript validation check package structure and
syntax. They do not substitute for testing focus and rendering on Roku
hardware. Roku permits only one sideloaded app at a time.

## Device validation

On 2026-10-21, the project owner sideloaded the generated
`roku-hello-world.zip` on a Roku TV and reported successful operation. Photos
show the Home, About, and Counter pages, a focused button, and the counter
changing from 0 to 4. This confirms rendering, route navigation, remote-control
focus, and counter updates for that device and package. The photos do not test
packaged content images, custom palettes, or behavior across other Roku models.
