package se.bjurr.violations.comments.gitea.lib;

import static org.assertj.core.api.Assertions.assertThat;
import static se.bjurr.violations.comments.gitea.lib.ViolationCommentsToGiteaApi.violationCommentsToGiteaApi;
import static se.bjurr.violations.lib.model.Violation.violationBuilder;

import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.bjurr.violations.comments.gitea.lib.client.GiteaClient;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaIssueComment;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaReview;
import se.bjurr.violations.lib.ViolationsLogger;
import se.bjurr.violations.lib.model.SEVERITY;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.reports.Parser;

/**
 * Exercises a real, local Gitea instance (scripts/gitea-start.sh + scripts/gitea-setup.sh) over
 * real HTTP, through the internal Java API. Useful both to manually verify the client still works
 * against a real server, and as the starting point for re-capturing the fixtures under {@code
 * src/test/resources/gitea} (see {@link GiteaCommentsProviderWireMockTest}) if Gitea's API shape
 * ever changes. Skips (doesn't fail) unless that local Gitea is up - see {@link LocalGiteaEnv}. See
 * also {@link se.bjurr.violations.main.GiteaCliIntegrationTest}, which runs the same kind of
 * scenario through the actual command-line entry point instead of this internal API.
 */
class GiteaIntegrationTest {
  private final ViolationsLogger violationsLogger =
      new ViolationsLogger() {
        @Override
        public void log(final Level level, final String string) {
          Logger.getLogger(GiteaIntegrationTest.class.getSimpleName()).info(string);
        }

        @Override
        public void log(final Level level, final String string, final Throwable t) {
          Logger.getLogger(GiteaIntegrationTest.class.getSimpleName()).log(Level.SEVERE, string, t);
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
  void withCreateSingleFileComments() throws Exception {
    final Set<Violation> violations = new TreeSet<>();
    violations.add(
        violationBuilder() //
            .setFile(LocalGiteaEnv.CHANGED_FILE) //
            .setMessage("no not ok!") //
            .setSeverity(SEVERITY.ERROR) //
            .setStartLine(LocalGiteaEnv.CHANGED_LINE) //
            .setReporter("The tool") //
            .setParser(Parser.FINDBUGS) //
            .build());

    violationCommentsToGiteaApi()
        .withPersonalAccessToken(this.env.token)
        .withGiteaUrl(this.env.giteaUrl) //
        .withOwner(this.env.owner) //
        .withRepo(this.env.repo) //
        .withPullRequestIndex(this.env.pullRequestIndex) //
        .withViolations(violations) //
        .withCreateCommentWithAllSingleFileComments(false) //
        .withCreateSingleFileComments(true) //
        .withCommentOnlyChangedContent(false) //
        .withShouldCommentOnlyChangedFiles(true) //
        .withCommentOnlyChangedContentContext(10) //
        .withShouldKeepOldComments(false) //
        .withMaxNumberOfViolations(9999) //
        .withViolationsLogger(this.violationsLogger) //
        .toPullRequest();

    assertThat(this.client.getReviews()).hasSize(1);
  }

  @Test
  void withCreateCommentWithAllSingleFileComments() throws Exception {
    final Set<Violation> violations = new TreeSet<>();
    violations.add(
        violationBuilder() //
            .setFile(LocalGiteaEnv.CHANGED_FILE) //
            .setMessage("no not ok!\nnewline") //
            .setSeverity(SEVERITY.ERROR) //
            .setStartLine(LocalGiteaEnv.CHANGED_LINE) //
            .setReporter("The tool") //
            .setParser(Parser.FINDBUGS) //
            .build());

    violationCommentsToGiteaApi()
        .withPersonalAccessToken(this.env.token)
        .withGiteaUrl(this.env.giteaUrl) //
        .withOwner(this.env.owner) //
        .withRepo(this.env.repo) //
        .withPullRequestIndex(this.env.pullRequestIndex) //
        .withViolations(violations) //
        .withCreateCommentWithAllSingleFileComments(true) //
        .withCreateSingleFileComments(false) //
        .withCommentOnlyChangedContent(false) //
        .withShouldCommentOnlyChangedFiles(false) //
        .withCommentOnlyChangedContentContext(10) //
        .withShouldKeepOldComments(false) //
        .withMaxNumberOfViolations(9999) //
        .withViolationsLogger(this.violationsLogger) //
        .toPullRequest();

    assertThat(this.client.getIssueComments()).hasSize(1);
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
