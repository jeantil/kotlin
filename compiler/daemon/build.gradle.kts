description = "Kotlin Daemon"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val ktorExcludesForDaemon : List<Pair<String, String>> by rootProject.extra

dependencies {
    api(commonDep("org.fusesource.jansi", "jansi"))
    api(commonDep("org.jline", "jline"))

    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:cli-js"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(project(":daemon-common-new"))

    compileOnly(intellijCore())
    compileOnly(intellijDependency("trove4j"))

    runtimeOnly(project(":kotlin-reflect"))

    embedded(project(":daemon-common")) { isTransitive = false }
    api(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) {
        isTransitive = false
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar()

sourcesJar()

javadocJar()

tasks {
    val compileKotlin by existing(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi"
        }
    }
}
