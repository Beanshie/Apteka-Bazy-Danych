package pk.bd.apteka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;

/** Exercises documentation generation and UI resources without requiring a database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.flyway.enabled=false", "spring.datasource.hikari.initialization-fail-timeout=-1"})
class SpringdocTest {
  @LocalServerPort int port;
  private final ObjectMapper json = new ObjectMapper();

  private HttpResponse<String> get(HttpClient client, String path) throws Exception {
    return client.send(HttpRequest.newBuilder(
        URI.create("http://localhost:" + port + "/api/v1" + path)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void openApiJsonIncludesResourceAndAuthenticationOperations() throws Exception {
    try (var client = HttpClient.newHttpClient()) {
      var response = get(client, "/v3/api-docs");
      assertEquals(200, response.statusCode(), response.body());
      var document = json.readTree(response.body());
      assertTrue(document.path("openapi").asString().startsWith("3."));
      var paths = document.path("paths");
      assertTrue(paths.path("/produkty").has("get"));
      assertTrue(paths.path("/auth/login").has("post"));
      assertTrue(paths.path("/auth/me").has("get"));
    }
  }

  @Test
  void swaggerUiAndConfigurationLoadUnderTheContextPath() throws Exception {
    try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
      var page = get(client, "/swagger-ui.html");
      assertEquals(200, page.statusCode());
      assertEquals("/api/v1/swagger-ui/index.html", page.uri().getPath());
      assertTrue(page.body().contains("swagger-ui-bundle.js"));
      assertEquals(200, get(client, "/swagger-ui/swagger-ui-bundle.js").statusCode());

      var config = get(client, "/v3/api-docs/swagger-config");
      assertEquals(200, config.statusCode());
      assertEquals("/api/v1/v3/api-docs", json.readTree(config.body()).path("url").asString());
    }
  }
}
