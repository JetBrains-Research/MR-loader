package loader.gerrit.iterators

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import java.io.File


open class FilesListIterator<T>(
  files: Sequence<File>,
  valueSerializer: KSerializer<T>
) : Iterator<T> {
  private var filesIter = FilesIterator(files, ListSerializer(ListSerializer(valueSerializer)))
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
    while (filesIter.hasNext()) {
      val listOfList = filesIter.next()
      if (listOfList.isEmpty()) continue

      listIter = listOfList.iterator()
      resetValueIterator()
      return
    }
  }

  private fun resetValueIterator() {
    val list = listIter!!.next()
    valueIter = list.iterator()
  }
}
