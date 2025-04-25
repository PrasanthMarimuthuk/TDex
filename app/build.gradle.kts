plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.tdexv01"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tdexv01"
        minSdk = 23
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true // Required for Robolectric
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt-android:3.1.2")
    implementation("io.ktor:ktor-client-apache5:3.1.1") {
        exclude(group = "org.apache.httpcomponents.core5", module = "httpcore5")
        exclude(group = "org.apache.httpcomponents.core5", module = "httpcore5-h2")
    }
    implementation(libs.google.firebase.analytics)
    implementation(libs.cloudinary.cloudinary.android)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.play.services.base)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.viewpager2)
    implementation(libs.glide)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.material)
    implementation(libs.androidx.gridlayout)
    implementation(libs.play.services.places)
    implementation(libs.mlkit.vision.common)
    implementation(libs.mlkit.image.labeling)
    implementation(libs.arcore)
    implementation(libs.activity.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage)
    implementation(libs.googleid)
    implementation(libs.androidx.rules)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.androidx.cardview)
    implementation(libs.circleindicator)
    implementation(libs.recyclerview)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.gson)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.mockito.android)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.kotlinx.coroutines.test) // For testing coroutines

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.mockito.mockito.android)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}