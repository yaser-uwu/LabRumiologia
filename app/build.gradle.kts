plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.uteq.software.labrumiologia"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.uteq.software.labrumiologia"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val ragUrl = ragBaseUrl()
        buildConfigField("String", "RAG_BASE_URL", "\"$ragUrl\"")
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
    buildFeatures {
        buildConfig = true
    }
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.tensorflow.lite)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

fun ragBaseUrl(): String {
    val fallback = "http://10.0.2.2:8000/"
    val file = rootProject.file("local.properties")
    if (!file.exists()) {
        return fallback
    }
    var url = fallback
    for (raw in file.readLines()) {
        val line = raw.trim()
        if (line.startsWith("rag.base.url=")) {
            val value = line.substringAfter("=").trim().trim('"')
            if (value.isNotEmpty()) {
                url = if (value.endsWith("/")) value else "$value/"
                break
            }
        }
    }
    return url
}
