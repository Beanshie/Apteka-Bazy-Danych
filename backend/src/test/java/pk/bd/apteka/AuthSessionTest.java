package pk.bd.apteka;

import static org.junit.jupiter.api.Assertions.*;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class AuthSessionTest {
  @Container static final MariaDBContainer database = new MariaDBContainer("mariadb:11.4");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", database::getJdbcUrl);
    properties.add("spring.datasource.username", database::getUsername);
    properties.add("spring.datasource.password", database::getPassword);
  }

  @LocalServerPort int port;
  @Autowired KlientService customers;
  @Autowired PracownikService employees;
  @Autowired JdbcTemplate jdbc;
  @Autowired ServletWebServerApplicationContext application;
  private final ObjectMapper json = new ObjectMapper();
  private CookieManager cookies;
  private HttpClient browser;
  private Klient customer;
  private Pracownik employee;

  @BeforeEach
  void setup() {
    cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    browser = HttpClient.newBuilder().cookieHandler(cookies).version(HttpClient.Version.HTTP_1_1).build();
    String suffix = UUID.randomUUID().toString();
    customer = customers.create(new KlientCreate("Session", "Customer", suffix + "@example.com", "secret"));
    employee = employees.create(new PracownikCreate().login("s-" + suffix).haslo("secret")
        .imie("Session").nazwisko("Employee"));
  }

  @AfterEach
  void cleanup() {
    if (customer != null) customers.delete(customer.getIdKlienta());
    if (employee != null) employees.delete(employee.getIdPracownika());
    browser.close();
  }

  private HttpResponse<String> send(String method, String path, String body, String csrf) throws Exception {
    var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path));
    if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
    if (body != null) request.header("Content-Type", "application/json");
    return browser.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
        : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
  }

  private String token() throws Exception {
    var response = send("GET", "/auth/csrf", null, null);
    assertEquals(200, response.statusCode());
    assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
    var value = json.readTree(response.body());
    assertEquals("X-CSRF-TOKEN", value.get("headerName").asString());
    return value.get("token").asString();
  }

  private String loginBody(String type, String identifier, String password) {
    return json.writeValueAsString(Map.of("typ_konta", type, "identyfikator", identifier, "haslo", password));
  }

  private HttpResponse<String> login(String type, String identifier, String password) throws Exception {
    return send("POST", "/auth/login", loginBody(type, identifier, password), token());
  }

  private String sessionId() {
    return cookies.getCookieStore().getCookies().stream().filter(cookie -> "JSESSIONID".equals(cookie.getName()))
        .findFirst().orElseThrow().getValue();
  }

  private void noSecrets(HttpResponse<String> response) {
    assertFalse(response.body().contains("haslo"));
    assertFalse(response.body().contains("secret"));
    assertFalse(response.body().contains("$2"));
    assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
  }

  @Test
  void customerLoginRotatesSessionPersistsIdentityAndLogoutInvalidatesCookie() throws Exception {
    assertEquals(401, send("GET", "/auth/me", null, null).statusCode());
    String beforeToken = token();
    String beforeId = sessionId();
    var response = send("POST", "/auth/login",
        loginBody("KLIENT", customer.getEmail(), "secret"), beforeToken);
    assertEquals(200, response.statusCode(), response.body());
    noSecrets(response);
    assertNotEquals(beforeId, sessionId());
    assertEquals(customer.getIdKlienta(), json.readTree(response.body()).get("id").asInt());
    assertEquals("KLIENT", json.readTree(response.body()).get("typ_konta").asString());
    assertTrue(json.readTree(response.body()).get("rola").isNull());
    String authenticatedId = sessionId();
    var me = send("GET", "/auth/me", null, null);
    assertEquals(200, me.statusCode());
    assertEquals(json.readTree(response.body()), json.readTree(me.body()));
    noSecrets(me);
    assertEquals(403, send("POST", "/auth/logout", null, beforeToken).statusCode());
    String currentToken = token();
    var logout = send("POST", "/auth/logout", null, currentToken);
    assertEquals(204, logout.statusCode());
    String expiredCookie = logout.headers().firstValue("Set-Cookie").orElseThrow();
    assertTrue(HttpCookie.parse(expiredCookie).getFirst().hasExpired(), expiredCookie);
    assertTrue(expiredCookie.contains("Path=/api/v1"), expiredCookie);
    assertEquals(401, send("GET", "/auth/me", null, null).statusCode());
    // Even replaying the previous authenticated cookie cannot recover the session.
    try (var isolated = HttpClient.newHttpClient()) {
      var replay = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/me"))
          .header("Cookie", "JSESSIONID=" + authenticatedId).GET().build();
      assertEquals(401, isolated.send(replay, HttpResponse.BodyHandlers.ofString()).statusCode());
    }
    assertEquals(403, send("POST", "/auth/logout", null, currentToken).statusCode());
    assertEquals(204, send("POST", "/auth/logout", null, token()).statusCode());
  }

  @Test
  void employeesCanLoginWithEveryRoleAndMeReloadsAccountDetails() throws Exception {
    for (String role : new String[] {"FARMACEUTA", "KIEROWNIK", "ADMIN"}) {
      jdbc.update("UPDATE Pracownik SET rola=? WHERE id_pracownika=?", role, employee.getIdPracownika());
      var response = login("PRACOWNIK", employee.getLogin(), "secret");
      assertEquals(200, response.statusCode());
      assertEquals(role, json.readTree(response.body()).get("rola").asString());
      assertEquals("PRACOWNIK", json.readTree(response.body()).get("typ_konta").asString());
      noSecrets(response);
    }
    jdbc.update("UPDATE Pracownik SET imie='Updated',rola='FARMACEUTA' WHERE id_pracownika=?",
        employee.getIdPracownika());
    var response = send("GET", "/auth/me", null, null);
    assertEquals(200, response.statusCode());
    JsonNode value = json.readTree(response.body());
    assertEquals("Updated", value.get("imie").asString());
    assertEquals("FARMACEUTA", value.get("rola").asString());
  }

  @Test
  void wrongUnknownInactiveAndWrongAccountTypeHaveSameUnauthorizedResponse() throws Exception {
    var wrong = login("KLIENT", customer.getEmail(), "wrong");
    assertEquals(401, wrong.statusCode());
    for (String[] values : new String[][] {
        {"KLIENT", "missing@example.com", "secret"},
        {"PRACOWNIK", customer.getEmail(), "secret"},
        {"KLIENT", employee.getLogin(), "secret"},
        {"PRACOWNIK", employee.getLogin(), "wrong"},
        {"PRACOWNIK", "missing", "secret"},
        {"KLIENT", "' OR '1'='1", "secret"},
        {"PRACOWNIK", "' OR '1'='1", "secret"}}) {
      var rejected = login(values[0], values[1], values[2]);
      assertEquals(401, rejected.statusCode());
      assertEquals(wrong.body(), rejected.body());
    }
    jdbc.update("UPDATE Pracownik SET aktywny=FALSE WHERE id_pracownika=?", employee.getIdPracownika());
    var inactive = login("PRACOWNIK", employee.getLogin(), "secret");
    assertEquals(401, inactive.statusCode());
    assertEquals(wrong.body(), inactive.body());
    assertEquals(401, send("GET", "/auth/me", null, null).statusCode());
  }

  @Test
  void csrfMustMatchSessionAndHeaderForBothAuthenticationWrites() throws Exception {
    String body = loginBody("KLIENT", customer.getEmail(), "secret");
    assertEquals(403, send("POST", "/auth/login", body, null).statusCode());
    assertEquals(403, send("POST", "/auth/logout", null, null).statusCode());
    String token = token();
    assertEquals(403, send("POST", "/auth/login?_csrf=" + token, body, null).statusCode());
    assertEquals(403, send("POST", "/auth/logout?_csrf=" + token, null, null).statusCode());
    assertEquals(403, send("POST", "/auth/login", body, "invalid").statusCode());
    assertEquals(403, send("POST", "/auth/logout", null, "invalid").statusCode());
    cookies.getCookieStore().removeAll();
    assertEquals(403, send("POST", "/auth/login", body, token).statusCode());
    assertEquals(401, send("GET", "/auth/me", null, null).statusCode());
  }

  @Test
  void malformedLoginBodiesReturnBadRequestWithValidCsrf() throws Exception {
    for (String body : new String[] {"{", "null", "{}",
        loginBody("UNKNOWN", "somebody", "secret"),
        loginBody("KLIENT", " ", "secret"),
        loginBody("PRACOWNIK", "a".repeat(51), "secret"),
        loginBody("KLIENT", customer.getEmail(), ""),
        loginBody("KLIENT", customer.getEmail(), "ą".repeat(37))}) {
      assertEquals(400, send("POST", "/auth/login", body, token()).statusCode(), body);
    }
  }

  @Test
  void deletedCustomersAndDeactivatedOrDeletedEmployeesLoseSessions() throws Exception {
    assertEquals(200, login("KLIENT", customer.getEmail(), "secret").statusCode());
    customers.delete(customer.getIdKlienta());
    assertEquals(401, send("GET", "/auth/me", null, null).statusCode());
    assertEquals(200, login("PRACOWNIK", employee.getLogin(), "secret").statusCode());
    jdbc.update("UPDATE Pracownik SET aktywny=FALSE WHERE id_pracownika=?", employee.getIdPracownika());
    assertEquals(401, send("GET", "/auth/me", null, null).statusCode());
    jdbc.update("UPDATE Pracownik SET aktywny=TRUE WHERE id_pracownika=?", employee.getIdPracownika());
    assertEquals(401, send("GET", "/auth/me", null, null).statusCode());
    assertEquals(200, login("PRACOWNIK", employee.getLogin(), "secret").statusCode());
    employees.delete(employee.getIdPracownika());
    assertEquals(401, send("GET", "/auth/me", null, null).statusCode());
  }

  @Test
  void sessionCookiesHaveBrowserAttributesAndUnknownSessionsReturnUnauthorized() throws Exception {
    var response = send("GET", "/auth/csrf", null, null);
    String cookie = response.headers().firstValue("Set-Cookie").orElseThrow();
    assertTrue(cookie.contains("HttpOnly"), cookie);
    assertTrue(cookie.contains("SameSite=Lax"), cookie);
    assertTrue(cookie.contains("Path=/api/v1"), cookie);
    assertFalse(cookie.toLowerCase(Locale.ROOT).contains("domain="), cookie);
    assertFalse(cookie.contains("Max-Age="), cookie);
    assertFalse(cookie.contains("Secure"), cookie);
    assertEquals(400, send("GET", "/auth/me;jsessionid=" + sessionId(), null, null).statusCode());
    cookies.getCookieStore().removeAll();
    assertEquals(401, send("GET", "/auth/me", null, null).statusCode());
  }

  @Test
  void credentialedCorsAcceptsConfiguredOriginAndRejectsOtherOrigins() throws Exception {
    var uri = URI.create("http://localhost:" + port + "/api/v1/auth/login");
    for (String origin : new String[] {"http://localhost:5173", "https://untrusted.example"}) {
      var request = HttpRequest.newBuilder(uri).header("Origin", origin)
          .header("Access-Control-Request-Method", "POST")
          .header("Access-Control-Request-Headers", "content-type,x-csrf-token")
          .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
      var response = browser.send(request, HttpResponse.BodyHandlers.ofString());
      if (origin.equals("http://localhost:5173")) {
        assertEquals(200, response.statusCode());
        assertEquals(origin, response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertEquals("true", response.headers().firstValue("Access-Control-Allow-Credentials").orElseThrow());
      } else {
        assertEquals(403, response.statusCode());
        assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
      }
    }
  }

  @Test
  void expiredSessionCannotAuthenticateAndDoesNotRedirectToLoginPage() throws Exception {
    assertEquals(200, login("KLIENT", customer.getEmail(), "secret").statusCode());
    var server = (TomcatWebServer) application.getWebServer();
    var context = (org.apache.catalina.Context) server.getTomcat().getHost().findChild("/api/v1");
    var session = context.getManager().findSession(sessionId());
    assertEquals(1800, session.getMaxInactiveInterval());
    // Expire directly rather than waiting for the idle timeout.
    session.expire();
    var response = send("GET", "/auth/me", null, null);
    assertEquals(401, response.statusCode());
    assertTrue(response.headers().firstValue("Location").isEmpty());
    assertEquals(204, send("POST", "/auth/logout", null, token()).statusCode());
  }

  @Test
  void failedLoginPreservesTheExistingAuthenticatedSession() throws Exception {
    assertEquals(200, login("KLIENT", customer.getEmail(), "secret").statusCode());
    String id = sessionId();
    assertEquals(401, login("PRACOWNIK", employee.getLogin(), "wrong").statusCode());
    assertEquals(id, sessionId());
    var response = send("GET", "/auth/me", null, null);
    assertEquals(200, response.statusCode());
    assertEquals("KLIENT", json.readTree(response.body()).get("typ_konta").asString());
  }

  @Test
  void existingCrudRemainsAccessibleWithoutAuthenticationOrCsrf() throws Exception {
    assertEquals(200, send("GET", "/produkty", null, null).statusCode());
    var created = send("POST", "/kategorie", json.writeValueAsString(Map.of(
        "nazwa_kategorii", "session-" + UUID.randomUUID(), "opis", "before")), null);
    assertEquals(201, created.statusCode());
    int id = json.readTree(created.body()).get("id_kategorii").asInt();
    try {
      assertEquals(200, send("PUT", "/kategorie/" + id, json.writeValueAsString(Map.of(
          "nazwa_kategorii", "updated-" + id, "opis", "after")), null).statusCode());
    } finally {
      assertEquals(204, send("DELETE", "/kategorie/" + id, null, null).statusCode());
    }
  }
}
