package loader.gerrit.iterators

import client.ClientGerritREST
import kotlinx.serialization.KSerializer
import java.io.File

open class FilesIterator<T>(files: Sequence<File>, private val valueSerializer: KSerializer<T>) : Iterator<T> {
  private val fileIter = files.iterator()
  private val json = ClientGerritREST.json

  override fun hasNext() = fileIter.hasNext()

  override fun next(): T =
    json.decodeFromString(valueSerializer, fileIter.next().readText())
}