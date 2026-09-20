plugins {
    id("com.android.application")
}

android {
    namespace = "com.yagay.NotifyLens"
    compileSdk {
        version = release(37) { minorApiLevel = 0 }
    }

    defaultConfig {
        applicationId = "com.yagay.NotifyLens"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging.resources.merges += "META-INF/xposed/*"

    sourceSets {
        getByName("main") { resources.srcDirs("src/main/resources") }
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
}

dependencies {
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime:2.9.2")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("net.zetetic:sqlcipher-android:4.19.0")
    annotationProcessor("androidx.room:room-compiler:2.7.2")
    compileOnly("io.github.libxposed:api:102.0.0")
}
