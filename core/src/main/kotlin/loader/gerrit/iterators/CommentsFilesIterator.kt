package loader.gerrit.iterators

import entity.rest.gerrit.CommentsREST
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.io.File

class CommentsFilesIterator(files: Sequence<File>) :
  FilesIterator<MutableMap<Int, CommentsREST>>(
    files,
    serializer
  ) {
  companion object {
    private val serializer: KSerializer<MutableMap<Int, CommentsREST>> = serializer()
  }
}