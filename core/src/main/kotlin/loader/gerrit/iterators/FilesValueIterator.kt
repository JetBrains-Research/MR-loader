package loader.gerrit.iterators

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

open class FilesValueIterator<T>(
  files: Sequence<File>,
  private val json: Json,
  private val valueSerializer: KSerializer<T>
) : Iterator<T> {
  private var fileIter = files.iterator()
  private var valueIter: Iterator<T>? = null

  override fun next(): T {
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
        val serializer = MapSerializer(Int.serializer(), valueSerializer)
        valueIter = json.decodeFromString(serializer, file.readText()).values.iterator()
      }
    }
  }

  private fun valueIteratorIsFinished() = valueIter == null || valueIter?.hasNext() == false

}
