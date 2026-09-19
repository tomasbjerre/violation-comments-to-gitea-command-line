package se.bjurr.violations.comments.gitea.lib;

import static java.util.logging.Level.SEVERE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import se.bjurr.violations.comments.gitea.lib.client.GiteaClient;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaChangedFile;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaIssueComment;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaPullRequest;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaReview;
import se.bjurr.violations.comments.gitea.lib.client.model.GiteaReviewComment;
import se.bjurr.violations.comments.lib.CommentsProvider;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.lib.ViolationsLogger;
import se.bjurr.violations.lib.util.PatchParserUtil;

public class GiteaCommentsProvider implements CommentsProvider {
  private static final Integer GITEA_MAX_COMMENT_SIZE = 65_536;

  /** {@link Comment#getSpecifics()} marker for a general (top-level, issue) comment. */
  static final String TYPE_GENERAL = "general";

  /**
   * {@link Comment#getSpecifics()} marker for an inline diff comment. Followed by the id of the
   * review it belongs to - Gitea can only delete a whole review, not one comment in it, so that
   * review id is what {@link #removeComments(List)} needs.
   */
  static final String TYPE_REVIEW = "review";

  private final ViolationCommentsToGiteaApi api;
  private final ViolationsLogger violationsLogger;
  private final GiteaClient client;
  private final String headSha;

  public GiteaCommentsProvider(
      final ViolationCommentsToGiteaApi api, final ViolationsLogger violationsLogger) {
    this.api = api;
    this.violationsLogger = violationsLogger;
    this.client =
        new GiteaClient(
            violationsLogger,
            api.getGiteaUrl(),
            api.getOwner(),
            api.getRepo(),
            api.getPullRequestIndex().longValue(),
            api.getUsername(),
            api.getPassword(),
            api.getPersonalAccessToken(),
            api.getProxyHostNameOrIp(),
            api.getProxyHostPort(),
            api.getProxyUser(),
            api.getProxyPassword());
    final GiteaPullRequest pullRequest = this.client.getPullRequest();
    this.headSha = pullRequest.getHead().getSha();
  }

  @Override
  public void createComment(final String comment) {
    this.client.createIssueComment(comment);
  }

  @Override
  public void createSingleFileComment(
      final ChangedFile file, final Integer line, final String comment) {
    this.client.createReviewComment(this.headSha, file.getFilename(), line, comment);
  }

  @Override
  public List<Comment> getComments() {
    final List<Comment> comments = new ArrayList<>();

    for (final GiteaIssueComment issueComment : this.client.getIssueComments()) {
      comments.add(
          new Comment(
              String.valueOf(issueComment.getId()),
              issueComment.getBody(),
              "PR",
              List.of(TYPE_GENERAL)));
    }

    if (this.shouldCreateSingleFileComment()) {
      for (final GiteaReview review : this.client.getReviews()) {
        if (review.getCommentsCount() == null || review.getCommentsCount() == 0) {
          continue;
        }
        for (final GiteaReviewComment reviewComment :
            this.client.getReviewComments(review.getId())) {
          comments.add(
              new Comment(
                  String.valueOf(reviewComment.getId()),
                  reviewComment.getBody(),
                  "PR",
                  List.of(TYPE_REVIEW, String.valueOf(review.getId()))));
        }
      }
    }

    return comments;
  }

  @Override
  public List<ChangedFile> getFiles() {
    final List<ChangedFile> changedFiles = new ArrayList<>();
    final Map<String, String> patches = this.client.getChangedFilePatches();
    for (final GiteaChangedFile file : this.client.getChangedFiles()) {
      final String patch = patches.getOrDefault(file.getFilename(), "");
      changedFiles.add(new ChangedFile(file.getFilename(), List.of(patch)));
    }
    return changedFiles;
  }

  @Override
  public void removeComments(final List<Comment> comments) {
    for (final Comment comment : comments) {
      try {
        final String type = comment.getSpecifics().get(0);
        if (TYPE_GENERAL.equals(type)) {
          this.client.deleteIssueComment(Long.valueOf(comment.getIdentifier()));
        } else {
          final Long reviewId = Long.valueOf(comment.getSpecifics().get(1));
          this.client.deleteReview(reviewId);
        }
      } catch (final Exception e) {
        this.violationsLogger.log(SEVERE, "Could not remove comment " + comment, e);
      }
    }
  }

  @Override
  public boolean shouldComment(final ChangedFile changedFile, final Integer line) {
    if (!this.api.getCommentOnlyChangedContent()) {
      return true;
    }
    final String patchString = changedFile.getSpecifics().get(0);
    final int contextLines = this.api.getCommentOnlyChangedContentContext();
    final PatchParserUtil patch = new PatchParserUtil(patchString);
    return IntStream.rangeClosed(-contextLines, contextLines)
        .anyMatch(i -> patch.isLineInDiff(line + i) && !patch.findOldLine(line + i).isPresent());
  }

  @Override
  public boolean shouldCreateCommentWithAllSingleFileComments() {
    return this.api.getCreateCommentWithAllSingleFileComments();
  }

  @Override
  public boolean shouldCreateSingleFileComment() {
    return this.api.getCreateSingleFileComments();
  }

  @Override
  public boolean shouldKeepOldComments() {
    return this.api.getShouldKeepOldComments();
  }

  @Override
  public boolean shouldCommentOnlyChangedFiles() {
    return this.api.getShouldCommentOnlyChangedFiles();
  }

  @Override
  public Optional<String> findCommentTemplate() {
    return this.api.findCommentTemplate();
  }

  @Override
  public Integer getMaxNumberOfViolations() {
    return this.api.getMaxNumberOfViolations();
  }

  @Override
  public Integer getMaxCommentSize() {
    return GITEA_MAX_COMMENT_SIZE;
  }
}
