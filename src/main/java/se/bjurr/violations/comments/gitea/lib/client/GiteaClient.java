package se.bjurr.violations.comments.gitea.lib.client;

import static java.util.logging.Level.INFO;
import static se.bjurr.violations.lib.util.Utils.isNullOrEmpty;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import se.bjurr.violations.comments.gitea.lib.client.GiteaInvoker.Method;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaChangedFile;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaIssueComment;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaPullRequest;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaReview;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaReviewComment;
import se.bjurr.violations.lib.ViolationsLogger;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

public class GiteaClient {
  private static final JsonMapper JSON_MAPPER =
      JsonMapper.builder()
          .changeDefaultVisibility(vc -> vc.withFieldVisibility(Visibility.ANY))
          .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
          .build();

  private static GiteaInvoker giteaInvoker = new GiteaInvoker();

  public static void setGiteaInvoker(final GiteaInvoker giteaInvoker) {
    GiteaClient.giteaInvoker = giteaInvoker;
  }

  private final ViolationsLogger violationsLogger;
  private final String giteaBaseUrl;
  private final String owner;
  private final String repo;
  private final Long pullRequestIndex;
  private final String username;
  private final String password;
  private final String personalAccessToken;
  private final ProxyConfig proxyConfig;

  public GiteaClient(
      final ViolationsLogger violationsLogger,
      final String giteaBaseUrl,
      final String owner,
      final String repo,
      final Long pullRequestIndex,
      final String username,
      final String password,
      final String personalAccessToken,
      final String proxyHostNameOrIp,
      final Integer proxyHostPort,
      final String proxyUser,
      final String proxyPassword) {
    this.violationsLogger = violationsLogger;
    this.giteaBaseUrl =
        giteaBaseUrl.endsWith("/")
            ? giteaBaseUrl.substring(0, giteaBaseUrl.length() - 1)
            : giteaBaseUrl;
    this.owner = owner;
    this.repo = repo;
    this.pullRequestIndex = pullRequestIndex;
    this.username = username;
    this.password = password;
    this.personalAccessToken = personalAccessToken;
    this.proxyConfig = new ProxyConfig(proxyHostNameOrIp, proxyHostPort, proxyUser, proxyPassword);
  }

  private String getRepoBase() {
    return this.giteaBaseUrl + "/api/v1/repos/" + this.owner + "/" + this.repo;
  }

  private String getPullBase() {
    return this.getRepoBase() + "/pulls/" + this.pullRequestIndex;
  }

  private String getIssueBase() {
    return this.getRepoBase() + "/issues/" + this.pullRequestIndex;
  }

  private String doInvokeUrl(final String url, final Method method, final String postContent) {
    if (isNullOrEmpty(this.personalAccessToken)) {
      return giteaInvoker.invokeUrl(
          this.violationsLogger,
          url,
          method,
          postContent,
          this.username,
          this.password,
          this.proxyConfig);
    } else {
      return giteaInvoker.invokeUrl(
          this.violationsLogger,
          url,
          method,
          postContent,
          this.personalAccessToken,
          this.proxyConfig);
    }
  }

  public GiteaPullRequest getPullRequest() {
    final String url = this.getPullBase();
    final String json = this.doInvokeUrl(url, Method.GET, null);
    final GiteaPullRequest pullRequest = this.readValue(json, GiteaPullRequest.class, url);
    if (pullRequest.getHead() == null || pullRequest.getBase() == null) {
      throw new RuntimeException(
          "Unable to read head/base refs for pull request from " + url + "\n\n" + json);
    }
    return pullRequest;
  }

  public List<GiteaChangedFile> getChangedFiles() {
    final String url = this.getPullBase() + "/files";
    final String json = this.doInvokeUrl(url, Method.GET, null);
    return this.readList(json, GiteaChangedFile.class, url);
  }

  /** The whole PR's unified diff, {@code GET .../pulls/{index}.diff}, not yet split per file. */
  public String getPullRequestDiff() {
    return this.doInvokeUrl(this.getPullBase() + ".diff", Method.GET, null);
  }

  /**
   * Each changed file's own patch text, keyed by its current path. See {@link
   * #getPullRequestDiff()}.
   */
  public Map<String, String> getChangedFilePatches() {
    return splitPerFilePatches(this.getPullRequestDiff());
  }

  public GiteaIssueComment createIssueComment(final String body) {
    final String postContent = "{\"body\":\"" + this.safeJson(body) + "\"}";
    final String url = this.getIssueBase() + "/comments";
    final String json = this.doInvokeUrl(url, Method.POST, postContent);
    return this.readValue(json, GiteaIssueComment.class, url);
  }

  public List<GiteaIssueComment> getIssueComments() {
    final String url = this.getIssueBase() + "/comments";
    final String json = this.doInvokeUrl(url, Method.GET, null);
    return this.readList(json, GiteaIssueComment.class, url);
  }

