package client

import com.example.generated.getpullrequests.RateLimit
import entity.rest.github.RateLimitREST
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

  private fun waitFor(resetDate: Date, currentDate: Date) {
    if (resetDate < currentDate) return
    val waitFor = (resetDate.time - currentDate.time) / 2
    println("Rate limit exceeded. Waiting for ${waitFor / (1000 * 60)} minutes.")
    Thread.sleep(waitFor)
  }

  fun waitRateLimit(rateLimit: RateLimit) {
    val currentDate = Date()
    val dateFormat = getDateFormatterGithub()
    val resetDate = dateFormat.parse(rateLimit.resetAt)
    waitFor(resetDate, currentDate)
  }

  fun waitRateLimit(rateLimit: RateLimitREST) {
    val currentDate = Date()
    val resetDate = Date(rateLimit.resources.core.reset.toLong() * 1000)
    waitFor(resetDate, currentDate)
  }
}