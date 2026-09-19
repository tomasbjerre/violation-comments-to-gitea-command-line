package se.bjurr.violations.comments.gitea.lib.client.model;

/**
 * A pull request review. Every single-file violation comment is posted as its own review with one
 * comment in it (event {@code COMMENT}, submitted immediately) - Gitea only supports deleting a
 * whole review, not one comment inside it, so "one review" is this tool's deletable unit for an
 * inline comment. {@code POST/GET/DELETE .../pulls/{index}/reviews[/{id}]}.
 */
public class GiteaReview {
  private Long id;
  private String state;
  private Integer commentsCount;

  public Long getId() {
    return this.id;
  }

  public String getState() {
    return this.state;
  }

  public Integer getCommentsCount() {
    return this.commentsCount;
  }

  @Override
  public String toString() {
    return "GiteaReview [id="
        + this.id
        + ", state="
        + this.state
        + ", commentsCount="
        + this.commentsCount
        + "]";
  }
}
