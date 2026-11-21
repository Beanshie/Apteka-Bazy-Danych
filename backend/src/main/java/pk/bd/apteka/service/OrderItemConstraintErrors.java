package pk.bd.apteka.service;

import java.sql.SQLException;
import org.springframework.dao.DataIntegrityViolationException;

/** Translates the business error codes emitted by the V03 item triggers. */
final class OrderItemConstraintErrors {
  private OrderItemConstraintErrors() {}

  static RuntimeException translate(DataIntegrityViolationException error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof SQLException sql) {
        switch (sql.getErrorCode()) {
          case 30001:
            return new IllegalArgumentException("Brak produktu na stanie", error);
          case 30002:
            return new IllegalArgumentException("Zbyt duża suma", error);
          default:
            break;
        }
      }
    }
    return error;
  }

  static IllegalArgumentException translateCreate(DataIntegrityViolationException error) {
    var translated = translate(error);
    return translated instanceof IllegalArgumentException businessError
        ? businessError
        : new IllegalArgumentException("Błąd integralności danych", error);
  }
}
