plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // --- ESTA ES LA SOLUCIÓN ---
    // Forzamos al sistema a buscar los recursos en cualquier subcarpeta de res
    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res")
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
    }
}

dependencies {
    // 1. GUAVA
    implementation("com.google.guava:guava:31.1-android")
    modules {
        module("com.google.guava:listenablefuture") {
            replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }

    // 2. CORE Y UI
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // ✅ AÑADIDO: FolderOpen, AccessTime, Storage, Dashboard, Map, etc.
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // 3. HILT
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // 4. ROOM
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // 5. PARSERS DE DOCUMENTOS
    // ✅ REEMPLAZADO itext7 por pdfbox-android (el código usa imports tom_roush)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0") {
        exclude(group = "com.google.guava")
    }
    // ✅ AÑADIDO poi-scratchpad: necesario para hwpf (.doc) y hslf (.ppt)
    implementation("org.apache.poi:poi:5.2.3") {
        exclude(group = "com.google.guava")
    }
    implementation("org.apache.poi:poi-ooxml:5.2.3") {
        exclude(group = "com.google.guava")
    }
    implementation("org.apache.poi:poi-scratchpad:5.2.3") {
        exclude(group = "com.google.guava")
    }
    // ✅ REEMPLAZADO tess-two por tesseract4android (el código usa cz.adaptech.TessBaseAPI)
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.7.0")

    // 6. RED
    // ✅ OkHttp explícito (toRequestBody, toMediaType, response.body)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // ✅ CORREGIDO: ktor-SERVER → ktor-CLIENT (server no tiene sentido en Android)
    // ✅ AÑADIDO: content-negotiation y gson para NetworkService
    val ktorVersion = "2.3.7"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
}
