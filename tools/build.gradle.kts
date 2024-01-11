tasks.register<JavaExec>("genatlas") {
    classpath = sourceSets.main.get().runtimeClasspath
    workingDir = rootDir
    mainClass.set("core.tool.AtlasGenerator")
}

dependencies {
    implementation(rootProject)
}
