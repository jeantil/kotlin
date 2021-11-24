plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":core:descriptors"))
    api(project(":compiler:resolution.common"))
    compileOnly(intellijDependency("trove4j"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
