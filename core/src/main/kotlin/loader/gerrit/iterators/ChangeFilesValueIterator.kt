package loader.gerrit.iterators

import entity.rest.gerrit.ChangeGerrit
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class ChangeFilesValueIterator(files: Sequence<File>, private val json: Json) : Iterator<ChangeGerrit> {
  private var fileIter = files.iterator()
  private var valueIter: Iterator<ChangeGerrit>? = null

  override fun next(): ChangeGerrit {
    return valueIter!!.next()
  }

  override fun hasNext(): Boolean {
    checkAndSetIterators()
    return !valueIteratorIsFinished()
  }

  private fun checkAndSetIterators() {
    if (valueIteratorIsFinished()) {
      if (fileIter.hasNext()) {
        val file = fileIter.next()
        valueIter = json.decodeFromString<Map<Int, ChangeGerrit>>(file.readText()).values.iterator()
      }
    }
  }

  private fun valueIteratorIsFinished() = valueIter == null || valueIter?.hasNext() == false

}
