package se.bjurr.violations.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.bjurr.violations.comments.gitea.lib.LocalGiteaEnv;
import se.bjurr.violations.comments.gitea.lib.client.GiteaClient;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaIssueComment;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaReview;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaReviewComment;
import se.bjurr.violations.lib.ViolationsLogger;

/**
 * Runs the actual command-line entry point ({@link Runner}, the same code {@link Main} calls)
 * against a real local Gitea instance, parsing a real Checkstyle report file - unlike {@link
 * se.bjurr.violations.comments.gitea.lib.GiteaIntegrationTest}, which drives the internal Java API
 * directly and skips argument parsing/report discovery entirely. Skips (doesn't fail) unless that
 * local Gitea is up - see {@link LocalGiteaEnv}.
 */
class GiteaCliIntegrationTest {
  private final ViolationsLogger violationsLogger =
      new ViolationsLogger() {
        @Override
        public void log(final Level level, final String string) {
          Logger.getLogger(GiteaCliIntegrationTest.class.getSimpleName()).info(string);
        }

        @Override
        public void log(final Level level, final String string, final Throwable t) {
          Logger.getLogger(GiteaCliIntegrationTest.class.getSimpleName())
              .log(Level.SEVERE, string, t);
        }
      };

  private final LocalGiteaEnv env = LocalGiteaEnv.read();
  private GiteaClient client;

  @BeforeEach
  void setUp() {
    this.env.assumeAvailable();
    this.client = this.env.newClient(this.violationsLogger);
    this.removeAllComments();
  }

  @Test
  void reportsAChecksyleViolationThroughTheRealCliEntryPoint(@TempDir final Path tempDir)
      throws Exception {
    final Path report = tempDir.resolve("checkstyle-report.xml");
    Files.writeString(
        report,
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<checkstyle version=\"8.0\">\n"
            + "  <file name=\""
            + LocalGiteaEnv.CHANGED_FILE
            + "\">\n"
            + "    <error line=\""
            + LocalGiteaEnv.CHANGED_LINE
            + "\" severity=\"error\" message=\"no not ok via the CLI!\""
            + " source=\"com.example.Rule\"/>\n"
            + "  </file>\n"
            + "</checkstyle>\n");

    new Runner()
        .main(
            "-url",
            this.env.giteaUrl,
            "-owner",
            this.env.owner,
            "-rs",
            this.env.repo,
            "-prid",
            String.valueOf(this.env.pullRequestIndex),
            "-pat",
            this.env.token,
            "-csfc",
            "true",
            "-ccwasfc",
            "false",
            "-v",
            "CHECKSTYLE",
            tempDir.toString(),
            ".*checkstyle-report\\.xml$",
            "Checkstyle");

    final List<GiteaReview> reviews = this.client.getReviews();
    assertThat(reviews).hasSize(1);
    final List<GiteaReviewComment> reviewComments =
        this.client.getReviewComments(reviews.get(0).getId());
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.get(0).getBody()).contains("no not ok via the CLI!");
    assertThat(reviewComments.get(0).getPath()).isEqualTo(LocalGiteaEnv.CHANGED_FILE);
    assertThat(reviewComments.get(0).getPosition()).isEqualTo(LocalGiteaEnv.CHANGED_LINE);
  }

  private void removeAllComments() {
    for (final GiteaIssueComment comment : this.client.getIssueComments()) {
      this.client.deleteIssueComment(comment.getId());
    }
    for (final GiteaReview review : this.client.getReviews()) {
      this.client.deleteReview(review.getId());
    }
  }
}
