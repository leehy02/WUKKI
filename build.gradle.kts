plugins {
    kotlin("android") version "1.9.10" apply false // Compose Compiler와 호환되는 버전
    id("com.android.application") version "8.1.1" apply false
    id("com.android.library") version "8.1.1" apply false
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
