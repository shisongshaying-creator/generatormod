# generatormod

## Overview
Generatormod is a starter Minecraft Forge mod project configured for Minecraft 1.20.1. The repository ships with a Gradle-based build that applies the ForgeGradle plugin, a preconfigured run directory, and publishing helpers so that you can focus on adding gameplay features instead of wiring up tooling from scratch.

## Purpose
This template demonstrates how to structure a Forge mod using official Mojang mappings, ForgeGradle 6, and the Gradle wrapper. It is intended for mod developers who want a minimal but functional foundation that builds, runs, and packages a mod without additional setup.

## Key Features
- Gradle wrapper with ForgeGradle 6 configured for Minecraft 1.20.1.
- Predefined client, server, and data generation run configurations.
- Maven publishing configuration that outputs artifacts to a local `mcmodsrepo` directory.
- Java 17 toolchain configuration compatible with the current Forge requirements.

## Requirements and Dependencies
- Java 17 JDK installed locally (the project enforces this via the Gradle toolchain).
- Gradle is provided via the included wrapper (`./gradlew` or `gradlew.bat`).
- Minecraft Forge dependency: `net.minecraftforge:forge:1.20.1-47.1.0`.
- Internet access for Gradle to resolve Forge and plugin artifacts on first run.

## Build and Run
1. Clone the repository and open a terminal in the project root.
2. Run `./gradlew genIntellijRuns` or `./gradlew genEclipseRuns` to generate IDE run configurations.
3. Use `./gradlew runClient` to start the modded client or `./gradlew runServer` for a dedicated server.
4. Build a distributable JAR with `./gradlew build`; the obfuscated mod JAR is produced after the `reobfJar` task finalizes the build.

## Development Workflow
- Generated resources are written to `src/generated/resources`; keep this directory under version control if you rely on data generation.
- When mappings or dependencies appear stale, run `./gradlew --refresh-dependencies` to refresh the cache, or `./gradlew clean` before regenerating IDE runs.
- Official Mojang mappings are used by default. Review the mapping license in `MCPConfig` if you plan to redistribute the names.

## Contributing
1. Fork the repository and create a feature branch.
2. Make your changes and ensure the project builds (`./gradlew build`).
3. Open a pull request summarizing the changes and include relevant testing details.
4. Follow standard Forge modding guidelines and respect the licensing terms of Forge and Mojang mappings.

## License
This project is distributed under the terms described in `LICENSE.txt`. Review the license before redistributing or modifying the template.
