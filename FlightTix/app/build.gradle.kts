plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.identusbook.flighttix"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.identusbook.flighttix"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // The Identus SDK targets JVM 17.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        // Matches Kotlin 1.9.25.
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
            pickFirsts += "google/protobuf/*.proto"
            pickFirsts += "org/bouncycastle/**"
        }
    }

    // The Identus SDK pulls BouncyCastle via `bcprov-jdk15to18:1.69`, while `didcommx`
    // pulls the older `bcprov-jdk15on:1.68` — they collide on classes/resources. Keep the
    // newer jdk15to18 variant.
    configurations.configureEach {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        // `tink` (JVM) collides with `tink-android` (from security-crypto); keep tink-android.
        exclude(group = "com.google.crypto.tink", module = "tink")
    }
}

dependencies {
    // --- Hyperledger Identus Edge Agent SDK (Kotlin Multiplatform, Android target) ---
    implementation("org.hyperledger.identus:edge-agent-sdk-android:4.0.0")

    // --- Jetpack Compose ---
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // --- Secure storage (Keychain equivalent) ---
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // --- Cloud Agent REST client ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Ktor — the Identus SDK's httpClient() returns an io.ktor.client.HttpClient, so the
    // type must be on our compile classpath (version pinned to the SDK's 2.3.11).
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation("io.ktor:ktor-client-okhttp:2.3.11")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
