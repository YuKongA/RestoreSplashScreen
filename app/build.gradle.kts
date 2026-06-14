@file:Suppress("UnstableApiUsage")

import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

val projectProperties = Properties().apply {
    file("gradle.properties").inputStream().use { load(it) }
}

fun projectProperty(key: String): String {
    val raw = projectProperties.getProperty(key)
        ?: throw GradleException("Missing property '$key' in app/gradle.properties")
    return Regex("""\$\{([^}]+)}""")
        .replace(raw.trim()) { projectProperty(it.groupValues[1].trim()) }
        .trim()
        .removeSurrounding("\"")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(key: String): String =
    System.getenv(key) ?: localProperties.getProperty(key) ?: ""

android {
    namespace = projectProperty("project.namespace")
    compileSdk = projectProperty("project.compileSdk").toInt()

    defaultConfig {
        applicationId = projectProperty("project.applicationId")
        minSdk = projectProperty("project.minSdk").toInt()
        targetSdk = projectProperty("project.targetSdk").toInt()
        versionCode = projectProperty("project.versionCode").toInt()
        versionName = projectProperty("project.versionName")
    }

    packaging.resources {
        excludes += "**"
        merges += "META-INF/yukihookapi_init"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    val keystorePath = secret("KEYSTORE_PATH")
    val keystorePass = secret("KEYSTORE_PASS")
    val signingKeyAlias = secret("KEY_ALIAS")
    val signingKeyPassword = secret("KEY_PASSWORD")
    val isKeyStoreAvailable = keystorePath.isNotBlank() && keystorePass.isNotBlank() &&
            signingKeyAlias.isNotBlank() && signingKeyPassword.isNotBlank()
    if (isKeyStoreAvailable) {
        signingConfigs {
            create("universal") {
                storeFile = file(keystorePath)
                storePassword = keystorePass
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        all { if (isKeyStoreAvailable) signingConfig = signingConfigs.getByName("universal") }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "tier"
    productFlavors {
        create("CI") {
            dimension = "tier"
            versionCode = defaultConfig.versionCode?.plus(1)
            versionName = "${defaultConfig.versionName?.split(" - ")?.get(0)}-CI.${getGitHeadRefsSuffix(rootProject)}"
        }
        create("app") {
            dimension = "tier"
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("21")
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)?.outputFileName?.set(
                output.versionName.map { versionName ->
                    "RestoreSplashScreen_${versionName}${if (variant.buildType == "debug") "_debug" else ""}.apk"
                }
            )
        }
    }
}

dependencies {
    implementation(projects.hyperxCompose)

    compileOnly(libs.xposed.api)
    implementation(libs.yukihookapi.api)
    ksp(libs.yukihookapi.ksp.xposed)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}

tasks.register("getVersionCode") {
    description = "getVersionCode"
    println("${projectProperty("project.versionCode")}-${projectProperty("project.versionName")}")
}

fun getGitHeadRefsSuffix(project: Project): String {
    // .git/HEAD描述当前目录所指向的分支信息，内容示例："ref: refs/heads/master\n"
    val headFile = File(project.rootProject.projectDir, ".git" + File.separator + "HEAD")
    if (headFile.exists()) {
        val string: String = headFile.readText(Charsets.UTF_8)
        val string1 = string.replace(Regex("""ref:|\s"""), "")
        val result = if (string1.isNotBlank() && string1.contains('/')) {
            val refFilePath = ".git" + File.separator + string1
            // 根据HEAD读取当前指向的hash值，路径示例为：".git/refs/heads/master"
            val refFile = File(project.rootProject.projectDir, refFilePath)
            // 索引文件内容为hash值+"\n"，
            // 示例："90312cd9157587d11779ed7be776e3220050b308\n"
            refFile.readText(Charsets.UTF_8).replace(Regex("""\s"""), "").subSequence(0, 7)
        } else {
            string.take(7)
        }
        println("commit_id: $result")
        return result.toString()
    } else {
        println("WARN: .git/HEAD does NOT exist")
        return ""
    }
}
