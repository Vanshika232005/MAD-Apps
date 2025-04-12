plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.cam_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cam_app"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.recyclerview) // For RecyclerView
    implementation(libs.github.glide) // For image loading
    annotationProcessor(libs.github.compiler) // Glide annotation processor
    implementation(libs.androidx.activity.ktx) // For registerForActivityResult
    implementation ("com.google.android.material:material:1.11.0")
    implementation(libs.androidx.documentfile) // For SAF DocumentFile access
    implementation(libs.androidx.exifinterface) // For reading image metadata
}