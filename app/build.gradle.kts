import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Własny klucz podpisu (przypominajka.jks + keystore.properties w głównym folderze, poza repozytorium).
// Aktualizacje z GitHuba zainstalują się tylko, gdy każde wydanie jest podpisane tym samym kluczem.
val keystoreFile = rootProject.file("keystore.properties")
val keystore = Properties().apply { if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) } }

android {
    namespace = "pl.przypominajka"
    compileSdk = 36

    defaultConfig {
        applicationId = "pl.przypominajka"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "4.15"
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("przypominajka") {
                storeFile = rootProject.file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: usuwa nieużywany kod i optymalizuje – Compose działa wtedy dużo płynniej niż w debug.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Bez pliku klucza (np. świeży klon z GitHuba) podpisujemy kluczem debug, żeby dało się zbudować.
            signingConfig = signingConfigs.findByName("przypominajka") ?: signingConfigs.getByName("debug")
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

    packaging {
        resources {
            // Pliki licencji z bibliotek Javy (SDK Anthropic) – powtarzają się i nie są potrzebne w APK.
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/INDEX.LIST",
                "META-INF/*.md",
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    // Profile startowe bibliotek (Compose) instalowane także przy instalacji z pliku APK – szybszy start i płynność.
    implementation(libs.androidx.profileinstaller)

    // Widżet "Dziś" na ekran główny telefonu.
    implementation(libs.androidx.glance.appwidget)

    // Przeciąganie punktów checklisty (zmiana kolejności w LazyColumn).
    implementation(libs.reorderable)

    // Quizy generowane przez AI (Claude) – oficjalne SDK Anthropic.
    implementation(libs.anthropic.java)
}
