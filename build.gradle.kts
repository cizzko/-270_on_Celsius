import groovy.json.JsonSlurper
import core.gen.glsl.GLSLPreprocessorTask;

plugins {
    java
    id("org.beryx.jlink") version "4.0.2"
    id("com.github.ben-manes.versions") version "0.54.0"
}

val MAIN_CLASS  = "core.Main"
val MAIN_MODULE = "core.main"

val generateUniforms = tasks.register<GLSLPreprocessorTask>("generateUniforms") {
    shadersDir.set(layout.projectDirectory.dir("src/assets/shaders"))
    outputDir.set(layout.buildDirectory.dir("generated/sources/generated/java/main"))
}

sourceSets {
    main {
        java {
            srcDir("src/main")
            srcDir(generateUniforms.flatMap { it.outputDir })
        }
        resources {
            srcDir("src/assets")
        }
    }

    create("tools") {
        java {
            srcDir("src/tools")
        }
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

tasks.named<JavaCompile>("compileToolsJava") {
    dependsOn(tasks.compileJava)
}

val syncTranslation = tasks.register<JavaExec>("syncTranslation") {
    mustRunAfter(tasks.classes)
    classpath = sourceSets["tools"].runtimeClasspath + sourceSets["main"].output

    workingDir = rootDir
    mainClass = "core.tool.lang.TranslationProcessor"
}

val genAtlas = tasks.register<JavaExec>("genAtlas") {
    mustRunAfter(tasks.classes)
    classpath = sourceSets["tools"].runtimeClasspath + sourceSets["main"].output

    workingDir = rootDir
    mainClass = "core.tool.AtlasGenerator"
}

// феерически не работает
// tasks.clean {
//     val assetsDir = layout.projectDirectory.dir("src/assets");
//     delete(assetsDir.files("sprites.atlas", "sprites.atlas.hash", "sprites.atlas.meta"))
// }

tasks.classes {
    finalizedBy(genAtlas)
    dependsOn(generateUniforms)
}

val lwjglVersion = "3.4.1"
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
        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) }                ->
            if (arch.startsWith("aarch64") || arch.startsWith("armv8"))
                "natives-macos-arm64"
            else
                "natives-macos"
        else -> throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
    }
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("-g")
    options.isDebug = true
    options.encoding = "UTF-8"
    options.release = 26
}

java {
    toolchain {
        val minVersion = 26 // минимальные требования
        val preferred = 26  // проверено

        val current = JavaLanguageVersion.current().asInt()

        // Суть в том, чтобы версия java была >=minVersion
        val target = when {
            current >= preferred -> current
            current >= minVersion -> current
            else -> preferred
        }

        languageVersion = JavaLanguageVersion.of(target)
    }
}

configurations["toolsImplementation"].extendsFrom(configurations["implementation"])

dependencies {
    implementation("it.unimi.dsi:fastutil:8.5.18")
    implementation("org.apache.logging.log4j:log4j-api:3.0.0-beta2")
    implementation("org.apache.logging.log4j:log4j-core:3.0.0-beta2")
    implementation("org.apache.logging.log4j:log4j-iostreams:3.0.0-beta2")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.0")
    implementation("org.jetbrains:annotations:26.1.0")

    val asmVersion = "9.10.1"
    "toolsImplementation"("org.ow2.asm:asm:$asmVersion")
    "toolsImplementation"("org.ow2.asm:asm-tree:$asmVersion")
    "toolsImplementation"("org.ow2.asm:asm-analysis:$asmVersion")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-jemalloc")

    implementation("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-jemalloc", classifier = lwjglNatives)

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass  = MAIN_CLASS
    mainModule = MAIN_MODULE
}

jlink {
    mainClass = MAIN_CLASS
    moduleName = MAIN_MODULE
    mergedModuleName = "core.libs.merged"
    mergedModuleJarName = "core-libs-merged"

    enableCds()
    mergedModule {
        version = "1.0.0"
    }
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
        args = applyAppArgs()
        jvmArgs = applyJvmArgs(true)
    }
    jpackage {
        args = applyAppArgs()
        jvmArgs = applyJvmArgs(true)
    }
}

fun applyAppArgs(): List<String> {
    return listOf(
        "--packaged"
    )
}

fun applyJvmArgs(aotCache: Boolean): List<String> {
    val jvmArgs: MutableList<String> = mutableListOf()
    jvmArgs.add("-XX:-OmitStackTraceInFastThrow")
    if (System.getProperty("os.name")!!.startsWith("Darwin") || System.getProperty("os.name")!!.startsWith("Mac OS X")) {
        jvmArgs.add("-XstartOnFirstThread")
    }
    jvmArgs.add("-XX:+UseZGC")
    jvmArgs.add("-XX:+UseCompactObjectHeaders")
    if (aotCache)
        jvmArgs.add("-XX:AOTCacheOutput=app.aot")
    jvmArgs.add("--enable-native-access=org.lwjgl.opengl")
    jvmArgs.add("--enable-native-access=org.lwjgl")
    jvmArgs.add("--enable-native-access=core.main")
    return jvmArgs
}

tasks.jar {
    exclude("sprites.atlas.hash")
    includeEmptyDirs = false
    duplicatesStrategy = DuplicatesStrategy.FAIL
    isReproducibleFileOrder = true

    doFirst {
        @Suppress("UNCHECKED_CAST")
        val json = JsonSlurper().parse(layout.projectDirectory.file("src/assets/sprites.atlas.hash").asFile) as Map<String, String>
        excludes.addAll(json.keys)
    }
}

tasks.run {
    jvmArguments.addAll(applyJvmArgs(false))

    if (System.getProperty("os.name")?.contains("Linux") == true && System.getenv("XDG_SESSION_TYPE") == "wayland") {
        val isBadDriver = providers
            .exec {  commandLine("sh", "-c", "glxinfo | grep 'OpenGL vendor' || echo 'unknown'")  }
            .standardOutput.asText
            .map { it.contains("NVIDIA") }

        if (isBadDriver.get()) {
            environment("__GL_THREADED_OPTIMIZATIONS", "0")
        }
    }

    //для профайлинга
//    jvmArguments.add("-XX:+UnlockDiagnosticVMOptions");
//    jvmArguments.add("-XX:+DebugNonSafepoints")
//    jvmArguments.add("-XX:+PreserveFramePointer")
//    jvmArguments.add("-XX:+ShowHiddenFrames")
//        jvmArguments.add("-XX:TieredStopAtLevel=4")
//    jvmArguments.add("-Xcomp")

    jvmArguments.add("-ea:core.main")
// экспериментируем как бы
//    jvmArguments.add("-XX:+UseShenandoahGC")
//    jvmArguments.add("-XX:ShenandoahGCMode=generational")

    val mainSourceSet = project.sourceSets["main"]
    jvmArgumentProviders.add {
        listOf(
            "--module-path", classpath.asPath,
            "--patch-module", "${mainModule.get()}=${mainSourceSet.output.resourcesDir}"
        )
    }

    classpath = mainSourceSet.runtimeClasspath
}

tasks.jpackageImage {
    dependsOn(tasks.createDelegatingModules)
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed")
    }
}
