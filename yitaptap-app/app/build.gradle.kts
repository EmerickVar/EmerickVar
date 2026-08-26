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
        versionCode = 2
        versionName = "1.0.1"
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
