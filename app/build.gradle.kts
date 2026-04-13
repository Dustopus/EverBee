import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

val rustCoreDir = file("${rootProject.projectDir}/rust-core")
val jniLibsDir = file("${projectDir}/src/main/jniLibs")

fun getNdkDir(): String {
    val fromLocal = localProperties.getProperty("ndk.dir")
    if (fromLocal != null && File(fromLocal).exists()) return fromLocal
    val sdkDir = localProperties.getProperty("sdk.dir")
        ?: System.getenv("ANDROID_HOME")
        ?: "${System.getProperty("user.home")}/Library/Android/sdk"
    val ndkDir = File("$sdkDir/ndk")
    if (ndkDir.exists()) {
        val versions = ndkDir.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.name }
        if (!versions.isNullOrEmpty()) return versions[0].absolutePath
    }
    return ""
}

fun buildRustForTarget(target: String, archDir: String) {
    val ndkDir = getNdkDir()
    if (ndkDir.isEmpty()) {
        throw GradleException("NDK not found. Install NDK via Android Studio SDK Manager or set ndk.dir in local.properties")
    }
    val toolchain = file("$ndkDir/toolchains/llvm/prebuilt/darwin-x86_64")
    val apiLevel = 26

    exec {
        workingDir = rustCoreDir
        environment("CC_${target.replace('-', '_')}", "$toolchain/bin/$target$apiLevel-clang")
        environment("AR_${target.replace('-', '_')}", "$toolchain/bin/llvm-ar")
        environment("CARGO_TARGET_${target.replace('-', '_').uppercase()}_LINKER", "$toolchain/bin/$target$apiLevel-clang")
        executable("cargo")
        args("build", "--target", target, "--release")
    }

    val outputDir = file("$jniLibsDir/$archDir")
    outputDir.mkdirs()
    val soFile = file("$rustCoreDir/target/$target/release/libapislens_core.so")
    if (soFile.exists()) {
        soFile.copyTo(File(outputDir, "libapislens_core.so"), overwrite = true)
    }
}

tasks.register("buildRustRelease") {
    description = "Build Rust core library for Android"
    group = "rust"

    doLast {
        val cargoExists = try {
            ProcessBuilder("cargo", "--version").start().waitFor() == 0
        } catch (_: Exception) { false }

        if (!cargoExists) {
            logger.lifecycle("Cargo not found, skipping Rust build (Kotlin fallback will be used)")
            return@doLast
        }

        try {
            buildRustForTarget("aarch64-linux-android", "arm64-v8a")
            logger.lifecycle("Rust core built successfully for arm64-v8a")
        } catch (e: Exception) {
            logger.warn("Rust build failed for arm64-v8a: ${e.message}")
            logger.warn("Kotlin fallback will be used at runtime")
        }
    }
}

tasks.register("buildRustAllArch") {
    description = "Build Rust core library for all Android architectures"
    group = "rust"

    doLast {
        val targets = mapOf(
            "aarch64-linux-android" to "arm64-v8a",
            "armv7-linux-androideabi" to "armeabi-v7a",
            "x86_64-linux-android" to "x86_64",
            "i686-linux-android" to "x86"
        )

        targets.forEach { (target, archDir) ->
            try {
                buildRustForTarget(target, archDir)
                logger.lifecycle("Rust core built successfully for $archDir")
            } catch (e: Exception) {
                logger.warn("Rust build failed for $archDir: ${e.message}")
            }
        }
    }
}

android {
    namespace = "com.apislens"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.apislens"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }.configureEach {
    val buildRustTask = tasks.findByName("buildRustRelease")
    if (buildRustTask != null) {
        dependsOn(buildRustTask)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
