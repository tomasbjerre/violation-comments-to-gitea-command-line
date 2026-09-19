package se.bjurr.violations.comments.gitea.lib.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.logging.Level.INFO;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Base64;
import se.bjurr.violations.lib.ViolationsLogger;

public class GiteaInvoker {
  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  public enum Method {
    DELETE,
    GET,
    POST
  }

  public String invokeUrl(
      final ViolationsLogger violationsLogger,
      final String url,
      final Method method,
      final String postContent,
      final String personalAccessToken,
      final ProxyConfig proxyConfig) {

    return this.doInvokeUrl(
        violationsLogger, url, method, postContent, "token " + personalAccessToken, proxyConfig);
  }

  public String invokeUrl(
      final ViolationsLogger violationsLogger,
      final String url,
      final Method method,
      final String postContent,
      final String giteaUser,
      final String giteaPassword,
      final ProxyConfig proxyConfig) {

    final String userAndPass = giteaUser + ":" + giteaPassword;
    final String authString = Base64.getEncoder().encodeToString(userAndPass.getBytes(UTF_8));

    return this.doInvokeUrl(
        violationsLogger, url, method, postContent, "Basic " + authString, proxyConfig);
  }

  private String doInvokeUrl(
      final ViolationsLogger violationsLogger,
      final String url,
      final Method method,
      final String postContent,
      final String authorizationValue,
      final ProxyConfig proxyConfig) {
    try {
      final HttpClient.Builder clientBuilder =
          HttpClient.newBuilder()
              .connectTimeout(TIMEOUT)
              .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
      proxyConfig.addTo(clientBuilder);
      final HttpClient httpClient = clientBuilder.build();

      final HttpRequest.Builder requestBuilder =
          HttpRequest.newBuilder(URI.create(url))
              .timeout(TIMEOUT)
              .header("Authorization", authorizationValue)
              .header("Content-Type", "application/json")
              .header("Accept", "application/json");

      switch (method) {
        case DELETE:
          requestBuilder.DELETE();
          break;
        case GET:
          requestBuilder.GET();
          break;
        case POST:
          final String body = postContent == null ? "" : postContent;
          requestBuilder.POST(BodyPublishers.ofString(body, UTF_8));
          break;
        default:
          throw new IllegalArgumentException(
              "Unsupported http method:\n" + url + "\n" + method + "\n" + postContent);
      }

      final HttpResponse<String> response =
          httpClient.send(requestBuilder.build(), BodyHandlers.ofString(UTF_8));

      final int statusCode = response.statusCode();
      final boolean wasNotOk = statusCode < 200 || statusCode >= 300;
      if (wasNotOk) {
        violationsLogger.log(
            INFO, method + " " + url + " " + statusCode + "\nSent:\n" + postContent);
        violationsLogger.log(INFO, "Response:\n" + response.body());
      } else {
        violationsLogger.log(INFO, method + " " + url + " " + statusCode);
      }
      return response.body();
    } catch (final Exception e) {
      throw new RuntimeException("Error calling:\n" + url + "\n" + method + "\n" + postContent, e);
    }
  }
}
