import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.smartlifestyle.companion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartlifestyle.companion"
        minSdk = 26          // targets recent enough API for modern sensor batching
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "WEATHER_API_KEY",
            "\"${localProperties.getProperty("WEATHER_API_KEY", "")}\""
        )

        // OPTIONAL. Leave blank to use the built-in on-device rule-based AI Coach.
        // Set to a real key (e.g. an OpenAI key) to route AI Coach chat through a
        // real hosted LLM instead - see AiCoachRepository.
        buildConfigField(
            "String",
            "AI_API_KEY",
            "\"${localProperties.getProperty("AI_API_KEY", "")}\""
        )
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Coroutines <-> Firebase Task interop (enables .await() on Firestore calls)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Room (local sensor-data buffer - see HealthRepository for why this stays
    // Room-based while routine data goes straight to Firestore)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Firebase (Auth, Firestore, Cloud Messaging) via BoM
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Local sensors: step counter is read via android.hardware.SensorManager
    // (no extra dependency needed - it's part of the Android framework).

    // Health Connect - real wearable data (Redmi Watch 3 Active via Mi Fitness ->
    // Google Fit -> Health Connect), used for heart rate with a simulated fallback.
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")

    // Background work (smart, context-triggered notifications)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Networking (weather API)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Location (for weather + context awareness)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Security (encrypted local storage for sensitive data)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
