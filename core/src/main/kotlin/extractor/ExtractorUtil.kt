package extractor

import java.io.File

object ExtractorUtil {
  fun lineCSV(vararg fields: String) = "${fields.joinToString("|")}\n"

  fun getFilesIgnoreHidden(dir: File) =
    dir.walk()
      .filter { it.isFile && !it.name.startsWith(".") }
      .sortedBy { it.name.toInt() }
}