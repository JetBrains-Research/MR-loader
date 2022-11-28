package extractor.gerrit

import java.io.File

class WriterProvider {
  private val writers = HashMap<String, HashMap<String, WriterCSV>>()
  private val projects = ArrayDeque<String>()

  fun getWriter(project: String, type: WriterCSV.TypeCSV, file: File): WriterCSV {
    val writer = writers
      .computeIfAbsent(project) {
        projects.addLast(project)
        HashMap()
      }
      .computeIfAbsent(type.name) { WriterCSV(file, type) }

    if (projects.size > 2) {
      val projectRemove = projects.removeFirst()
      writers[projectRemove]!!.values.forEach {
        it.writeToFile()
        it.close()
      }
      writers.remove(projectRemove)
    }
    return writer
  }

  fun writeAllAndClose() {
    writers.values.forEach { projectWriters ->
      projectWriters.values.forEach {
        it.writeToFile()
        it.close()
      }
    }
    writers.clear()
    projects.clear()
  }
}
