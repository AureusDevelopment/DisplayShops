import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.text.SimpleDateFormat
import java.util.*
import org.gradle.internal.os.OperatingSystem
import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml
import xyz.jpenilla.resourcefactory.bukkit.Permission

plugins {
    id("java-library")
    id("xyz.jpenilla.resource-factory") version "1.3.1"
    id("com.gradleup.shadow") version "9.2.2"
}

var mainPath = rootProject.name

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/")
    maven("https://nexus.iridiumdevelopment.net/repository/maven-releases/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.glaremasters.me/repository/towny/")
    maven("https://repo.bg-software.com/repository/api/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.auxilor.io/repository/maven-public/")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://maven.devs.beer/")
    maven("https://mvn.wesjd.net/")

}

tasks.shadowJar {
    relocate("net.wesjd.anvilgui", "xzot1k.plugins.ds.anvilgui")
    relocate("de.tr7zw.changeme.nbtapi", "xzot1k.plugins.ds.nbtapi")
    relocate("org.bstats", "xzot1k.plugins.ds.bstats")

    archiveBaseName = rootProject.name
    archiveClassifier.set(null)

    mergeServiceFiles()
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    manifest {
        attributes(Pair("Build-Time-Long", Date().time))
        attributes(Pair("Build-Time", SimpleDateFormat("HH:mm:ss dd.MM.yyyy").format(Date())))
    }
}


tasks {
    compileJava {
        options.release = 21
        options.encoding = "UTF-8"
    }
    /*register("run1_21_11", RunServer::class){
        minecraftVersion("1.21.10")
        pluginJars.from(shadowJar.flatMap { it.archiveFile })
        runDirectory = rootProject.layout.projectDirectory.dir("run1_21_11")
        systemProperties["Paper.IgnoreJavaVersion"] = true
    }*/
    jar {
        manifest.attributes(
            "paperweight-mappings-namespace" to "spigot"
        )
    }
}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    disableAutoTargetJvm()
}

