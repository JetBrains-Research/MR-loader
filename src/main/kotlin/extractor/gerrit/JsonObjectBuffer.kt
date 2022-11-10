package extractor.gerrit

import extractor.ExtractorUtil.getFilesIgnoreHidden
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class JsonObjectBuffer(private val directory: File) {
  companion object {
    const val MAX_SIZE = 5_000_000
  }

  // already thread safe
  protected val stringBuffer = StringBuffer()
  private val readWriteLock = ReentrantReadWriteLock()
  private val flag = AtomicBoolean(false)
  private var fileId: Int

  init {
    val lastID = getFilesIgnoreHidden(directory).maxByOrNull { it.name.toInt() }?.name?.toInt()
    fileId = if (lastID == null) 0 else lastID + 1
  }

  protected abstract fun addStart()

  protected abstract fun addEnd()


  protected fun addRawJson(rawJson: String) {
    readWriteLock.read {
      stringBuffer.append(rawJson)
    }

    if (stringBuffer.length > MAX_SIZE) {
      if (flag.compareAndSet(false, true)) {
        // TODO: recheck it seems there is no need in double check
        if (stringBuffer.length < MAX_SIZE) {
          flag.set(false)
          return
        }

        readWriteLock.write {
          writeToFile()
          stringBuffer.delete(0, stringBuffer.length)

          addStart()
          fileId++
          flag.set(false)
        }
      }
    }
  }

  private fun writeToFile() {
    // check start
    if (stringBuffer.length > 1) {
      // remove ", "
      stringBuffer.delete(stringBuffer.length - 2, stringBuffer.length)

    }
    addEnd()
    val file = File(directory, fileId.toString())
    val writer = BufferedWriter(FileWriter(file))
    writer.use { it.append(stringBuffer) }
  }

  fun close() {
    if (stringBuffer.isNotEmpty()) {
      writeToFile()
    }
  }

}
