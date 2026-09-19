package se.bjurr.violations.comments.gitea.lib.client.model;

/** One comment of a {@link GiteaCreateReviewRequest}, serialized as {@code new_position}. */
public class GiteaReviewCommentInput {
  private final String path;
  private final Integer newPosition;
  private final String body;

  public GiteaReviewCommentInput() {
    this.path = null;
    this.newPosition = null;
    this.body = null;
  }

  public GiteaReviewCommentInput(final String path, final Integer newPosition, final String body) {
    this.path = path;
    this.newPosition = newPosition;
    this.body = body;
  }

  public String getPath() {
    return this.path;
  }

  public Integer getNewPosition() {
    return this.newPosition;
  }

  public String getBody() {
    return this.body;
  }
}