sourceSets.main {
    resourceFactory {
        bukkitPluginYaml {
            main = "xzot1k.plugins.ds.DisplayShops"
            name = rootProject.name
            version = rootProject.version.toString()
            apiVersion = "1.13"
            author = "XZot1K"
            authors = listOf("petulikan1")
            description = "Create immersive simplistic shops with animations, efficient transaction handling, and much more!"
            softDepend = listOf(
                "Vault",
                "NBTAPI",
                "HeadDatabase",
                "Prisma",
                "PlaceholderAPI",
                "PlotSquared",
                "BentoBox",
                "BSkyBlock",
                "BentoBox-BSkyBlock",
                "Oneblock",
                "CMI",
                "Essentials",
                "MultiWorld",
                "Multiverse-Core",
                "ASkyBlock",
                "USkyBlock",
                "FabledSkyBlock",
                "PlayerWorldsPro",
                "WorldGuard",
                "WorldEdit",
                "ItemsAdder",
                "PlayerPoints",
                "eco",
                "EcoBits",
                "Geyser-Spigot",
                "DecentHolograms",
                "EliteEnchantment",
                "Nexo"
            )
            loadBefore = listOf("GriefPrevention", "JetsMinions")
            load = BukkitPluginYaml.PluginLoadOrder.POSTWORLD

            commands.register("displayshops") {
                description = "The main command for the DisplayShops plugin."
                aliases = listOf("ds")

            }
            commands.register("dsfilter") {
                description = "The filter command for searching in the Shops visit menu."
                aliases = listOf("swfilter")
            }
            permissions {
                register("displayshops.*") {
                    description = "Gives all permissions contained in the plugin."
                    default = Permission.Default.OP
                    val commands: List<String> = listOf(
                        "user",
                        "give",
                        "admin",
                        "reload",
                        "stock",
                        "owner",
                        "cmr",
                        "dmr",
                        "sm",
                        "mrl",
                        "adminedit",
                        "adminhelp",
                        "admindelete",
                        "cdbypass",
                        "info",
                        "cleanup",
                        "bbmaccess",
                        "clear",
                        "reset",
                        "block",
                        "cost",
                        "rcost",
                        "id"
                    )
                    commands.forEach {
                        children.put("displayshops.$it", true)
                    }
                }

                register("displayshops.user") {
                    description = "Gives all permissions that were made for a normal player."
                    default = Permission.Default.OP
                    val commands: List<String> = listOf(
                        "buy",
                        "create",
                        "delete",
                        "craft",
                        "visit",
                        "help",
                        "rent",
                        "edit",
                        "assistants",
                        "description",
                        "advertise",
                        "notify",
                        "buyprice",
                        "sellprice",
                        "withdraw",
                        "deposit",
                        "balwithdraw",
                        "baldeposit",
                        "currency.Vault",
                        "currency.item-for-item"
                    )
                    commands.forEach {
                        children.put("displayshops.$it", true)
                    }
                }
                listOf(
                    Pair("buy","Allows the player to buy one shop creation item from the server."),
                    Pair("give","Allows the sender to give display shop creation items to players."),
                    Pair("admin","Allows the player to set the shop in their line of sight to admin mode and bypass the visit command charge."),
                    Pair("reload","Allows the sender to reload the shops, configurations, etc."),
                    Pair("create","Allows the player to create new shops."),
                    Pair("help","Allows the sender to view the user help message."),
                    Pair("edit","Allows the player to do anything to any shop without being the rightful owner."),
                    Pair("adminedit","Allows the player to do anything to any shop without being the rightful owner."),
                    Pair("bypass","Allows the player to bypass the charge when buying from a shop (Owner still gets the amount)."),
                    Pair("stock","Allows the player to set the stock of the shop they are looking at."),
                    Pair("owner","Allows the player to set the owner of the shop they are looking at."),
                    Pair("cmr","Allows the player to create a market region."),
                    Pair("dmr","Allows the player to delete a market region."),
                    Pair("sm","Allows the player to enter region selection mode."),
                    Pair("mrl","Allows the player to view all market region ids."),
                    Pair("commands","Allows the player to manage commands of admin shops."),
                    Pair("delete","Allows the player to delete owned shops."),
                    Pair("adminhelp","The admin help message will be shown instead of the user help message."),
                    Pair("admindelete","Allows the player to delete a shop even if they do not own it or it is an admin shop."),
                    Pair("cdbypass","Allows the player to bypass any cooldowns the plugin uses."),
                    Pair("craft","Allows the player to craft the shop creation item."),
                    Pair("info","Allows the player to see information about the current plugin build."),
                    Pair("visit","Allows the player to teleport to a safe location near a shop."),
                    Pair("cleanup","Allows the player to use the shop cleanup command."),
                    Pair("assistants","Allows the player to add/remove assistants from a shop as long as access to the edit menu is provided."),
                    Pair("description","Allows the player to modify a shop's description."),
                    Pair("bbmaccess","Allows the player to unlock/lock all base-block materials for a player using the unlock/lock commands."),
                    Pair("clear","Allows the sender to use the clear command to remove all shops from a defined world."),
                    Pair("rent","Allows the player to use the rent command to rent out a market region."),
                    Pair("reset","Allows the sender to completely reset a market region releasing any for of rent data."),
                    Pair("block","This allows the player to block the item in-hand from being sold in shops."),
                    Pair("advertise","Allows the player to advertise their shop via command."),
                    Pair("notify","Allows the player to toggle shop sale notifications for themselves."),
                    Pair("buyprice","Allows the player to set the buy price of a shop they can access via command."),
                    Pair("sellprice","Allows the player to set the sell price of a shop they can access via command."),
                    Pair("cost","Allows the player to use the market region rent cost command."),
                    Pair("rcost","Allows the player to use the market region rent renewal cost command."),
                    Pair("withdraw","Allows the player to use the withdraw stock command."),
                    Pair("deposit","Allows the player to use the deposit stock command."),
                    Pair("balwithdraw","Allows the player to use the withdraw balance command."),
                    Pair("baldeposit","Allows the player to use the deposit balance command."),
                    Pair("id","Allows the player to retrieve the ID of a shop in view."),
                ).forEach { register("displayshops.${it.first}"){
                    description = it.second
                    default = Permission.Default.OP
                }}
            }
        }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    implementation(project(":api"))


    //SPIGOT
    runtimeOnly(project(":nms:v1_8_r3"))
    runtimeOnly(project(":nms:v1_9_r1"))
    runtimeOnly(project(":nms:v1_9_r2"))
    runtimeOnly(project(":nms:v1_10_r1"))
    runtimeOnly(project(":nms:v1_11_r1"))
    runtimeOnly(project(":nms:v1_12_r1"))
    runtimeOnly(project(":nms:v1_13_r1"))
    runtimeOnly(project(":nms:v1_13_r2"))
    runtimeOnly(project(":nms:v1_14_r1"))
    runtimeOnly(project(":nms:v1_15_r1"))
    runtimeOnly(project(":nms:v1_16_r1"))
    runtimeOnly(project(":nms:v1_16_r2"))
    runtimeOnly(project(":nms:v1_16_r3"))

    //PAPER - REOBF
    runtimeOnly(project((":nms:v1_17_r1"), configuration = "reobf"))
    runtimeOnly(project((":nms:v1_18_r1"), configuration = "reobf"))
    runtimeOnly(project((":nms:v1_18_r2"), configuration = "reobf"))
    runtimeOnly(project((":nms:v1_19_r1"), configuration = "reobf"))
    runtimeOnly(project((":nms:v1_19_r2"), configuration = "reobf"))
    runtimeOnly(project((":nms:v1_19_r3"), configuration = "reobf"))
    runtimeOnly(project((":nms:v1_20_r1"), configuration = "reobf"))
    runtimeOnly(project((":nms:v1_20_r2"), configuration = "reobf"))
    runtimeOnly(project((":nms:v1_20_r3"), configuration = "reobf"))

    //PAPER - NO REOBF
    runtimeOnly(project((":nms:v1_20_r6")))
    runtimeOnly(project((":nms:v1_21_r1")))
    runtimeOnly(project((":nms:v1_21_r3")))
    runtimeOnly(project((":nms:v1_21_r4")))
    runtimeOnly(project((":nms:v1_21_r5")))
    runtimeOnly(project((":nms:v1_21_r6")))
    runtimeOnly(project((":nms:v1_21_r7")))
    runtimeOnly(project((":nms:v1_21_r8")))
    runtimeOnly(project((":nms:v1_21_r9")))
    runtimeOnly(project((":nms:v1_21_r10")))
    runtimeOnly(project((":nms:v1_21_r11")))
    runtimeOnly(project((":nms:v26.1.2")))


    compileOnly("com.mojang:authlib:1.5.21")
    compileOnly("org.geysermc.geyser:api:2.2.0-SNAPSHOT")
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.8.8")
    compileOnly("de.tr7zw:item-nbt-api-plugin:2.13.1")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        isTransitive = false
    }

    compileOnly("me.clip:placeholderapi:2.11.1")
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.1")
    compileOnly("dev.lone:api-itemsadder:4.0.10")
    compileOnly("com.wasteofplastic:askyblock:3.0.9.4")
    compileOnly("com.plotsquared:PlotSquared-Core:6.10.5")
    compileOnly("com.palmergames.bukkit.towny:towny:0.98.2.0")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.7-SNAPSHOT")
    compileOnly("com.bgsoftware:SuperiorSkyblockAPI:1.11.0")
    compileOnly("world.bentobox:bentobox:1.20.1")
    compileOnly("world.bentobox:bskyblock:1.15.2")
    compileOnly("com.iridium:IridiumSkyblock:4.0.8")
    compileOnly("com.songoda:skyblock:2.5.0")
    compileOnly("com.willfp:eco:6.60.0")
    compileOnly("com.willfp:core-plugin:1.8.3")
    compileOnly("org.black_ixx:playerpoints:3.2.6")
    compileOnly("com.Zrips:CMI:9.6.2.3")
    compileOnly("io.th0rgal:oraxen:1.167.0")
    compileOnly("com.edwardbelt.edprison:EdPrision:5.4")
    compileOnly("com.google.code.gson:gson:2.10.1")

    compileOnly("org.apache.commons:commons-lang3:3.20.0")

    //implementation("com.github.TheDevTec:TheAPI-Shared:13.8.2")
    implementation("net.wesjd:anvilgui:1.10.12-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:3.0.2")
    compileOnly("com.nexomc:nexo:1.10.0") {
        isTransitive = false
    }
}


