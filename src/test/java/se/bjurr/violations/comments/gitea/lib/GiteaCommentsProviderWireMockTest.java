package se.bjurr.violations.comments.gitea.lib;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static se.bjurr.violations.comments.gitea.lib.ViolationCommentsToGiteaApi.violationCommentsToGiteaApi;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.lib.ViolationsLogger;

/**
 * Integration tests that replay, via WireMock, real request/response pairs captured from a local
 * Gitea instance (see scripts/gitea-setup.sh and scripts/wiremock-capture/raw). Unlike {@link
 * se.bjurr.violations.comments.gitea.lib.client.GiteaClientTest}, these drive {@link
 * GiteaCommentsProvider} through its public constructor over real HTTP, so {@link
 * se.bjurr.violations.comments.gitea.lib.client.GiteaInvoker}'s URL/header/body construction is
 * covered too. The fixtures under {@code src/test/resources/gitea} are unmodified captures, except
 * {@code list-issue-comments-response.json}, which wraps the single-comment creation response in a
 * JSON array to match the shape of the list endpoint.
 */
class GiteaCommentsProviderWireMockTest {

  private static final String OWNER = "admin";
  private static final String REPO = "violations-demo";
  private static final long PR_INDEX = 1;
  private static final String REPO_PATH = "/api/v1/repos/" + OWNER + "/" + REPO;
  private static final String PR_PATH = REPO_PATH + "/pulls/" + PR_INDEX;
  private static final String ISSUE_PATH = REPO_PATH + "/issues/" + PR_INDEX;

  @RegisterExtension static WireMockExtension wireMock = WireMockExtension.newInstance().build();

  private ViolationsLogger violationsLogger;

  @BeforeEach
  void setUp() {
    this.wireMock.resetAll();
    this.violationsLogger =
        new ViolationsLogger() {
          @Override
          public void log(final Level level, final String string) {}

          @Override
          public void log(final Level level, final String string, final Throwable t) {}
        };
    this.wireMock.stubFor(get(urlPathEqualTo(PR_PATH)).willReturn(okJson(fixture("pr.json"))));
  }

