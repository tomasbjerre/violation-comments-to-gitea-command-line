package se.bjurr.violations.comments.gitea.lib;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import se.bjurr.violations.comments.gitea.lib.client.GiteaClient;
import se.bjurr.violations.lib.ViolationsLogger;

/**
 * Connection details for the local Gitea instance seeded by scripts/gitea-setup.sh, read from
 * scripts/.env - so a test using this can target a real, live Gitea with no source changes: run
 * scripts/gitea-start.sh && scripts/gitea-setup.sh, then just run the tests normally. {@link
 * #assumeAvailable()} makes such a test skip (rather than fail) when that hasn't been done, so it
 * stays safe to run in CI.
 */
public final class LocalGiteaEnv {
  /** The file scripts/gitea-setup.sh seeds a change into, on the demo pull request. */
  public static final String CHANGED_FILE = "src/main/java/com/example/MyClass.java";

  /** The new-file line number of that change, comments in tests are anchored to. */
  public static final int CHANGED_LINE = 8;

  public final String giteaUrl;
  public final String owner;
  public final String repo;
  public final Integer pullRequestIndex;
  public final String token;

  private LocalGiteaEnv(final Map<String, String> env) {
    this.giteaUrl = env.getOrDefault("GITEA_URL", "http://localhost:3000");
    this.owner = env.getOrDefault("GITEA_OWNER", "admin");
    this.repo = env.getOrDefault("GITEA_REPO", "violations-demo");
    this.pullRequestIndex = Integer.valueOf(env.getOrDefault("GITEA_PR_INDEX", "1"));
    this.token = env.get("GITEA_TOKEN");
  }

  public static LocalGiteaEnv read() {
    return new LocalGiteaEnv(readDotEnv());
  }

  private static Map<String, String> readDotEnv() {
    final Map<String, String> env = new HashMap<>();
    final Path path = Path.of("scripts/.env");
    if (!Files.exists(path)) {
      return env;
    }
    try {
      for (final String line : Files.readAllLines(path)) {
        if (line.isBlank() || line.startsWith("#") || !line.contains("=")) {
          continue;
        }
        final int eq = line.indexOf('=');
        env.put(line.substring(0, eq), line.substring(eq + 1));
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return env;
  }

  public boolean isAvailable() {
    return this.token != null;
  }

  /** Skips (JUnit assumption failure, not a test failure) the calling test if not available. */
  public void assumeAvailable() {
    assumeTrue(
        this.isAvailable(),
        "No scripts/.env found - run scripts/gitea-start.sh && scripts/gitea-setup.sh to run this"
            + " test against a live local Gitea.");
  }

  public GiteaClient newClient(final ViolationsLogger violationsLogger) {
    return new GiteaClient(
        violationsLogger,
        this.giteaUrl,
        this.owner,
        this.repo,
        this.pullRequestIndex.longValue(),
        null,
        null,
        this.token,
        null,
        null,
        null,
        null);
  }
}
