package client

import java.text.SimpleDateFormat
import java.util.*

object ClientUtil {
  fun getDateFormatterGithub(): SimpleDateFormat {
    val dateFormat = SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US
    )

    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    return dateFormat
  }

  fun getDateFormatterGetter(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSSSS")

}