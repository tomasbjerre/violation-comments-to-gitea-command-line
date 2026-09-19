package se.bjurr.violations.comments.gitea.lib;

import static java.util.Optional.ofNullable;
import static se.bjurr.violations.comments.lib.CommentsCreator.createComments;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import se.bjurr.violations.comments.lib.CommentsProvider;
import se.bjurr.violations.lib.ViolationsLogger;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.util.Utils;

public class ViolationCommentsToGiteaApi {

  private static final String DEFAULT_VIOLATION_TEMPLATE_MUSTACH =
      "/default-violation-template-gitea.mustach";

  private Set<Violation> violations;
  private boolean createCommentWithAllSingleFileComments;
  private boolean createSingleFileComments = true;
  private boolean commentOnlyChangedContent;
  private int commentOnlyChangedContentContext;
  private String giteaUrl;
  private String owner;
  private String repo;
  private Integer pullRequestIndex;
  private String username;
  private String password;
  private String personalAccessToken;
  private boolean shouldKeepOldComments;
  private String commentTemplate;
  private ViolationsLogger violationsLogger = new ViolationsLoggerJavaLogger();
  private String proxyHostNameOrIp;
  private Integer proxyHostPort;
  private String proxyUser;
  private String proxyPassword;
  private Integer maxNumberOfViolations;
  private boolean shouldCommentOnlyChangedFiles = true;

  private static class ViolationsLoggerJavaLogger implements ViolationsLogger {
    @Override
    public void log(final Level level, final String string) {
      Logger.getLogger(ViolationsLogger.class.getSimpleName()).log(level, string);
    }

    @Override
    public void log(final Level level, final String string, final Throwable t) {
      Logger.getLogger(ViolationsLogger.class.getSimpleName()).log(level, string, t);
    }
  }

  public static ViolationCommentsToGiteaApi violationCommentsToGiteaApi() {
    return new ViolationCommentsToGiteaApi();
  }

  ViolationCommentsToGiteaApi() {}

  public void toPullRequest() throws Exception {
    if (Utils.isNullOrEmpty(this.commentTemplate)) {
      this.commentTemplate = this.getDefaultTemplate();
    }
    final CommentsProvider commentsProvider =
        new GiteaCommentsProvider(this, this.violationsLogger);
    createComments(this.violationsLogger, this.violations, commentsProvider);
  }

  private String getDefaultTemplate() {
    try (InputStream inputStream =
        ViolationCommentsToGiteaApi.class.getResourceAsStream(DEFAULT_VIOLATION_TEMPLATE_MUSTACH)) {
      if (inputStream == null) {
        throw new RuntimeException("Did not find " + DEFAULT_VIOLATION_TEMPLATE_MUSTACH);
      }
      try (InputStreamReader is = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
          BufferedReader br = new BufferedReader(is)) {
        return br.lines().collect(Collectors.joining("\n"));
      }
    } catch (final Throwable t) {
      throw new RuntimeException(t.getMessage(), t);
    }
  }

  public ViolationCommentsToGiteaApi withViolations(final Set<Violation> violations) {
    this.violations = violations;
    return this;
  }

  public Set<Violation> getViolations() {
    return this.violations;
  }

  public ViolationCommentsToGiteaApi withGiteaUrl(final String giteaUrl) {
    this.giteaUrl = this.emptyToNull(giteaUrl);
    return this;
  }

  public String getGiteaUrl() {
    return this.giteaUrl;
  }

  public ViolationCommentsToGiteaApi withOwner(final String owner) {
    this.owner = this.emptyToNull(owner);
    return this;
  }

  public String getOwner() {
    return this.owner;
  }

  public ViolationCommentsToGiteaApi withRepo(final String repo) {
    this.repo = this.emptyToNull(repo);
    return this;
  }

  public String getRepo() {
    return this.repo;
  }

  public ViolationCommentsToGiteaApi withPullRequestIndex(final Integer pullRequestIndex) {
    this.pullRequestIndex = pullRequestIndex;
    return this;
  }

  public Integer getPullRequestIndex() {
    return this.pullRequestIndex;
  }

  public ViolationCommentsToGiteaApi withUsername(final String username) {
    this.username = this.emptyToNull(username);
    return this;
  }

  public String getUsername() {
    return this.username;
  }

  public ViolationCommentsToGiteaApi withPassword(final String password) {
    this.password = this.emptyToNull(password);
    return this;
  }

