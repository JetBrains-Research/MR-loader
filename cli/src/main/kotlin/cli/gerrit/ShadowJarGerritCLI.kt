package cli.gerrit

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import loader.gerrit.GerritLoader

class ShadowJarGerritCLI : GerritCLI() {
  private val dataDir by option("--data-dir", help = "Folder for saving results.").file().required()

  override fun run() {
    runBlocking {
      val gerritLoader =
        GerritLoader(url, dataDir, patchsetCommentKey = patchsetCommentKey)
      gerritLoader.run(
        ignoreLoad = false,
        numOfThreads = numOfThreads
      )
      gerritLoader.checkLoadedData()
    }
  }
}
