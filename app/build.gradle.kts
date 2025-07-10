import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.FileInputStream
import java.util.Properties
import java.util.stream.StreamSupport

buildscript {
    dependencies {
        classpath(libs.org.eclipse.jgit)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

// 获取当前的 buildType 并将其动态添加到版本号
fun getBuildType(): String {
    return kotlin.runCatching {
        project.gradle.startParameter.taskNames.find { it.contains("assemble", ignoreCase = true) }
        ?.split("assemble")
        ?.getOrNull(1)
        ?.lowercase()
    }.getOrNull() ?: "unknown"
}

fun getGitBranchName(): String {
    return kotlin.runCatching {
        val gitDir = project.rootDir.resolve(".git")
        val repository = FileRepositoryBuilder.create(gitDir)
        repository.use { repo ->
            val fullBranch = repo.fullBranch // This gets the full branch name or commit hash if detached
            if (fullBranch.startsWith("refs/heads/")) {
                // If it's a branch, return the branch name
                fullBranch.removePrefix("refs/heads/")
            } else {
                // Otherwise, return a detached HEAD identifier
                "detached-${fullBranch.substring(0, 7)}"
            }
        }
    }.getOrNull() ?: "nogit"
}

fun getGitCommitCount(): Int {
    return kotlin.runCatching {
        val gitDir = project.rootDir.resolve(".git")
        val repository = FileRepositoryBuilder.create(gitDir)
        repository.use { repo ->
            val head = repo.resolve("HEAD")
            StreamSupport.stream(Git(repo).log().add(head).call().spliterator(), false).count().toInt()
        }
    }.getOrNull() ?: 1000
}

fun getGitHash(): String {
    return kotlin.runCatching {
        val gitDir = project.rootDir.resolve(".git")
        val repository = FileRepositoryBuilder.create(gitDir)
        repository.use { repo ->
            val head = repo.resolve("HEAD")
            head?.abbreviate(8)?.name()
        }
    }.getOrNull() ?: "nogit"
}

android {
    namespace = "rj.browser"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "rj.browser"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = getGitCommitCount()
        versionName = "2.4.0-${getGitBranchName()}:${getBuildType()}+${getGitHash()}"
        multiDexEnabled = true
 
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
        // testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("config") {
            storeFile = project.rootProject.file("sign.jks")
            storePassword = "android"
            keyPassword = "android"
            keyAlias = "key"
        }
    }

    sourceSets {
        create("clone") {
            assets.srcDirs("src/clone/assets")
        }
        getByName("main") {
            jniLibs.srcDirs("jniLibs")
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("config")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("config")
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("clone") {
            initWith(getByName("release")) // 从 'debug' 构建类型继承所有配置
            applicationIdSuffix = ".pro"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
    }
    
    buildFeatures {
        viewBinding = true
    }

    kotlin {
        compileOptions {
            jvmToolchain(libs.versions.javaVersion.get().toInt())
        }
    }
}

configurations.all {
    resolutionStrategy {
        exclude(group = "com.google.android.material", module = "material")
    }
}

dependencies {
    implementation(fileTree(Pair("dir", "libs"), Pair("include", listOf("*.aar"))))
    implementation(fileTree(Pair("dir", "libs"), Pair("include", listOf("*.jar"))))
    implementation(libs.material.compat)
    implementation(libs.okhttp.compat)
    implementation(libs.materialpreferences)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.multidex)

    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded) {
        isTransitive = false
    }

    implementation(libs.hikage.core)
    implementation(libs.hikage.recyclerview)
    implementation(libs.hikage.extension)
    implementation(libs.hikage.extension.betterandroid)
    ksp(libs.hikage.compiler)
    implementation(libs.hikage.widget.androidx)
    implementation(libs.hikage.widget.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso.core)
}
