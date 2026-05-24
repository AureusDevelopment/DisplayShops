plugins {
    `java-library`
}

dependencies {
    compileOnly(project(":api"))

    compileOnly("org.jetbrains:annotations:26.0.2")
    annotationProcessor("org.jetbrains:annotations:26.0.2")
}


repositories {
    mavenLocal()
}

