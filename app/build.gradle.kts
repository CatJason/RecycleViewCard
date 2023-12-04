plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "cat.jason.composecardactivity"
    compileSdk = 33

    defaultConfig {
        applicationId = "cat.jason.composecardactivity"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                // 这里可以设置 CMake 相关的配置
                arguments("-DANDROID_STL=c++_shared", "-DANDROID_CPP_FEATURES=exceptions rtti", "-DCMAKE_CXX_STANDARD=17")
            }
        }

        // 指定 NDK 版本
        ndkVersion = "26.1.10909125"
    }

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

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.2.0-alpha08" // 使用适当的版本号
    }
}

dependencies {
    implementation ("com.github.bumptech.glide:glide:4.12.0")// 请替换为最新版本
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0") // 如果需要用到注解处理器
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")
    implementation("androidx.activity:activity-compose:1.3.0")
    val composeVersion = "1.0.0-beta01" // 使用适当的版本号
    implementation("androidx.compose.material3:material3:$composeVersion")
    implementation("androidx.compose.ui:ui:1.2.0")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}