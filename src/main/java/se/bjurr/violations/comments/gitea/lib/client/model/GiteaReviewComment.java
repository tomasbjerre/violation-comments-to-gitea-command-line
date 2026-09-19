package se.bjurr.violations.comments.gitea.lib.client.model;

/**
 * One inline comment of a {@link GiteaReview}. {@code position} is the new-file line number it is
 * anchored to (Gitea's {@code new_position}/{@code NewLineNum} on creation, echoed back as {@code
 * position} - confirmed against a real Gitea instance to be an actual line number, not a
 * GitHub-style diff-position index). {@code GET .../pulls/{index}/reviews/{reviewId}/comments}.
 */
public class GiteaReviewComment {
  private Long id;
  private String path;
  private String body;
  private Integer position;
  private Long pullRequestReviewId;

  public Long getId() {
    return this.id;
  }

  public String getPath() {
    return this.path;
  }

  public String getBody() {
    return this.body;
  }

  public Integer getPosition() {
    return this.position;
  }

  public Long getPullRequestReviewId() {
    return this.pullRequestReviewId;
  }

  @Override
  public String toString() {
    return "GiteaReviewComment [id="
        + this.id
        + ", path="
        + this.path
        + ", body="
        + this.body
        + ", position="
        + this.position
        + ", pullRequestReviewId="
        + this.pullRequestReviewId
        + "]";
  }
}
