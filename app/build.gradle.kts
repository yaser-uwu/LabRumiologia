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

        buildConfigField("String", "GEMINI_API_KEY", "\"${escapeJava(geminiValue("GEMINI_API_KEY", "gemini.api.key"))}\"")
        buildConfigField("String", "GEMINI_MODEL", "\"${escapeJava(geminiValue("LLM_MODEL", "gemini.model").ifEmpty { "gemini-3.1-flash-lite" })}\"")
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

val syncLabDocs = tasks.register<Copy>("syncLabDocs") {
    group = "lab"
    description = "Copia las guías del laboratorio a assets (el chat las usa en el teléfono)"
    from(rootProject.file("backend/data/docs")) {
        include("**/*.md", "**/*.txt")
    }
    into(file("src/main/assets/docs"))
}

afterEvaluate {
    tasks.named("preBuild").configure { dependsOn(syncLabDocs) }
}

fun escapeJava(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

fun geminiValue(envKey: String, localKey: String): String {
    val envFile = rootProject.file("backend/.env")
    if (envFile.exists()) {
        for (raw in envFile.readLines()) {
            val line = raw.trim()
            if (line.startsWith("$envKey=")) {
                val v = line.substringAfter("=").trim().trim('"')
                if (v.isNotEmpty() && !v.startsWith("your_")) return v
            }
        }
    }
    val local = rootProject.file("local.properties")
    if (local.exists()) {
        for (raw in local.readLines()) {
            val line = raw.trim()
            if (line.startsWith("$localKey=")) {
                val v = line.substringAfter("=").trim().trim('"')
                if (v.isNotEmpty()) return v
            }
        }
    }
    return ""
}
