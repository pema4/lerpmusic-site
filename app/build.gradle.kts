plugins {
    alias(libs.plugins.ktor.plugin)
}

ktor {
    fatJar {
        archiveFileName.set("lerpmusic-website.jar")
    }
}

val deployWebsite = task<Task>("deployWebsite") {
    dependsOn(":app:buildFatJar")
    group = "deployment"

    doLast {
        exec {
            commandLine(
                "scp",
                "build/libs/lerpmusic-website.jar",
                "lerpmusic:~/lerpmusic/lerpmusic-website.jar"
            )
        }

        exec {
            commandLine(
                "ssh",
                "lerpmusic",
                "sudo ~/lerpmusic/run-sudo.sh"
            )
        }

        exec {
            commandLine(
                "ssh",
                "lerpmusic",
                "~/lerpmusic/run.sh"
            )
        }
    }
}