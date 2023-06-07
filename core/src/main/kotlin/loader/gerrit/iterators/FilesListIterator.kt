package loader.gerrit.iterators

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File


open class FilesListIterator<T>(
  files: Sequence<File>,
  private val json: Json,
  private val valueSerializer: KSerializer<T>
) : Iterator<T> {
  private var fileIter = files.iterator()
  private var listIter: Iterator<List<T>>? = null
  private var valueIter: Iterator<T>? = null

  override fun next(): T {
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
      val serializer = ListSerializer(ListSerializer(valueSerializer))
      val listOfList = json.decodeFromString(serializer, file.readText())
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
