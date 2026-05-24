plugins {
    `java-library`
    id("io.papermc.paperweight.userdev")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly("org.jetbrains:annotations:26.0.2")
    annotationProcessor("org.jetbrains:annotations:26.0.2")


    paperweight.paperDevBundle("26.1.2.build.+")
    /*compileOnly(

    )*/
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release = 25
    }
}


