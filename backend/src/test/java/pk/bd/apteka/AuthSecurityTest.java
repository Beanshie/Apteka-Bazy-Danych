package pk.bd.apteka;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pk.bd.apteka.dto.AuthUser;
import pk.bd.apteka.dto.TypKonta;
import pk.bd.apteka.service.AuthService;
import tools.jackson.databind.ObjectMapper;

/** Exercises browser cookie configuration and failure handling without requiring Docker. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.flyway.enabled=false", "spring.datasource.hikari.initialization-fail-timeout=-1",
    "server.servlet.session.cookie.secure=true", "server.servlet.session.timeout=5m"})
class AuthSecurityTest {
  @LocalServerPort int port;
  @MockitoBean AuthService service;
  @Autowired ServletWebServerApplicationContext application;
  private final ObjectMapper json = new ObjectMapper();

  private HttpResponse<String> send(HttpClient client, String method, String path,
      String cookie, String token, String body) throws Exception {
    var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path));
    if (cookie != null) request.header("Cookie", cookie);
    if (token != null) request.header("X-CSRF-TOKEN", token);
    if (body != null) request.header("Content-Type", "application/json");
    return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
        : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void secureCookieSettingAppliesToCreationAndDeletionAndTimeoutIsConfigurable() throws Exception {
    try (var client = HttpClient.newHttpClient()) {
      var csrf = send(client, "GET", "/auth/csrf", null, null, null);
      assertEquals(200, csrf.statusCode());
      String cookie = csrf.headers().firstValue("Set-Cookie").orElseThrow();
      assertTrue(cookie.contains("Secure"), cookie);
      assertTrue(cookie.contains("HttpOnly"), cookie);
      assertTrue(cookie.contains("SameSite=Lax"), cookie);
      String pair = cookie.split(";", 2)[0];
      var context = (org.apache.catalina.Context) ((TomcatWebServer) application.getWebServer())
          .getTomcat().getHost().findChild("/api/v1");
      assertEquals(300, context.getManager().findSession(pair.substring("JSESSIONID=".length()))
          .getMaxInactiveInterval());
      String token = json.readTree(csrf.body()).get("token").asString();
      var response = send(client, "POST", "/auth/logout", pair, token, null);
      assertEquals(204, response.statusCode());
      assertTrue(response.headers().firstValue("Set-Cookie").orElseThrow().contains("Secure"));
      verifyNoInteractions(service);
    }
  }

  @Test
  void databaseFailuresReturnServerErrorsWithoutExposingDetailsOrLosingSession() throws Exception {
    try (var client = HttpClient.newHttpClient()) {
      var csrf = send(client, "GET", "/auth/csrf", null, null, null);
      String cookie = csrf.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
      String token = json.readTree(csrf.body()).get("token").asString();
      String body = "{\"typ_konta\":\"KLIENT\",\"identyfikator\":\"session@example.com\",\"haslo\":\"secret\"}";
      when(service.authenticate(any())).thenThrow(new DataAccessResourceFailureException("SQL private detail"));
      var rejected = send(client, "POST", "/auth/login", cookie, token, body);
      assertEquals(500, rejected.statusCode());
      assertEquals("", rejected.body());
      assertEquals(401, send(client, "GET", "/auth/me", cookie, null, null).statusCode());
      var user = new AuthUser().id(7).typKonta(TypKonta.KLIENT).identyfikator("session@example.com")
          .imie("Session").nazwisko("Customer").rola(null);
      doReturn(user).when(service).authenticate(any());
      var loggedIn = send(client, "POST", "/auth/login", cookie, token, body);
      assertEquals(200, loggedIn.statusCode());
      cookie = loggedIn.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
      when(service.currentUser(any())).thenThrow(new DataAccessResourceFailureException("SQL private detail"));
      var failedMe = send(client, "GET", "/auth/me", cookie, null, null);
      assertEquals(500, failedMe.statusCode());
      assertEquals("", failedMe.body());
      doReturn(user).when(service).currentUser(any());
      assertEquals(200, send(client, "GET", "/auth/me", cookie, null, null).statusCode());
      var context = (org.apache.catalina.Context) ((TomcatWebServer) application.getWebServer())
          .getTomcat().getHost().findChild("/api/v1");
      var session = context.getManager().findSession(cookie.substring("JSESSIONID=".length()));
      var security = (org.springframework.security.core.context.SecurityContext) session.getSession()
          .getAttribute("SPRING_SECURITY_CONTEXT");
      assertNull(security.getAuthentication().getCredentials());
      assertInstanceOf(pk.bd.apteka.security.SessionIdentity.class, security.getAuthentication().getPrincipal());
    }
  }
}
