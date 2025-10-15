plugins {
    java
    id("org.beryx.jlink") version "3.1.1"
}

sourceSets {
    create("tools") {
        java {
            srcDir("src/tools")
        }
    }
    main {
        java {
            srcDir("src/main")
        }
        resources {
            srcDirs("src/assets", "src/assets-gen")
        }
    }
}

tasks.named<JavaCompile>("compileToolsJava") {
    dependsOn(tasks.compileJava)
    classpath = sourceSets["main"].runtimeClasspath + sourceSets["main"].output
}

val genatlas = tasks.register<JavaExec>("genatlas") {
    mustRunAfter(tasks.classes)
    classpath = sourceSets["tools"].runtimeClasspath +
            sourceSets["main"].runtimeClasspath +
            sourceSets["main"].output

    workingDir = rootDir
    mainClass.set("core.tool.AtlasGenerator")
}

tasks.register<JavaExec>("gentypes") {
    mustRunAfter(tasks.classes)
    classpath = sourceSets["tools"].runtimeClasspath +
            sourceSets["main"].runtimeClasspath +
            sourceSets["main"].output

    workingDir = rootDir
    mainClass.set("core.tool.ConvertTypesToJson")
}

tasks.classes {
    finalizedBy(genatlas)
}

val lwjglVersion = "3.3.3"
val lwjglNatives = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        arrayOf("Linux", "FreeBSD", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
                "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
            else
                "natives-linux"
        arrayOf("Windows").any { name.startsWith(it) }                           ->
            if (arch.contains("64"))
                "natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
            else
                "natives-windows-x86"
        else -> throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
    }
}


allprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "java")

    dependencies {
        implementation("com.google.code.gson:gson:2.10.1")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
        compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
    }

    tasks.compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("it.unimi.dsi:fastutil:8.5.15")
    implementation("org.apache.logging.log4j:log4j-api:3.0.0-beta2")
    implementation("org.apache.logging.log4j:log4j-core:3.0.0-beta2")
    implementation("org.apache.logging.log4j:log4j-iostreams:3.0.0-beta2")

    implementation("org.jcodec:jcodec:0.2.5")
    implementation("org.jcodec:jcodec-javase:0.2.5")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-jemalloc")

    implementation("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-jemalloc", classifier = lwjglNatives)
}

application {
    mainClass = "core.Main"
    mainModule = "core.main"
}

jlink {
    mergedModule {
        requires("java.desktop")
    }
    enableCds()
    options.addAll(listOf(
        "--no-header-files",
        "--no-man-pages",
    ))
    if (System.getProperty("os.name")!!.startsWith("Linux")) {
        options.add("--strip-native-debug-symbols")
        options.add("exclude-debuginfo-files");
    }
    launcher {
        name = "celsius"
        args = listOf("--packaged")
    }
}

tasks.named("jpackageImage") {
    dependsOn(tasks.createDelegatingModules)
}
