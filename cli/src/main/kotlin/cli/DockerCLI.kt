package cli

import cli.gerrit.DockerGerritCLI
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class DockerCLI : CliktCommand() {
  override fun run() {}
}

fun main(args: Array<String>) = DockerCLI().subcommands(
  DockerGerritCLI()
).main(args)
