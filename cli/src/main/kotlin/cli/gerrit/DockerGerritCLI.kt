package cli.gerrit

import kotlinx.coroutines.runBlocking
import loader.gerrit.GerritLoader
import java.io.File

class DockerGerritCLI : GerritCLI() {
  override fun run() {
    runBlocking {
      val dataDir = File(System.getProperty("user.home"))
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