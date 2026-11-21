package pk.bd.apteka.service;

import java.sql.SQLException;
import java.util.NoSuchElementException;
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
          case 30003:
            return new IllegalArgumentException("Recepta należy do innego klienta", error);
          case 30004:
            return new IllegalArgumentException("Recepta nie jest ważna", error);
          case 30005:
            return new IllegalArgumentException("Wymagana recepta", error);
          case 30006:
            return new NoSuchElementException("Nie znaleziono rekordu", error);
          default:
            break;
        }
      }
    }
    return error;
  }

  static RuntimeException translateCreate(DataIntegrityViolationException error) {
    var translated = translate(error);
    return translated == error
        ? new IllegalArgumentException("Błąd integralności danych", error)
        : translated;
  }
}
