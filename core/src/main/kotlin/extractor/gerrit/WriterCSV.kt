package extractor.gerrit

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class WriterCSV(private val file: File, val type: TypeCSV) {
  companion object {
    const val MAX_SIZE = 2_000_000
  }

  enum class TypeCSV(val fields: Array<String>) {
    CHANGES(
      arrayOf(
        "created_at",
        "number",
        "key_user",
        "status",
        "comment",
        "key_change",
        "updated_time",
        "subject"
      )
    ),
    CHANGES_FILES(
      arrayOf("key_change", "key_file")
    ),
    CHANGES_REVIEWER(arrayOf("key_change", "key_user")),
    COMMITS(arrayOf("oid", "committed_date", "key_commit", "key_change")),
    COMMITS_AUTHOR(arrayOf("key_commit", "author_key_user", "committer_key_user", "uploader_key_user")),
    COMMITS_FILE(
      arrayOf(
        "key_commit",
        "key_file",
        "lines_inserted",
        "lines_deleted",
        "size",
        "size_delta",
        "status"
      )
    ),
    FILES(arrayOf("path", "key")),
    USERS(arrayOf("name", "email", "login")),
    COMMENTS_FILE(arrayOf("key_change", "key_user", "time", "key_file")),
    COMMENTS_PATCH(arrayOf("key_change", "key_user", "time", "oid")),
  }

  private val stringBuilder = StringBuilder()
  private val writer: BufferedWriter

  init {
    if (!file.parentFile.exists()) file.parentFile.mkdirs()
    writer = BufferedWriter(FileWriter(file))
    addLineCSV(*type.fields)
  }

  fun addLineCSV(vararg fields: String) {
    if (fields.size != type.fields.size) {
      throw Exception("Wrong number of fields for type: $type")
    }
    stringBuilder.appendLine(fields.joinToString("|"))

    if (stringBuilder.length > MAX_SIZE) {
      writeToFile()
    }
  }

  fun writeToFile() {
    if (stringBuilder.isNotEmpty()) {
      writer.append(stringBuilder)
      stringBuilder.clear()
    }
  }

  fun close() = writer.close()

}
