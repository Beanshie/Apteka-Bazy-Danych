package pk.bd.apteka.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/** Checks every endpoint's local error handling, including read and list operations. */
class ControllerErrorHandlingTest {
  private static final String[] RESOURCES = {
    "AdresKlienta",
    "Dostawca",
    "Kategoria",
    "Klient",
    "Platnosc",
    "Pracownik",
    "Produkt",
    "Recepta",
    "SzczegolZamowienia",
    "Zamowienie"
  };

  private Object controller(String resource, RuntimeException error) throws Exception {
    Class<?> serviceType = Class.forName("pk.bd.apteka.service." + resource + "Service");
    Object service =
        mock(
            serviceType,
            invocation -> {
              throw error;
            });
    return Class.forName("pk.bd.apteka.controller." + resource + "Controller")
        .getConstructor(serviceType)
        .newInstance(service);
  }

  private Method[] endpoints(Object controller) {
    return Arrays.stream(controller.getClass().getDeclaredMethods())
        .filter(method -> method.getReturnType() == ResponseEntity.class)
        .toArray(Method[]::new);
  }

  private Object[] arguments(Method method) throws Exception {
    Class<?>[] types = method.getParameterTypes();
    Object[] values = new Object[types.length];
    for (int i = 0; i < types.length; i++) {
      values[i] =
          types[i] == Integer.class
              ? 1
              : types[i] == String.class ? "test" : types[i].getConstructor().newInstance();
    }
    return values;
  }

  @Test
  void allFiftyEndpointsMapErrorsWithoutExposingDetails() throws Exception {
    RuntimeException[] errors = {
      new DataIntegrityViolationException("private SQL details"),
      new IllegalArgumentException("private argument details"),
      new DataAccessResourceFailureException("private connection details")
    };
    int[] expected = {409, 400, 500};
    for (int i = 0; i < errors.length; i++) {
      int checked = 0;
      for (String resource : RESOURCES) {
        Object controller = controller(resource, errors[i]);
        for (Method method : endpoints(controller)) {
          var response = (ResponseEntity<?>) method.invoke(controller, arguments(method));
          assertEquals(
              expected[i], response.getStatusCode().value(), resource + "." + method.getName());
          assertNull(response.getBody());
          checked++;
        }
      }
      assertEquals(50, checked);
    }
  }

  @Test
  void allOtherEndpointsMapMissingRecordsTo404() throws Exception {
    for (String resource : RESOURCES) {
      if (resource.equals("Klient")) continue;
      Object controller = controller(resource, new java.util.NoSuchElementException("missing"));
      for (Method method : endpoints(controller)) {
        var response = (ResponseEntity<?>) method.invoke(controller, arguments(method));
        assertEquals(404, response.getStatusCode().value(), resource + "." + method.getName());
        assertNull(response.getBody());
      }
    }
  }

  @Test
  void explicitServiceHttpStatusesRemainForSpringToHandle() throws Exception {
    for (HttpStatus status : new HttpStatus[] {HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND}) {
      var error = new ResponseStatusException(status, "Resource validation");
      for (String resource : RESOURCES) {
        Object controller = controller(resource, error);
        for (Method method : endpoints(controller)) {
          var thrown =
              assertThrows(
                  InvocationTargetException.class,
                  () -> method.invoke(controller, arguments(method)));
          assertSame(error, thrown.getCause());
        }
      }
    }
  }
}