  public void deleteIssueComment(final Long commentId) {
    this.doInvokeUrl(this.getRepoBase() + "/issues/comments/" + commentId, Method.DELETE, null);
  }

  /**
   * Posts a single inline comment on the diff, as its own one-comment review (submitted
   * immediately, {@code event: "COMMENT"}) - see {@link GiteaReview}.
   */
  public GiteaReview createReviewComment(
      final String commitId, final String path, final int newLine, final String body) {
    final int line = newLine <= 0 ? 1 : newLine;
    final String postContent =
        "{\"commit_id\":\""
            + this.safeJson(commitId)
            + "\",\"event\":\"COMMENT\",\"comments\":[{\"path\":\""
            + this.safeJson(path)
            + "\",\"new_position\":"
            + line
            + ",\"body\":\""
            + this.safeJson(body)
            + "\"}]}";
    final String url = this.getPullBase() + "/reviews";
    final String json = this.doInvokeUrl(url, Method.POST, postContent);
    return this.readValue(json, GiteaReview.class, url);
  }

  public List<GiteaReview> getReviews() {
    final String url = this.getPullBase() + "/reviews";
    final String json = this.doInvokeUrl(url, Method.GET, null);
    return this.readList(json, GiteaReview.class, url);
  }

  public List<GiteaReviewComment> getReviewComments(final Long reviewId) {
    final String url = this.getPullBase() + "/reviews/" + reviewId + "/comments";
    final String json = this.doInvokeUrl(url, Method.GET, null);
    return this.readList(json, GiteaReviewComment.class, url);
  }

  public void deleteReview(final Long reviewId) {
    this.doInvokeUrl(this.getPullBase() + "/reviews/" + reviewId, Method.DELETE, null);
  }

  /**
   * Splits a multi-file unified diff (as returned by {@link #getPullRequestDiff()}) into per-file
   * patch text, keyed by the file's current path - suitable for {@link
   * se.bjurr.violations.lib.util.PatchParserUtil}. A file's leading {@code diff --git}/{@code
   * index}/{@code ---}/{@code +++} header lines don't disturb that parser (only {@code @@}, {@code
   * +} and space-prefixed lines do), so each chunk is kept whole rather than trimmed down to just
   * the {@code @@} hunks.
   */
  static Map<String, String> splitPerFilePatches(final String multiFileDiff) {
    final Map<String, String> result = new LinkedHashMap<>();
    if (isNullOrEmpty(multiFileDiff)) {
      return result;
    }
    StringBuilder current = null;
    String currentPath = null;
    for (final String line : multiFileDiff.split("\n", -1)) {
      if (line.startsWith("diff --git ")) {
        if (currentPath != null) {
          result.put(currentPath, current.toString());
        }
        current = new StringBuilder();
        currentPath = null;
      }
      if (current == null) {
        continue;
      }
      current.append(line).append("\n");
      if (line.startsWith("--- ") && currentPath == null) {
        currentPath = pathFromDiffHeaderLine(line, "a/");
      } else if (line.startsWith("+++ ")) {
        final String newPath = pathFromDiffHeaderLine(line, "b/");
        if (newPath != null) {
          currentPath = newPath;
        }
      }
    }
    if (currentPath != null) {
      result.put(currentPath, current.toString());
    }
    return result;
  }

  private static String pathFromDiffHeaderLine(final String line, final String prefix) {
    String path = line.substring(4).trim();
    if (path.equals("/dev/null")) {
      return null;
    }
    if (path.startsWith(prefix)) {
      path = path.substring(prefix.length());
    }
    return path;
  }

  private <T> T readValue(final String json, final Class<T> type, final String url) {
    try {
      return JSON_MAPPER.readValue(json, type);
    } catch (final Exception e) {
      throw new RuntimeException(
          "Unable to parse response from " + url + " as " + type.getSimpleName() + "\n\n" + json,
          e);
    }
  }

  private <T> List<T> readList(final String json, final Class<T> type, final String url) {
    try {
      final List<T> parsed =
          JSON_MAPPER.readValue(
              json, JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, type));
      if (parsed.isEmpty()) {
        this.violationsLogger.log(INFO, "Found no " + type.getSimpleName() + " from " + url);
      }
      return parsed;
    } catch (final Exception e) {
      throw new RuntimeException(
          "Unable to parse response from "
              + url
              + " as a list of "
              + type.getSimpleName()
              + "\n\n"
              + json,
          e);
    }
  }

  String safeJson(final String message) {
    return message
        .replaceAll("\\\\", "\\\\\\\\")
        .replaceAll("\"", "")
        .replaceAll("\n", "\\\\n")
        .replaceAll("\t", "    ");
  }
}
