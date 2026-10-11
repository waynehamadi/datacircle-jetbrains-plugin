plugins {
    // Downloads the JDK 21 the toolchain below asks for when the machine has none.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "datacircle-mcp-server"
