plugins {
    `paper-conv`
    id("io.papermc.paperweight.userdev")
}

dependencies {
    compileOnly(
        paperweight.paperDevBundle("1.21.9-R0.1-SNAPSHOT")
    )
}
