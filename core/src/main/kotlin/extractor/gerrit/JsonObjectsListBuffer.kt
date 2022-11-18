package extractor.gerrit

import java.io.File

class JsonObjectsListBuffer(directory: File) : JsonObjectBuffer(directory) {
  init {
    // TODO: bad logic rewrite
    addStart()
  }

  override fun addStart() {
    stringBuffer.append("[")
  }

  override fun addEnd() {
    stringBuffer.append("]")
  }

  fun addObject(rawJson: String) {
    val trimmedRawJson = rawJson.trim()
    addRawJson("$trimmedRawJson, ")
  }
}