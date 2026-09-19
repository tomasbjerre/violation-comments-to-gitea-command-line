package se.bjurr.violations.comments.gitea.lib.client.model;

/**
 * A general (top-level) PR comment - a Gitea issue comment, since pull requests are backed by
 * issues. {@code GET/POST .../issues/{index}/comments}.
 */
public class GiteaIssueComment {
  private final Long id;
  private final String body;

  public GiteaIssueComment() {
    this.id = null;
    this.body = null;
  }

  public GiteaIssueComment(final Long id, final String body) {
    this.id = id;
    this.body = body;
  }

  public Long getId() {
    return this.id;
  }

  public String getBody() {
    return this.body;
  }

  @Override
  public String toString() {
    return "GiteaIssueComment [id=" + this.id + ", body=" + this.body + "]";
  }
}
