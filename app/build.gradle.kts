plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.expressive.oneuicalc"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.expressive.oneuicalc"
        minSdk = 31
        targetSdk = 36
        versionCode = 100
        versionName = "1.0.0"
    }

    flavorDimensions += "xposedApi"
    productFlavors {
        create("modern") {
            dimension = "xposedApi"
            versionNameSuffix = "-vector-api102"
        }
        create("legacy") {
            dimension = "xposedApi"
            versionNameSuffix = "-legacy-api82"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    "modernCompileOnly"("io.github.libxposed:api:102.0.0")
    "modernImplementation"("io.github.libxposed:service:101.0.0")

    "legacyCompileOnly"("de.robv.android.xposed:api:82")
}
