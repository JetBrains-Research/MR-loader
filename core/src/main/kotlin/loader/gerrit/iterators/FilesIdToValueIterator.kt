package loader.gerrit.iterators

import client.ClientGerritREST
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File

open class FilesIdToValueIterator<T>(
  files: Sequence<File>,
  valueSerializer: KSerializer<T>
) : Iterator<T> {
  private val filesIter = FilesIterator(files, MapSerializer(Int.serializer(), valueSerializer))
  private var valueIter: Iterator<T>? = null
  private val json = ClientGerritREST.json

  override fun next(): T {
    return valueIter!!.next()
  }

  override fun hasNext(): Boolean {
    checkAndSetIterators()
    return !valueIteratorIsFinished()
  }

  private fun checkAndSetIterators() {
    if (valueIteratorIsFinished()) {
      if (filesIter.hasNext()) {
        valueIter = filesIter.next().values.iterator()
      }
    }
  }

  private fun valueIteratorIsFinished() = valueIter == null || valueIter?.hasNext() == false

}
