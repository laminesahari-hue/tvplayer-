plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.tvplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tvplayer"
        minSdk = 21
        targetSdk = 34
        versionCode = 12
        versionName = "7.4.0"
    }

    signingConfigs {
        create("fixed") {
            storeFile = file("tv.keystore")
            storePassword = "tvplayer"
            keyAlias = "tv"
            keyPassword = "tvplayer"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("fixed")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
}
