package se.bjurr.violations.comments.gitea.lib.client.model;

/** One entry of {@code GET /repos/{owner}/{repo}/pulls/{index}/files}. */
public class GiteaChangedFile {
  private String filename;
  private String previousFilename;
  private String status;

  public String getFilename() {
    return this.filename;
  }

  public String getPreviousFilename() {
    return this.previousFilename;
  }

  public String getStatus() {
    return this.status;
  }

  @Override
  public String toString() {
    return "GiteaChangedFile [filename="
        + this.filename
        + ", previousFilename="
        + this.previousFilename
        + ", status="
        + this.status
        + "]";
  }
}
