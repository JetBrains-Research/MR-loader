package cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class CLI : CliktCommand() {
    override fun run() {}
}

fun main(args: Array<String>) = CLI().subcommands(
    GerritCLI()
).main(args)
