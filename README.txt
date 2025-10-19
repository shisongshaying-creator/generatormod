# Source Installation Guide for Modders

## Quickstart
1. Open a terminal and navigate to the folder where you extracted the project zip.
2. Decide which IDE you want to use and follow the relevant setup instructions below.

## Setting Up the Development Environment
### Eclipse
1. Run `gradlew genEclipseRuns` (use `./gradlew genEclipseRuns` on macOS/Linux).
2. Open Eclipse and choose **File → Import → Existing Gradle Project**, then select the project folder.
   - Alternatively, run `gradlew eclipse` to generate the project files before importing.

### IntelliJ IDEA
1. Start IntelliJ IDEA and select **Import Project**.
2. Choose the `build.gradle` file and let IntelliJ import the Gradle project.
3. Run `gradlew genIntellijRuns` (or `./gradlew genIntellijRuns` on macOS/Linux) to create run configurations.
4. If dependencies appear missing, refresh the Gradle project from within IntelliJ.

## Troubleshooting Builds
- Run `gradlew --refresh-dependencies` to refresh the local Gradle cache when libraries fail to resolve.
- Execute `gradlew clean` to reset generated files without affecting your source code, then repeat the setup steps.

## Mapping Names
By default the MDK uses official Mojang mappings for Minecraft methods and fields. These names are covered by a specific license. If you prefer community-sourced names, change the mapping configuration in `build.gradle`. For the latest Mojang mapping license text, refer directly to the mapping file or the reference copy at:
<https://github.com/MinecraftForge/MCPConfig/blob/master/Mojang.md>

## Additional Resources
- Community Documentation: <http://mcforge.readthedocs.io/en/latest/gettingstarted/>
- LexManos' Install Video: <https://www.youtube.com/watch?v=8VEdtQLuLO0>
- Forge Forum: <https://forums.minecraftforge.net/>
- Forge Discord: <https://discord.gg/UvedJ9m>
