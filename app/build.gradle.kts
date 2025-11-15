plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {

    ndkVersion = "27.0.12077973"
    namespace = "com.example.edgeviewer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.edgeviewer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        // -----------------------------------------------------
        // FIX: The only valid Kotlin DSL way to pass CMake args
        // -----------------------------------------------------
        externalNativeBuild {
            cmake {
                arguments.add("-DOpenCV_DIR=D:/OpenCV-android-sdk/sdk/native/jni")
            }
        }
    }

    // -----------------------------------------------------
    // This block MUST NOT contain arguments — only path + version
    // -----------------------------------------------------
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        jniLibs.useLegacyPackaging = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
