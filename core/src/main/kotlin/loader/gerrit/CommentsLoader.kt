package loader.gerrit

import entity.rest.gerrit.CommentsREST
import loader.gerrit.iterators.CommentsFilesIterator
import java.io.File

class CommentsLoader(files: Sequence<File>) {
  private val loadedComments = mutableListOf<MutableMap<Int, CommentsREST>>()
  private val commentsFilesIterator = CommentsFilesIterator(files)

  fun get(id: Int): CommentsREST? {
    val iteratorLoadedComments = loadedComments.iterator()
    while (iteratorLoadedComments.hasNext()) {
      val loaded = iteratorLoadedComments.next()
      loaded.remove(id)?.let { return it }
      if (loaded.isEmpty()) {
        iteratorLoadedComments.remove()
      }
    }

    while (commentsFilesIterator.hasNext()) {
      val comments = commentsFilesIterator.next()
      val value = comments.remove(id)
      loadedComments.add(comments)
      value?.let { return it }
    }

    return null
  }

}
