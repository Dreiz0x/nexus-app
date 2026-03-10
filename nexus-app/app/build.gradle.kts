plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    // Corregimos la referencia a KSP para que coincida con la raíz
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.nexus.intelligence"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nexus.intelligence"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // 1. SOLUCIÓN A CLASES DUPLICADAS (Forzamos Guava Android)
    implementation("com.google.guava:guava:31.1-android")
    
    modules {
        module("com.google.guava:listenablefuture") {
            replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }

    // 2. CORE Y UI (Jetpack Compose)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // 3. INYECCIÓN DE DEPENDENCIAS (Hilt)
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // 4. BASE DE DATOS (Room)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    
    // 5. PARSERS DE DOCUMENTOS (Apache POI, iText, Tesseract)
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.3") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
    implementation("com.rmtheis:tess-two:9.1.0")
    
    // 6. RED Y API (Retrofit)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
