package cli

import cli.gerrit.ShadowJarGerritCLI
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class ShadowJarCLI : CliktCommand() {
    override fun run() {}
}

fun main(args: Array<String>) = ShadowJarCLI().subcommands(
    ShadowJarGerritCLI()
).main(args)
