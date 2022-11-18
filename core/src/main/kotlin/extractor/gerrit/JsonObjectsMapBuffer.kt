package extractor.gerrit

import java.io.File

class JsonObjectsMapBuffer(directory: File) : JsonObjectBuffer(directory) {
  init {
    // TODO: bad logic rewrite
    addStart()
  }

  override fun addStart() {
    stringBuffer.append("{")
  }

  override fun addEnd() {
    stringBuffer.append("}")
  }

  fun addEntry(rawJson: String, id: Int) {
    val trimmedRawJson = rawJson.trim()
    addRawJson("\"$id\" : $trimmedRawJson, ")
  }
}