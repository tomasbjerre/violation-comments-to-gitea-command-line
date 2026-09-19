package se.bjurr.violations.comments.gitea.lib.client;

import static se.bjurr.violations.lib.util.Utils.isNullOrEmpty;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;

public class ProxyConfig {

  private final String proxyHostNameOrIp;
  private final Integer proxyHostPort;
  private final String proxyUser;
  private final String proxyPassword;

  public ProxyConfig(
      final String proxyHostNameOrIp,
      final Integer proxyHostPort,
      final String proxyUser,
      final String proxyPassword) {
    this.proxyHostNameOrIp = proxyHostNameOrIp;
    this.proxyHostPort = proxyHostPort;
    this.proxyUser = proxyUser;
    this.proxyPassword = proxyPassword;
  }

  public HttpClient.Builder addTo(final HttpClient.Builder builder) {
    if (isNullOrEmpty(this.proxyHostNameOrIp)) {
      return builder;
    }
    builder.proxy(
        ProxySelector.of(new InetSocketAddress(this.proxyHostNameOrIp, this.proxyHostPort)));

    if (!isNullOrEmpty(this.proxyUser)) {
      builder.authenticator(
          new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(
                  ProxyConfig.this.proxyUser, ProxyConfig.this.proxyPassword.toCharArray());
            }
          });
    }
    return builder;
  }
}
