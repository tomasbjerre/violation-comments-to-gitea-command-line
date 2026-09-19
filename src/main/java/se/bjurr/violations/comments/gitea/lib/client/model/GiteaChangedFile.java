package se.bjurr.violations.comments.gitea.lib.client.model;

/** One entry of {@code GET /repos/{owner}/{repo}/pulls/{index}/files}. */
public class GiteaChangedFile {
  private final String filename;
  private final String previousFilename;
  private final String status;

  public GiteaChangedFile() {
    this.filename = null;
    this.previousFilename = null;
    this.status = null;
  }

  public GiteaChangedFile(
      final String filename, final String previousFilename, final String status) {
    this.filename = filename;
    this.previousFilename = previousFilename;
    this.status = status;
  }

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
