package se.bjurr.violations.comments.gitea.lib.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.bjurr.violations.lib.util.PatchParserUtil;

/**
 * Unit tests of {@link GiteaClient}'s pure helpers, using {@code pr.diff} - a real, unmodified
 * multi-file diff captured from a local Gitea instance (see scripts/gitea-setup.sh).
 */
class GiteaClientTest {

  private static String fixture(final String name) {
    try {
      return Files.readString(Path.of("src/test/resources/gitea", name));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  void splitPerFilePatchesFindsTheOneChangedFileByItsCurrentPath() {
    final Map<String, String> patches = GiteaClient.splitPerFilePatches(fixture("pr.diff"));

    assertThat(patches).containsOnlyKeys("src/main/java/com/example/MyClass.java");
  }

  @Test
  void splitPerFilePatchesProducesAPatchUsablePatchParserUtilCanMapLinesFrom() {
    final Map<String, String> patches = GiteaClient.splitPerFilePatches(fixture("pr.diff"));
    final String patch = patches.get("src/main/java/com/example/MyClass.java");

    final PatchParserUtil parsed = new PatchParserUtil(patch);

    // Line 8 is the added "System.out.println("done");" - a pure addition, no old line.
    assertThat(parsed.isLineInDiff(8)).isTrue();
    assertThat(parsed.findOldLine(8)).isEmpty();
    // Line 6 is unchanged context, present in both old and new.
    assertThat(parsed.isLineInDiff(6)).isTrue();
    assertThat(parsed.findOldLine(6)).isPresent();
    // Line 100 isn't part of the diff at all.
    assertThat(parsed.isLineInDiff(100)).isFalse();
  }

  @Test
  void splitPerFilePatchesOfEmptyDiffIsEmpty() {
    assertThat(GiteaClient.splitPerFilePatches("")).isEmpty();
    assertThat(GiteaClient.splitPerFilePatches(null)).isEmpty();
  }

  @Test
  void safeJsonEscapesBackslashesAndNewlinesAndStripsQuotes() {
    final GiteaClient client =
        new GiteaClient(
            null,
            "http://localhost",
            "owner",
            "repo",
            1L,
            null,
            null,
            "token",
            null,
            null,
            null,
            null);

    assertThat(client.safeJson("no not ok!\nnewline")).isEqualTo("no not ok!\\nnewline");
    assertThat(client.safeJson("say \"hi\"")).isEqualTo("say hi");
    assertThat(client.safeJson("back\\slash")).isEqualTo("back\\\\slash");
  }
}
