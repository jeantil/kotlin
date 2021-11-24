import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.project

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val Project.intellijVersion
    get() = rootProject.extra["versions.intellijSdk"]

fun Project.intellijCore() = dependencies.project(":dependencies:intellij-core")

fun Project.intellijDependency(module: String): String {
    val organisation = when (module) {
        "guava" -> "com.google.guava"
        "streamex" -> "one.util"
        "jna", "jna-platform" -> "net.java.dev.jna"
        "lz4-java" -> "org.lz4"
        "oro" -> "oro"
        "commons-lang" -> "commons-lang"
        "gson" -> "com.google.code.gson"
        "groovy", "groovy-xml" -> "org.codehaus.groovy"
        "serviceMessages" -> "org.jetbrains.teamcity"
        "intellij-deps-fastutil" -> "org.jetbrains.intellij.deps.fastutil"
        else -> "org.jetbrains.intellij.deps"
    }

    if (!rootProject.extra.has("versions.$module"))
        error("$module version is missing in versions.properties")

    val version = rootProject.extra.get("versions.$module")
    return "$organisation:$module:$version"
}

fun Project.jpsModel() = "com.jetbrains.intellij.platform:jps-model:$intellijVersion"
fun Project.jpsModelSerialization() = "com.jetbrains.intellij.platform:jps-model-serialization:$intellijVersion"
fun Project.jpsModelImpl() = "com.jetbrains.intellij.platform:jps-model-impl:$intellijVersion"
fun Project.jpsBuildTest() = "com.jetbrains.intellij.idea:jps-build-test:$intellijVersion"
fun Project.intellijPlatformUtil() = "com.jetbrains.intellij.platform:util:$intellijVersion"
fun Project.intellijJavaRt() = "com.jetbrains.intellij.java:java-rt:$intellijVersion"

/**
 * Runtime version of annotations that are already in Kotlin stdlib (historically Kotlin has older version of this one).
 *
 * SHOULD NOT BE USED IN COMPILE CLASSPATH!
 *
 * `@NonNull`, `@Nullabe` from `idea/annotations.jar` has `TYPE` target which leads to different types treatment in Kotlin compiler.
 * On the other hand, `idea/annotations.jar` contains org/jetbrains/annotations/Async annations which is required for IDEA debugger.
 *
 * So, we are excluding `annotaions.jar` from all other `kotlin.build` and using this one for runtime only
 * to avoid accidentally including `annotations.jar` by calling `intellijDep()`.
 */
fun Project.intellijRuntimeAnnotations() = "org.jetbrains:annotations:${rootProject.extra["versions.annotations"]}"
