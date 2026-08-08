plugins {
    alias(libs.plugins.android.application) apply false

    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.kover)
}

sonarqube {
    properties {
        property("sonar.host.url", System.getenv("SONAR_HOST_URL") ?: "http://localhost:9000")
        property("sonar.token", System.getenv("SONAR_TOKEN") ?: "")
        property("sonar.projectKey", "monatsblitz-android")
        property("sonar.projectName", "Monatsblitz Android")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.android.lint.reportPaths", "app/build/reports/lint-results-debug.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "${layout.buildDirectory.get().asFile}/reports/kover/report.xml")
    }
}

dependencies {
    kover(project(":app"))
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
