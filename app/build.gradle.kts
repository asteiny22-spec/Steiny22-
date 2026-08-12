plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ariabridge.lite"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ariabridge.lite"
        minSdk = 28
        targetSdk = 36
        versionCode = 4
        versionName = "0.1.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":libadb"))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.MuntashirAkon:sun-security-android:1.1")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")

    // Force libadb onto the bundled Conscrypt provider instead of Android 16's
    // platform-private Conscrypt API, whose exportKeyingMaterial signature changed.
    implementation("org.conscrypt:conscrypt-android:2.5.2")
}
