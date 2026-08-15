import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
    id("jacoco")
}

configure<JacocoPluginExtension> {
    toolVersion = "0.8.12"
}

android {
    namespace = "de.kindermaenner.monatsblitz"
    compileSdk = 37

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
    }
    val apiKey = localProperties.getProperty("API_KEY_MONATSBLITZ", "DEBUG_KEY")

    defaultConfig {
        applicationId = "de.kindermaenner.monatsblitz"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_KEY", "\"$apiKey\"")
    }

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        val appName = project.name
        val versionName = android.defaultConfig.versionName

        variant.outputs.forEach { output ->
            output.outputFileName.set(
                "monatsblitz-${variant.name}-v${versionName}.apk"
            )
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)
    androidTestImplementation(libs.androidx.navigation.testing)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.retrofit.adapter.java8)
    implementation(libs.okhttp)

    implementation(libs.kotlinx.serialization.json)
    
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.agent)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    jvmArgs(
        "-XX:+EnableDynamicAgentLoading",
        "-Xshare:off"
    )
}

tasks.register<JacocoReport>("connectedDebugAndroidTestCoverageReport") {
    dependsOn("connectedDebugAndroidTest")
    group = "Reporting"
    description = "Generate Jacoco coverage reports for instrumented tests."

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val javaClasses = fileTree(project.layout.buildDirectory.asFile) {
        include("**/intermediates/javac/debug/**/classes/**")
    }
    val kotlinClasses = fileTree(project.layout.buildDirectory.asFile) {
        include("**/intermediates/kotlin_classes/debug/**")
        include("**/classes/kotlin/debug/**")
    }
    classDirectories.setFrom(files(javaClasses, kotlinClasses))

    sourceDirectories.setFrom(files("$projectDir/src/main/java", "$projectDir/src/main/kotlin"))

    executionData.setFrom(fileTree(project.layout.buildDirectory.asFile) {
        include("**/outputs/code_coverage/**/*.ec")
        include("**/outputs/connected_android_test_additional_output/**/*.ec")
    })
}
