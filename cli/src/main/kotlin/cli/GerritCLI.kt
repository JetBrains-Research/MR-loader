package cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import extractor.gerrit.ExtractorChanges
import kotlinx.coroutines.runBlocking
import loader.gerrit.LoaderChanges
import java.text.SimpleDateFormat
import java.util.*

class GerritCLI :
  CliktCommand(name = "GerritLoad", help = "Load changes from Gerrit.") {

  companion object {
    private const val DATE_PATTERN = "yyyy-MM-dd HH:mm:ss"
  }

  private val url by option("--url", help = "Base url of Gerrit site.").required()
  private val dataDir by option("--data-dir", help = "Folder for saving results.").file().required()
  private val patchsetCommentKey by option(
    "--patchset-comment-key",
    help = "Comments key for the whole patchset."
  ).default(ExtractorChanges.DEFAULT_PATCHSET_COMMENT_KEY)
  private val numOfThreads by option(
    "--num-of-threads",
    help = "Number of threads for loading data. By default ${LoaderChanges.DEFAULT_NUM_THREADS}"
  ).int().default(LoaderChanges.DEFAULT_NUM_THREADS)

  override fun run() {
    runBlocking {
      val extractorChanges =
        ExtractorChanges(url, dataDir, patchsetCommentKey = patchsetCommentKey)
      extractorChanges.run(
        ignoreLoad = false,
        numOfThreads = numOfThreads
      )
    }
  }

  private fun dateHelpMessage(value: String) = "$value threshold. Enter date in following format: $DATE_PATTERN"
  private fun convertDate(dateString: String): Date {
    val dateFormat = SimpleDateFormat(DATE_PATTERN)
    return dateFormat.parse(dateString)
  }
}