val shadowJarProvider = tasks.named<ShadowJar>("shadowJar")

val isWindows = OperatingSystem.current().isWindows
val isLinux = OperatingSystem.current().isLinux

val pluginPath = "/home/petu/SERVERS/26_1_2_SERVER/plugins/"

val deleteOldPlugin by tasks.registering(Delete::class) {

    enabled = isLinux

    delete(fileTree(pluginPath) {
        include("*${rootProject.name}*.jar")
    })

    delete(fileTree(rootProject.projectDir.resolve("Builds").path) {
        include("*${rootProject.name}*.jar")
    })
}

val copyToBuilds by tasks.registering(Copy::class) {
    dependsOn(shadowJarProvider, createBuildDir)
    from(shadowJarProvider.flatMap { it.archiveFile })
    into(rootProject.projectDir.resolve("Builds"))
}

val copyToServer by tasks.registering(Copy::class) {
    dependsOn(shadowJarProvider, deleteOldPlugin)

    from(shadowJarProvider.flatMap { it.archiveFile })

    if (isWindows) {
        into("C:/SERVERS/1_21_4_SERVER_2/plugins")
    }
    if (isLinux) {
        into(pluginPath)
    }
}


val createBuildDir by tasks.registering {

    val buildsDir = rootProject.layout.projectDirectory.dir("Builds")

    doLast {
        if (!buildsDir.asFile.exists())
            buildsDir.asFile.mkdirs()
    }
}
tasks.build {
    dependsOn(copyToBuilds, copyToServer)
}

configurations {
    all {
        exclude(group = "io.netty", module = "netty-transport-native-epoll")
    }
}
