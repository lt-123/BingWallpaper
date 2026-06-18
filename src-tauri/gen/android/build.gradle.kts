import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.11.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    // Android/Tauri 生成代码和上游 Tauri Android 库仍会触发平台弃用告警。
    // 本项目把这些告警集中压制，避免正常验证输出被非业务代码噪声淹没。
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-Xlint:-options")
    }
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            suppressWarnings = true
        }
    }
}

tasks.register("clean").configure {
    delete("build")
}
