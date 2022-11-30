package loader.gerrit.iterators

import entity.rest.gerrit.ChangeMetaData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class ChangesMetaDataFilesIterator(files: Sequence<File>, private val json: Json) : Iterator<ChangeMetaData> {
  private var fileIter = files.iterator()
  private var listIter: Iterator<List<ChangeMetaData>>? = null
  private var valueIter: Iterator<ChangeMetaData>? = null

  override fun next(): ChangeMetaData {
    return valueIter!!.next()
  }

  override fun hasNext(): Boolean = checkAndSetIterators()


  private fun checkAndSetIterators(): Boolean {
    when {
      listIter == null -> resetIterators()

      valueIter?.hasNext() == false -> {
        if (listIter!!.hasNext()) {
          resetValueIterator()
        } else {
          resetIterators()
        }
      }
    }

    return valueIter?.hasNext() ?: false
  }

  private fun resetIterators() {
    if (fileIter.hasNext()) {
      val file = fileIter.next()
      val listOfList = json.decodeFromString<List<List<ChangeMetaData>>>(file.readText())
      // assume files not empty
      listIter = listOfList.iterator()
      resetValueIterator()
    }
  }

  private fun resetValueIterator() {
    val list = listIter!!.next()
    valueIter = list.iterator()
  }
}
