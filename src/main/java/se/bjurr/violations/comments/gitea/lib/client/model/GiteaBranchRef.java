package se.bjurr.violations.comments.gitea.lib.client.model;

/** The {@code head}/{@code base} object of a {@link GiteaPullRequest} (Gitea's PRBranchInfo). */
public class GiteaBranchRef {
  private final String ref;
  private final String sha;

  public GiteaBranchRef() {
    this.ref = null;
    this.sha = null;
  }

  public GiteaBranchRef(final String ref, final String sha) {
    this.ref = ref;
    this.sha = sha;
  }

  public String getRef() {
    return this.ref;
  }

  public String getSha() {
    return this.sha;
  }

  @Override
  public String toString() {
    return "GiteaBranchRef [ref=" + this.ref + ", sha=" + this.sha + "]";
  }
}