  private static String fixture(final String name) {
    try {
      return Files.readString(Path.of("src/test/resources/gitea", name));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private ViolationCommentsToGiteaApi newApi() {
    return violationCommentsToGiteaApi()
        .withGiteaUrl(this.wireMock.baseUrl())
        .withPersonalAccessToken("test-token")
        .withOwner(OWNER)
        .withRepo(REPO)
        .withPullRequestIndex((int) PR_INDEX)
        .withShouldCommentOnlyChangedFiles(false)
        .withCreateSingleFileComments(false)
        .withCreateCommentWithAllSingleFileComments(false);
  }

  private GiteaCommentsProvider newProvider(final ViolationCommentsToGiteaApi api) {
    return new GiteaCommentsProvider(api, this.violationsLogger);
  }

  private String lastRequestBodyDecoded(final RequestPatternBuilder pattern) {
    final List<LoggedRequest> requests = this.wireMock.findAll(pattern);
    assertThat(requests).isNotEmpty();
    final LoggedRequest last = requests.get(requests.size() - 1);
    return new String(last.getBody(), StandardCharsets.UTF_8);
  }

  @Test
  void getCommentsReturnsGeneralAndReviewCommentsFromTheRealFixtures() {
    this.wireMock.stubFor(
        get(urlPathEqualTo(ISSUE_PATH + "/comments"))
            .willReturn(okJson(fixture("list-issue-comments-response.json"))));
    this.wireMock.stubFor(
        get(urlPathEqualTo(PR_PATH + "/reviews"))
            .willReturn(okJson(fixture("list-reviews-response.json"))));
    this.wireMock.stubFor(
        get(urlPathEqualTo(PR_PATH + "/reviews/1/comments"))
            .willReturn(okJson(fixture("list-review-comments-response.json"))));

    final GiteaCommentsProvider provider =
        this.newProvider(this.newApi().withCreateSingleFileComments(true));

    final List<Comment> comments = provider.getComments();

    assertThat(comments).hasSize(2);
    assertThat(comments.stream().map(Comment::getIdentifier)).containsExactlyInAnyOrder("2", "3");
  }

  @Test
  void getCommentsSkipsReviewsWhenSingleFileCommentsAreDisabled() {
    this.wireMock.stubFor(
        get(urlPathEqualTo(ISSUE_PATH + "/comments"))
            .willReturn(okJson(fixture("list-issue-comments-response.json"))));

    final GiteaCommentsProvider provider =
        this.newProvider(this.newApi().withCreateSingleFileComments(false));

    final List<Comment> comments = provider.getComments();

    assertThat(comments).hasSize(1);
    assertThat(comments.get(0).getIdentifier()).isEqualTo("2");
    this.wireMock.verify(0, getRequestedFor(urlPathEqualTo(PR_PATH + "/reviews")));
  }

  @Test
  void removeCommentsDeletesTheIssueCommentForAGeneralComment() {
    this.wireMock.stubFor(
        delete(urlPathEqualTo(REPO_PATH + "/issues/comments/2")).willReturn(noContent()));

    final GiteaCommentsProvider provider = this.newProvider(this.newApi());
    final Comment comment = new Comment("2", "this is a comment", "PR", List.of("general"));

    provider.removeComments(List.of(comment));

    this.wireMock.verify(deleteRequestedFor(urlPathEqualTo(REPO_PATH + "/issues/comments/2")));
  }

  @Test
  void removeCommentsDeletesTheWholeReviewForAnInlineComment() {
    this.wireMock.stubFor(delete(urlPathEqualTo(PR_PATH + "/reviews/1")).willReturn(noContent()));

    final GiteaCommentsProvider provider = this.newProvider(this.newApi());
    final Comment comment = new Comment("3", "no not ok!", "PR", List.of("review", "1"));

    provider.removeComments(List.of(comment));

    this.wireMock.verify(deleteRequestedFor(urlPathEqualTo(PR_PATH + "/reviews/1")));
  }

  @Test
  void createCommentPostsAGeneralIssueComment() {
    this.wireMock.stubFor(
        post(urlPathEqualTo(ISSUE_PATH + "/comments"))
            .willReturn(okJson(fixture("create-issue-comment-response.json"))));

    final GiteaCommentsProvider provider = this.newProvider(this.newApi());

    provider.createComment("Integration test top-level comment");

    final String body =
        this.lastRequestBodyDecoded(postRequestedFor(urlPathEqualTo(ISSUE_PATH + "/comments")));
    assertThat(body).isEqualTo("{\"body\":\"Integration test top-level comment\"}");
  }

  @Test
  void createSingleFileCommentPostsAOneCommentReviewAnchoredAtTheHeadSha() {
    this.wireMock.stubFor(
        post(urlPathEqualTo(PR_PATH + "/reviews"))
            .willReturn(okJson(fixture("create-review-response.json"))));

    final GiteaCommentsProvider provider = this.newProvider(this.newApi());

    final ChangedFile file = new ChangedFile("src/main/java/com/example/MyClass.java", List.of(""));
    provider.createSingleFileComment(file, 9, "no not ok!");

    final String body =
        this.lastRequestBodyDecoded(postRequestedFor(urlPathEqualTo(PR_PATH + "/reviews")));
    assertThat(body)
        .isEqualTo(
            "{\"commit_id\":\"6b7d81f9c2d53de5643d891bf76fec18376a6e79\",\"event\":\"COMMENT\","
                + "\"comments\":[{\"path\":\"src/main/java/com/example/MyClass.java\","
                + "\"new_position\":9,\"body\":\"no not ok!\"}]}");
  }

  @Test
  void getFilesCombinesTheFilesListingWithThePerFileDiffPatch() {
    this.wireMock.stubFor(
        get(urlPathEqualTo(PR_PATH + "/files")).willReturn(okJson(fixture("pull-files.json"))));
    this.wireMock.stubFor(
        get(urlPathEqualTo(PR_PATH + ".diff"))
            .willReturn(aResponse().withStatus(200).withBody(fixture("pr.diff"))));

    final GiteaCommentsProvider provider = this.newProvider(this.newApi());

    final List<ChangedFile> files = provider.getFiles();

    assertThat(files).hasSize(1);
    final ChangedFile file = files.get(0);
    assertThat(file.getFilename()).isEqualTo("src/main/java/com/example/MyClass.java");
    assertThat(file.getSpecifics().get(0)).contains("@@ -5,9 +5,12 @@");
  }
}
