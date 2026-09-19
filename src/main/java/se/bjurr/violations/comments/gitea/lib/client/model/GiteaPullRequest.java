package se.bjurr.violations.comments.gitea.lib.client.model;

/**
 * A Gitea pull request, trimmed to the fields this tool needs: {@code number}, and the {@code
 * head}/{@code base} refs (with their commit SHAs, needed to create review comments).
 */
public class GiteaPullRequest {
  private Long number;
  private GiteaBranchRef head;
  private GiteaBranchRef base;

  public Long getNumber() {
    return this.number;
  }

  public GiteaBranchRef getHead() {
    return this.head;
  }

  public GiteaBranchRef getBase() {
    return this.base;
  }

  @Override
  public String toString() {
    return "GiteaPullRequest [number="
        + this.number
        + ", head="
        + this.head
        + ", base="
        + this.base
        + "]";
  }
}
