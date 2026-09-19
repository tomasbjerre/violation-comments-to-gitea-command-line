package se.bjurr.violations.comments.gitea.lib.client.model;

import java.util.List;

/** {@code POST .../pulls/{index}/reviews} body, serialized with {@code commit_id}. */
public class GiteaCreateReviewRequest {
  private final String commitId;
  private final String event;
  private final List<GiteaReviewCommentInput> comments;

  public GiteaCreateReviewRequest() {
    this.commitId = null;
    this.event = null;
    this.comments = null;
  }

  public GiteaCreateReviewRequest(
      final String commitId, final String event, final List<GiteaReviewCommentInput> comments) {
    this.commitId = commitId;
    this.event = event;
    this.comments = comments;
  }

  public String getCommitId() {
    return this.commitId;
  }

  public String getEvent() {
    return this.event;
  }

  public List<GiteaReviewCommentInput> getComments() {
    return this.comments;
  }
}
