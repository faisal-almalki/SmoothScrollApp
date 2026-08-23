plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.densitech.scrollsmooth"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.densitech.scrollsmooth"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 10.0.2.2 is the host machine as seen from the Android emulator, so a
        // debug build talks to a server running on the developer's laptop.
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8787\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = project.findProperty("MYAPP_UPLOAD_STORE_FILE").toString()
            val storePassword = project.findProperty("MYAPP_UPLOAD_STORE_PASSWORD").toString()
            val keyAlias = project.findProperty("MYAPP_UPLOAD_KEY_ALIAS").toString()
            val keyPassword = project.findProperty("MYAPP_UPLOAD_KEY_PASSWORD").toString()

            storeFile = file(storeFilePath)
            this.storePassword = storePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
        }
    }


    buildTypes {
        getByName("release") {
            // Point this at the deployed API before shipping.
            buildConfigField("String", "API_BASE_URL", "\"https://api.smoothscroll.app\"")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig =
                signingConfigs.getByName("release")  // Link signingConfig to release build type
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        allWarningsAsErrors = false
        freeCompilerArgs = listOf(
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
            "-opt-in=androidx.compose.runtime.ExperimentalComposeApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi",
            "-opt-in=com.google.accompanist.pager.ExperimentalPagerApi",
            "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
            "-opt-in=kotlin.ExperimentalUnsignedTypes",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.InternalCoroutinesApi"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Exo player
    implementation(libs.bundles.media3.bundles)

    // Navigation and viewModel() were both reaching this project only through
    // hilt-navigation-compose, which was never declared for either of them.
    // Removing Hilt took Navigation Compose with it and broke the build — so
    // both are now direct dependencies, which is what they always were in
    // practice.
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization)
    implementation(libs.coil)
    implementation(libs.permission)
    implementation(libs.okhttp)

    // Push notifications.
    //
    // The google-services Gradle plugin is deliberately NOT applied, and there
    // is no google-services.json in the repository: that file is per-project
    // configuration belonging to whoever ships the app, and committing a
    // placeholder would only produce an APK that looks configured and is not.
    //
    // Without it Firebase logs "Default FirebaseApp failed to initialize" once
    // at startup and every push call becomes a no-op — the app runs normally,
    // notifications simply do not arrive. See README for the two lines that
    // turn it on.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}