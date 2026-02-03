plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "hr.jkacan.setmaker"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "hr.jkacan.setmaker"
        minSdk = 27
        targetSdk = 36
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.7")

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // CardView
    implementation("androidx.cardview:cardview:1.0.0")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Image Loading (Optional - uncomment if needed)
    // implementation("com.github.bumptech.glide:glide:4.16.0")
    // implementation("com.squareup.picasso:picasso:2.8")
}