  public String getPassword() {
    return this.password;
  }

  public ViolationCommentsToGiteaApi withPersonalAccessToken(final String personalAccessToken) {
    this.personalAccessToken = this.emptyToNull(personalAccessToken);
    return this;
  }

  public String getPersonalAccessToken() {
    return this.personalAccessToken;
  }

  public ViolationCommentsToGiteaApi withProxyHostNameOrIp(final String proxyHostNameOrIp) {
    this.proxyHostNameOrIp = this.emptyToNull(proxyHostNameOrIp);
    return this;
  }

  public String getProxyHostNameOrIp() {
    return this.proxyHostNameOrIp;
  }

  public ViolationCommentsToGiteaApi withProxyHostPort(final Integer proxyHostPort) {
    this.proxyHostPort = proxyHostPort;
    return this;
  }

  public Integer getProxyHostPort() {
    return this.proxyHostPort;
  }

  public ViolationCommentsToGiteaApi withProxyUser(final String proxyUser) {
    this.proxyUser = this.emptyToNull(proxyUser);
    return this;
  }

  public String getProxyUser() {
    return this.proxyUser;
  }

  public ViolationCommentsToGiteaApi withProxyPassword(final String proxyPassword) {
    this.proxyPassword = this.emptyToNull(proxyPassword);
    return this;
  }

  public String getProxyPassword() {
    return this.proxyPassword;
  }

  public ViolationCommentsToGiteaApi withCreateCommentWithAllSingleFileComments(
      final boolean createCommentWithAllSingleFileComments) {
    this.createCommentWithAllSingleFileComments = createCommentWithAllSingleFileComments;
    return this;
  }

  public boolean getCreateCommentWithAllSingleFileComments() {
    return this.createCommentWithAllSingleFileComments;
  }

  public ViolationCommentsToGiteaApi withCreateSingleFileComments(
      final boolean createSingleFileComments) {
    this.createSingleFileComments = createSingleFileComments;
    return this;
  }

  public boolean getCreateSingleFileComments() {
    return this.createSingleFileComments;
  }

  public ViolationCommentsToGiteaApi withCommentOnlyChangedContent(
      final boolean commentOnlyChangedContent) {
    this.commentOnlyChangedContent = commentOnlyChangedContent;
    return this;
  }

  public boolean getCommentOnlyChangedContent() {
    return this.commentOnlyChangedContent;
  }

  public ViolationCommentsToGiteaApi withCommentOnlyChangedContentContext(
      final int commentOnlyChangedContentContext) {
    this.commentOnlyChangedContentContext = commentOnlyChangedContentContext;
    return this;
  }

  public int getCommentOnlyChangedContentContext() {
    return this.commentOnlyChangedContentContext;
  }

  public ViolationCommentsToGiteaApi withShouldCommentOnlyChangedFiles(
      final boolean shouldCommentOnlyChangedFiles) {
    this.shouldCommentOnlyChangedFiles = shouldCommentOnlyChangedFiles;
    return this;
  }

  public boolean getShouldCommentOnlyChangedFiles() {
    return this.shouldCommentOnlyChangedFiles;
  }

  public ViolationCommentsToGiteaApi withShouldKeepOldComments(
      final boolean shouldKeepOldComments) {
    this.shouldKeepOldComments = shouldKeepOldComments;
    return this;
  }

  public boolean getShouldKeepOldComments() {
    return this.shouldKeepOldComments;
  }

  public ViolationCommentsToGiteaApi withCommentTemplate(final String commentTemplate) {
    this.commentTemplate = commentTemplate;
    return this;
  }

  public Optional<String> findCommentTemplate() {
    return ofNullable(this.commentTemplate);
  }

  public ViolationCommentsToGiteaApi withMaxNumberOfViolations(
      final Integer maxNumberOfViolations) {
    this.maxNumberOfViolations = maxNumberOfViolations;
    return this;
  }

  public Integer getMaxNumberOfViolations() {
    return this.maxNumberOfViolations;
  }

  public ViolationCommentsToGiteaApi withViolationsLogger(final ViolationsLogger violationsLogger) {
    this.violationsLogger = violationsLogger;
    return this;
  }

  public ViolationsLogger getViolationsLogger() {
    return this.violationsLogger;
  }

  private String emptyToNull(final String str) {
    if (str == null) {
      return null;
    }
    if (str.trim().isEmpty()) {
      return null;
    }
    return str.trim();
  }
}
