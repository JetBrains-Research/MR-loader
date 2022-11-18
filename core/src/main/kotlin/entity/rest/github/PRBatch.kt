package entity.rest.github

import com.example.generated.GetPullRequests
import com.example.generated.getpullrequests.*

typealias PRBatch = GetPullRequests.Result

fun PRBatch.getRateLimit() = this.rateLimit!!


// PullRequests
fun PRBatch.getPRTotalCount() = this.repository!!.pullRequests.totalCount

fun PRBatch.getPullRequests() = this.repository!!.pullRequests.edges!!

fun PRBatch.firstCreatedPRCursor() = this.repository!!.pullRequests.edges!!.first()!!.cursor

fun PRBatch.size() = this.getPullRequests().size

fun PullRequestEdge?.getCreatedAt() = this!!.node!!.createdAt

fun PullRequestEdge?.getClosedAt() = this!!.node!!.closedAt

fun PullRequestEdge?.getNumber() = this!!.node!!.number

fun PullRequestEdge?.getState() = this!!.node!!.state

fun PullRequestEdge?.getAuthorProfile(): UserProfile {
  val actor = this!!.node!!.author ?: return UserProfile("", "", "")
  return UserProfile(actor.login, actor.getName(), actor.getEmail())
}

fun PullRequestEdge?.getAuthorEmail() = this!!.node!!.author!!.getEmail()

fun PullRequestEdge?.commitsIsFullyLoaded() = getCommits().size == getCommitsTotalCount()

fun PullRequestEdge?.reviewThreadsIsFullyLoaded() = getReviewThreads().size == getReviewThreadsTotalCount()

fun PullRequestEdge?.rtCommentsIsFullyLoaded(): Boolean {
  getReviewThreads().forEach {
    val allCommentsLoaded = it.getComments().size == it.getCommentsTotalCount()
    if (!allCommentsLoaded) return false
  }
  return true
}

fun PullRequestEdge?.reviewsIsFullyLoaded() = this.getReviews().size == this.getReviewsTotalCount()

fun PullRequestEdge?.commentsFullyLoaded() =
  reviewThreadsIsFullyLoaded() && rtCommentsIsFullyLoaded() && reviewsIsFullyLoaded()

fun PullRequestEdge?.isFullyLoaded() =
  commitsIsFullyLoaded() && commentsFullyLoaded()

fun PullRequestEdge?.isNotFullyLoaded() = !this.isFullyLoaded()

// Commits
fun PullRequestEdge?.getCommitsTotalCount() = this!!.node!!.commits.totalCount

fun PullRequestEdge?.getCommits() = this!!.node!!.commits.edges!!

fun PullRequestCommitEdge?.getHash() = this!!.node!!.commit.oid

// ReviewThreads
fun PullRequestEdge?.getReviewThreads() = this!!.node!!.reviewThreads.edges!!

fun PullRequestEdge?.getReviewThreadsTotalCount() = this!!.node!!.reviewThreads.totalCount

fun PullRequestReviewThreadEdge?.getComments() = this!!.node!!.comments.nodes!!

fun PullRequestReviewThreadEdge?.getCommentsTotalCount() = this!!.node!!.comments.totalCount

fun PullRequestReviewComment?.getAuthorProfile(): UserProfile {
  val actor = this!!.author!!
  return UserProfile(actor.login, actor.getName(), actor.getEmail())
}

fun PullRequestReviewComment?.getAuthorEmail() = this!!.author!!.getEmail()

// Reviews
fun PullRequestEdge?.getReviews() = this!!.node!!.reviews!!.edges!!

fun PullRequestEdge?.getReviewsTotalCount() = this!!.node!!.reviews!!.totalCount

fun PullRequestReviewEdge?.getAuthorProfile(): UserProfile {
  val actor = this!!.node!!.author!!
  return UserProfile(actor.login, actor.getName(), actor.getEmail())
}

fun PullRequestReviewEdge?.getAuthorEmail() = this!!.node!!.author!!.getEmail()

fun Actor?.getEmail(): String =
  when (this) {
    is User2 -> this.email
    is Mannequin -> this.email!!
    is Organization -> this.email!!
    is EnterpriseUserAccount -> this.user!!.email
    else -> ""
  }

fun Actor?.getName(): String =
  when (this) {
    is User2 -> this.name ?: ""
    is Organization -> this.name!!
    is EnterpriseUserAccount -> this.user!!.name!!
    else -> ""
  }
