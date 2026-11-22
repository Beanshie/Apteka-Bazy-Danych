package pk.bd.apteka;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Verifies the actual Spring wiring and all generated routes without a database connection. */
@SpringBootTest(
    properties = {
      "spring.flyway.enabled=false",
      "spring.datasource.hikari.initialization-fail-timeout=-1"
    })
class ApiContextTest {
  @Autowired RequestMappingHandlerMapping mappings;

  @Test
  void allFiftyResourceOperationsAreRegistered() {
    long operations =
        mappings.getHandlerMethods().values().stream()
            .filter(
                method -> method.getBeanType().getPackageName().equals("pk.bd.apteka.controller"))
            .count();
    assertEquals(50, operations);
  }
}
