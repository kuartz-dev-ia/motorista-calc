plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.motorista.calc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.motorista.calc"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        viewBinding = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // OCR (ML Kit) removido por enquanto — não é usado na versão atual do parser,
    // que lê o texto direto da árvore de acessibilidade. Se no futuro precisarmos
    // ler texto de PRINTS (imagens), adicionamos de volta:
    // implementation("com.google.mlkit:text-recognition:16.0.1")
}
