plugins {
    id("com.android.application")
}

android {
    namespace = "org.newagecoding.yitaptap"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.newagecoding.yitaptap"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
