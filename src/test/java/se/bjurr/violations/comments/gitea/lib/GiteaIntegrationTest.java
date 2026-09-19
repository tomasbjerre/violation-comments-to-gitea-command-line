package se.bjurr.violations.comments.gitea.lib;

import static org.assertj.core.api.Assertions.assertThat;
import static se.bjurr.violations.comments.gitea.lib.ViolationCommentsToGiteaApi.violationCommentsToGiteaApi;
import static se.bjurr.violations.lib.model.Violation.violationBuilder;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.bjurr.violations.comments.gitea.lib.client.GiteaClient;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaIssueComment;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaReview;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaReviewComment;
import se.bjurr.violations.lib.ViolationsLogger;
import se.bjurr.violations.lib.model.SEVERITY;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.reports.Parser;

/**
 * Exercises a real, local Gitea instance (scripts/gitea-start.sh + scripts/gitea-setup.sh) over
 * real HTTP, through the internal Java API - covering every scenario the CLI itself can be
 * configured for (see {@link se.bjurr.violations.main.Runner}), the same way {@link
 * GiteaCommentsProviderWireMockTest} covers them against WireMock. Useful both to manually verify
 * the client still works against a real server, and as the starting point for re-capturing the
 * fixtures under {@code src/test/resources/gitea} if Gitea's API shape ever changes. Skips (doesn't
 * fail) unless that local Gitea is up - see {@link LocalGiteaEnv}. See also {@link
 * se.bjurr.violations.main.GiteaCliIntegrationTest}, which runs a scenario through the actual
 * command-line entry point instead of this internal API.
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

  private Violation violation(final int line, final String message) {
    return violationBuilder()
        .setFile(LocalGiteaEnv.CHANGED_FILE)
        .setMessage(message)
        .setSeverity(SEVERITY.ERROR)
        .setStartLine(line)
        .setReporter("The tool")
        .setParser(Parser.FINDBUGS)
        .build();
  }

  /**
   * Reports {@code violations}, with sensible defaults a test can override via {@code customize}.
   */
  private void report(
      final Consumer<ViolationCommentsToGiteaApi> customize, final Set<Violation> violations)
      throws Exception {
    final ViolationCommentsToGiteaApi api =
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
            .withViolationsLogger(this.violationsLogger);
    customize.accept(api);
    api.toPullRequest();
  }

  @Test
  void withCreateSingleFileComments() throws Exception {
    final Set<Violation> violations =
        new TreeSet<>(Set.of(this.violation(LocalGiteaEnv.CHANGED_LINE, "no not ok!")));

    this.report(api -> {}, violations);

    assertThat(this.client.getReviews()).hasSize(1);
  }

  @Test
  void withCreateCommentWithAllSingleFileComments() throws Exception {
    final Set<Violation> violations =
        new TreeSet<>(Set.of(this.violation(LocalGiteaEnv.CHANGED_LINE, "no not ok!\nnewline")));

    this.report(
        api ->
            api.withCreateSingleFileComments(false)
                .withCreateCommentWithAllSingleFileComments(true)
                .withShouldCommentOnlyChangedFiles(false),
        violations);

    assertThat(this.client.getIssueComments()).hasSize(1);
  }

  @Test
  void withBothCommentModesEnabledCreatesBothAnAccumulatedAndASingleFileComment() throws Exception {
    final Set<Violation> violations =
        new TreeSet<>(Set.of(this.violation(LocalGiteaEnv.CHANGED_LINE, "both modes")));

    this.report(api -> api.withCreateCommentWithAllSingleFileComments(true), violations);

    assertThat(this.client.getIssueComments()).hasSize(1);
    assertThat(this.client.getReviews()).hasSize(1);
  }

  @Test
  void withCommentOnlyChangedContentTrueOnlyCommentsLinesThatAreActuallyInTheDiff()
      throws Exception {
    final Set<Violation> violations =
        new TreeSet<>(
            Set.of(
                this.violation(LocalGiteaEnv.CHANGED_LINE, "on the added line"),
                this.violation(1, "on an untouched line")));

    this.report(
        api -> api.withCommentOnlyChangedContent(true).withCommentOnlyChangedContentContext(0),
        violations);

    final List<GiteaReview> reviews = this.client.getReviews();
    assertThat(reviews).hasSize(1);
    final List<GiteaReviewComment> comments = this.client.getReviewComments(reviews.get(0).getId());
    assertThat(comments).hasSize(1);
    assertThat(comments.get(0).getPosition()).isEqualTo(LocalGiteaEnv.CHANGED_LINE);
  }

  @Test
  void withMaxNumberOfViolationsLimitsCreatedComments() throws Exception {
    final Set<Violation> violations =
        new TreeSet<>(
            Set.of(
                this.violation(LocalGiteaEnv.CHANGED_LINE, "first"),
                this.violation(LocalGiteaEnv.OTHER_CHANGED_LINE, "second")));

    this.report(api -> api.withMaxNumberOfViolations(1), violations);

    assertThat(this.client.getReviews()).hasSize(1);
  }

  @Test
  void withShouldKeepOldCommentsFalseRemovesCommentsForViolationsNoLongerReported()
      throws Exception {
    this.report(
        api -> {}, new TreeSet<>(Set.of(this.violation(LocalGiteaEnv.CHANGED_LINE, "first run"))));
    assertThat(this.client.getReviews()).hasSize(1);

    this.report(
        api -> api.withShouldKeepOldComments(false),
        new TreeSet<>(Set.of(this.violation(LocalGiteaEnv.OTHER_CHANGED_LINE, "second run"))));

    final List<GiteaReview> reviews = this.client.getReviews();
    assertThat(reviews).hasSize(1);
    final List<GiteaReviewComment> comments = this.client.getReviewComments(reviews.get(0).getId());
    assertThat(comments.get(0).getBody()).contains("second run");
  }

  @Test
  void withShouldKeepOldCommentsTrueKeepsCommentsForViolationsNoLongerReported() throws Exception {
    this.report(
        api -> {}, new TreeSet<>(Set.of(this.violation(LocalGiteaEnv.CHANGED_LINE, "first run"))));
    assertThat(this.client.getReviews()).hasSize(1);

    this.report(
        api -> api.withShouldKeepOldComments(true),
        new TreeSet<>(Set.of(this.violation(LocalGiteaEnv.OTHER_CHANGED_LINE, "second run"))));

    assertThat(this.client.getReviews()).hasSize(2);
  }

  @Test
  void withCommentTemplateRendersCustomContent() throws Exception {
    final Set<Violation> violations =
        new TreeSet<>(Set.of(this.violation(LocalGiteaEnv.CHANGED_LINE, "boom")));

    this.report(api -> api.withCommentTemplate("CUSTOM: {{violation.message}}"), violations);

    final List<GiteaReview> reviews = this.client.getReviews();
    final List<GiteaReviewComment> comments = this.client.getReviewComments(reviews.get(0).getId());
    assertThat(comments.get(0).getBody()).contains("CUSTOM: boom");
  }

  @Test
  void withUsernameAndPasswordInsteadOfAToken() throws Exception {
    final Set<Violation> violations =
        new TreeSet<>(Set.of(this.violation(LocalGiteaEnv.CHANGED_LINE, "basic auth")));

    violationCommentsToGiteaApi()
        .withUsername("admin")
        .withPassword(LocalGiteaEnv.ADMIN_PASSWORD)
        .withGiteaUrl(this.env.giteaUrl) //
        .withOwner(this.env.owner) //
        .withRepo(this.env.repo) //
        .withPullRequestIndex(this.env.pullRequestIndex) //
        .withViolations(violations) //
        .withCreateSingleFileComments(true) //
        .withCommentOnlyChangedContent(false) //
        .withShouldCommentOnlyChangedFiles(true) //
        .withViolationsLogger(this.violationsLogger) //
        .toPullRequest();

    assertThat(this.client.getReviews()).hasSize(1);
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
