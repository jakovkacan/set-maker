import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties()
localProperties.load(project.rootProject.file("local.properties").inputStream())

android {
    namespace = "hr.jkacan.setmaker"
    compileSdk = 36

    defaultConfig {
        applicationId = "hr.jkacan.setmaker"
        minSdk = 27
        targetSdk = 35
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
        debug {
            buildConfigField("String", "SPOTIFY_CLIENT_ID", localProperties.getProperty("SPOTIFY_CLIENT_ID"))
            buildConfigField("String", "SPOTIFY_CLIENT_SECRET", localProperties.getProperty("SPOTIFY_CLIENT_SECRET"))
            buildConfigField("String", "SOUNDCLOUD_CLIENT_ID", localProperties.getProperty("SOUNDCLOUD_CLIENT_ID"))
            buildConfigField("String", "SOUNDCLOUD_CLIENT_SECRET", localProperties.getProperty("SOUNDCLOUD_CLIENT_SECRET"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ViewPager2
    implementation(libs.androidx.viewpager2)

    // RecyclerView
    implementation(libs.androidx.recyclerview)

    // CardView
    implementation(libs.androidx.cardview)

    // Preferences
    implementation(libs.androidx.preference.ktx)

    // Image Loading
    implementation(libs.coil)
    implementation(libs.coil.compose)

    // Retrofit for networking
    implementation(libs.retrofit)
    // GSON converter for Retrofit to handle JSON
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.firebase.crashlytics.buildtools)

    // JSON Path for parsing JSON for fetching Spotify preview URL
    implementation(libs.json.path)
    implementation(libs.jsoup)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.ui.geometry)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
}
