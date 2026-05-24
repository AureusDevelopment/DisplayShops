plugins {
    `paper-conv`
    id("io.papermc.paperweight.userdev")
}

dependencies {
    compileOnly(
        paperweight.paperDevBundle("1.21.7-R0.1-SNAPSHOT")
    )
}
