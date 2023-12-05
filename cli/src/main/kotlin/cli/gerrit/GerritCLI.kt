package cli.gerrit

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import loader.gerrit.GerritLoader
import loader.gerrit.LoaderChanges


abstract class GerritCLI :
  CliktCommand(name = "GerritLoad", help = "Load changes from Gerrit.") {

  protected val url by option("--url", help = "Base url of Gerrit site.").required()
  protected val patchsetCommentKey by option(
    "--patchset-comment-key",
    help = "Comments key for the whole patchset. By default ${GerritLoader.DEFAULT_PATCHSET_COMMENT_KEY}"
  ).default(GerritLoader.DEFAULT_PATCHSET_COMMENT_KEY)
  protected val numOfThreads by option(
    "--num-of-threads",
    help = "Number of threads for loading data. By default ${LoaderChanges.DEFAULT_NUM_THREADS}"
  ).int().default(LoaderChanges.DEFAULT_NUM_THREADS)

}
