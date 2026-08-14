plugins {
    id("base")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.kover)
}

val plantumlConfig: Configuration = configurations.create("plantumlConfig")

dependencies {
    plantumlConfig(libs.plantuml)
}

tasks.register<JavaExec>("generateDiagrams") {
    group = "documentation"
    description = "Generates SVG diagrams from PlantUML source files."
    classpath = plantumlConfig
    mainClass.set("net.sourceforge.plantuml.Run")

    args(
        "-tsvg",
        "-o", "${projectDir}/docs/architecture/out",
        "${projectDir}/docs/architecture/src/**.puml"
    )
}

// Automatically generate diagrams during build
tasks.named("assemble") {
    dependsOn("generateDiagrams")
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
                    "android/**/*.*", "**/databinding/*", "**/androidx/*",
                    "**/*MapperImpl*.*", "**/*Component*.*", "**/*BR*.*",
                    "**/*Module*.*", "**/*Dagger*.*", "**/*Hilt*.*",
                    "**/*_Factory*.*", "**/*_Provide*Factory*.*"
                )
            }
        }
    }
}

sonar {
    properties {
        property("sonar.host.url", System.getenv("SONAR_HOST_URL") ?: "http://localhost:9000")
        property("sonar.token", System.getenv("SONAR_TOKEN") ?: "")
        property("sonar.projectKey", "monatsblitz-android")
        property("sonar.projectName", "monatsblitz-android")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.android.lint.reportPaths", "app/build/reports/lint-results-debug.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "${layout.buildDirectory.get().asFile}/reports/kover/report.xml")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")

    pluginManager.withPlugin("org.sonarqube") {
        extensions.configure<org.sonarqube.gradle.SonarExtension> {
            properties {
                val lintReport = layout.buildDirectory
                    .file("reports/lint-results-debug.xml")
                    .get()
                    .asFile

                property(
                    "sonar.androidLint.reportPaths",
                    lintReport.absolutePath
                )
            }
        }
    }

    // Konfiguration für Android-App-Module
    pluginManager.withPlugin("com.android.application") {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension>("android") {
            lint {
                abortOnError = false
                checkReleaseBuilds = false
            }
        }
    }

    // Konfiguration für Android-Library-Module
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension>("android") {
            lint {
                abortOnError = false
                checkReleaseBuilds = false
            }
        }
    }
}