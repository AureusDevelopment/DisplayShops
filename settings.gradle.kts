pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}


rootProject.name = "DisplayShops"
include("core")
include("api")
include("nms")
include("nms:v1_8_r3")
include("nms:v1_9_r1")
include("nms:v1_9_r2")
include("nms:v1_10_r1")
include("nms:v1_11_r1")
include("nms:v1_12_r1")
include("nms:v1_13_r1")
include("nms:v1_13_r2")
include("nms:v1_14_r1")
include("nms:v1_15_r1")
include("nms:v1_16_r1")
include("nms:v1_16_r2")
include("nms:v1_16_r3")
include("nms:v1_17_r1")
include("nms:v1_18_r1")
include("nms:v1_18_r2")
include("nms:v1_19_r1")
include("nms:v1_19_r2")
include("nms:v1_19_r3")
include("nms:v1_20_r1")
include("nms:v1_20_r2")
include("nms:v1_20_r3")
include("nms:v1_20_r6")
include("nms:v1_21_r1")
include("nms:v1_21_r3")
include("nms:v1_21_r4")
include("nms:v1_21_r5")
include("nms:v1_21_r6")
include("nms:v1_21_r7")
include("nms:v1_21_r8")
include("nms:v1_21_r9")
include("nms:v1_21_r10")
include("nms:v1_21_r11")
include("nms:v26.1.2")