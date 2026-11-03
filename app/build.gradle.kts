import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.navigation.safeargs)
}

// As senhas da assinatura vivem no local.properties (fora do git),
// nunca dentro do build script.
val segredos = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
android {
    namespace = "com.algarit.taskflow"
    compileSdk {
        version = release(37)
    }
    buildFeatures {
        viewBinding = true
    }
    defaultConfig {
        applicationId = "com.algarit.taskflow"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            storeFile = file("../taskflow.jks")
            storePassword = segredos.getProperty("TF_STORE_PASSWORD") ?: ""
            keyAlias = segredos.getProperty("TF_KEY_ALIAS") ?: "taskflow"
            keyPassword = segredos.getProperty("TF_KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.viewmodel.ktx)
    testImplementation(libs.org.json)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
