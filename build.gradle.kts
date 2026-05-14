plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace 'com.example.opensrc_better_break'

    compileSdk 36

    defaultConfig {
        applicationId "com.example.opensrc_better_break"

        minSdk 26
        targetSdk 36

        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false

            proguardFiles(
                    getDefaultProguardFile('proguard-android-optimize.txt'),
                    'proguard-rules.pro'
            )
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = '11'
    }

    buildFeatures {
        viewBinding true
        compose true
    }

    composeOptions {
        kotlinCompilerExtensionVersion '1.5.14'
    }
}

dependencies {

    // 기본 Android
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'

    // OSMdroid
    implementation 'org.osmdroid:osmdroid-android:6.1.20'

    // Compose
    implementation platform(libs.androidx.compose.bom)
    implementation libs.androidx.lifecycle.runtime.ktx
    implementation libs.androidx.activity.compose
    implementation libs.androidx.compose.ui
    implementation libs.androidx.compose.ui.graphics
    implementation libs.androidx.compose.ui.tooling.preview
    implementation libs.androidx.compose.material3

    // Test
    testImplementation libs.junit
    androidTestImplementation libs.androidx.junit
    androidTestImplementation libs.androidx.espresso.core
    androidTestImplementation platform(libs.androidx.compose.bom)
    androidTestImplementation libs.androidx.compose.ui.test.junit4

    // Debug
    debugImplementation libs.androidx.compose.ui.tooling
    debugImplementation libs.androidx.compose.ui.test.manifest
}
