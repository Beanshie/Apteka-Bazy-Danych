package pk.bd.apteka;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mariadb.MariaDBContainer;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SzczegolZamowieniaServiceTest {
  @Container static final MariaDBContainer database = new MariaDBContainer("mariadb:11.4");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", database::getJdbcUrl);
    properties.add("spring.datasource.username", database::getUsername);
    properties.add("spring.datasource.password", database::getPassword);
  }

  @Autowired SzczegolZamowieniaService items;
  @Autowired ZamowienieService orders;
  @Autowired ReceptaService prescriptions;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager transactions;

  private Zamowienie newOrder() {
    return orders.create(
        new ZamowienieCreate(1, 1, List.of(new SzczegolZamowieniaCreate(4))));
  }

  private int stock(int product) {
    return jdbc.queryForObject(
        "SELECT stan_magazynowy FROM Produkt WHERE id_produktu=?", Integer.class, product);
  }

  private record State(List<SzczegolZamowienia> items, BigDecimal total, List<Integer> stocks) {}

  private State state(int order) {
    return new State(
        items.list(order),
        jdbc.queryForObject(
            "SELECT suma_calkowita FROM Zamowienie WHERE id_zamowienia=?", BigDecimal.class, order),
        jdbc.queryForList(
            "SELECT stan_magazynowy FROM Produkt ORDER BY id_produktu", Integer.class));
  }

  private <T extends Throwable> T assertRejected(Class<T> exception, int order, Runnable mutation) {
    var before = state(order);
    var error = assertThrows(exception, mutation::run);
    assertEquals(before, state(order));
    return error;
  }

  private void assertTotal(int order) {
    var expected =
        items.list(order).stream()
            .map(item -> item.getCenaJednostkowa().multiply(BigDecimal.valueOf(item.getIlosc())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertEquals(0, expected.compareTo(state(order).total()));
  }

  private SzczegolZamowienia committedItem(Future<SzczegolZamowienia> result) throws Exception {
    try {
      return result.get(30, TimeUnit.SECONDS);
    } catch (ExecutionException error) {
      assertInstanceOf(ConcurrencyFailureException.class, error.getCause());
      return null;
    }
  }

  @Test
  void deletingFinalItemZerosTotalAndRepeatedDeletionDoesNothing() {
    int initialStock = stock(4);
    var order = newOrder();
    int id = order.getIdZamowienia();
    int line = order.getPozycje().getFirst().getIdSzczegolu();
    try {
      items.delete(line, id);
      assertTrue(items.list(id).isEmpty());
      assertEquals(0, state(id).total().signum());
      assertEquals(initialStock, stock(4));
      var empty = state(id);
      items.delete(line, id);
      assertEquals(empty, state(id));
    } finally {
      orders.delete(id);
    }
    assertTrue(items.list(id).isEmpty());
    assertThrows(NoSuchElementException.class, () -> items.delete(line, id));
  }

  @Test
  void wrongParentCannotReadUpdateOrDeleteAnItem() {
    var first = newOrder();
    var second = newOrder();
    int firstId = first.getIdZamowienia(), secondId = second.getIdZamowienia();
    int line = first.getPozycje().getFirst().getIdSzczegolu();
    try {
      var firstBefore = state(firstId);
      var secondBefore = state(secondId);
      assertThrows(NoSuchElementException.class, () -> items.get(line, secondId));
      assertThrows(
          NoSuchElementException.class,
          () -> items.update(line, new SzczegolZamowieniaUpdate().idProduktu(4).ilosc(2), secondId));
      items.delete(line, secondId);
      assertEquals(firstBefore, state(firstId));
      assertEquals(secondBefore, state(secondId));
    } finally {
      orders.delete(firstId);
      orders.delete(secondId);
    }
  }

  @Test
  void databaseQuantityAndUniquenessFailuresLeaveReservationsUnchanged() {
    var order = newOrder();
    int id = order.getIdZamowienia();
    int line = order.getPozycje().getFirst().getIdSzczegolu();
    try {
      for (int quantity : new int[] {0, -1}) {
        assertRejected(
            IllegalArgumentException.class,
            id,
            () -> items.create(new SzczegolZamowieniaCreate(2).ilosc(quantity), id));
        assertRejected(
            DataIntegrityViolationException.class,
            id,
            () -> items.update(line, new SzczegolZamowieniaUpdate().idProduktu(4).ilosc(quantity), id));
      }
      assertRejected(
          DataIntegrityViolationException.class,
          id,
          () -> items.update(line, new SzczegolZamowieniaUpdate().idProduktu(4), id));
      assertRejected(
          IllegalArgumentException.class,
          id,
          () -> items.create(new SzczegolZamowieniaCreate(4), id));
      var added = items.create(new SzczegolZamowieniaCreate(2).ilosc(null), id);
      assertEquals(1, added.getIlosc());
      assertTrue(added.getIdSzczegolu() > 0);
      assertEquals(added, items.get(added.getIdSzczegolu(), id));
      assertEquals(
          jdbc.queryForObject("SELECT cena FROM Produkt WHERE id_produktu=2", BigDecimal.class),
          added.getCenaJednostkowa());
      assertRejected(
          DataIntegrityViolationException.class,
          id,
          () -> items.update(line, new SzczegolZamowieniaUpdate().idProduktu(2).ilosc(1), id));
      assertTotal(id);
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void unavailableStockAndExcessiveTotalRollBackEveryChange() {
    var order = newOrder();
    int id = order.getIdZamowienia();
    int line = order.getPozycje().getFirst().getIdSzczegolu();
    var originalPrice =
        jdbc.queryForObject("SELECT cena FROM Produkt WHERE id_produktu=1", BigDecimal.class);
    try {
      int tooMany = stock(1) + 1;
      var createStockError = assertRejected(
          IllegalArgumentException.class,
          id,
          () -> items.create(new SzczegolZamowieniaCreate(1).ilosc(tooMany), id));
      assertEquals("Brak produktu na stanie", createStockError.getMessage());
      var updateStockError = assertRejected(
          IllegalArgumentException.class,
          id,
          () -> items.update(line, new SzczegolZamowieniaUpdate().idProduktu(1).ilosc(tooMany), id));
      assertEquals("Brak produktu na stanie", updateStockError.getMessage());
      jdbc.update("UPDATE Produkt SET cena=99999999.99 WHERE id_produktu=1");
      var createTotalError = assertRejected(
          IllegalArgumentException.class, id, () -> items.create(new SzczegolZamowieniaCreate(1), id));
      assertEquals("Zbyt duża suma", createTotalError.getMessage());
      items.create(new SzczegolZamowieniaCreate(2), id);
      var updateTotalError = assertRejected(
          IllegalArgumentException.class, id,
          () -> items.update(line, new SzczegolZamowieniaUpdate().idProduktu(1).ilosc(1), id));
      assertEquals("Zbyt duża suma", updateTotalError.getMessage());
    } finally {
      jdbc.update("UPDATE Produkt SET cena=? WHERE id_produktu=1", originalPrice);
      orders.delete(id);
    }
  }

  @Test
  void missingReferencesAndNullBodiesDoNotChangeTheOrder() {
    var order = newOrder();
    int id = order.getIdZamowienia();
    int line = order.getPozycje().getFirst().getIdSzczegolu();
    try {
      assertRejected(IllegalArgumentException.class, id, () -> items.create(null, id));
      assertRejected(IllegalArgumentException.class, id, () -> items.update(line, null, id));
      var missing = assertRejected(
          NoSuchElementException.class,
          id,
          () -> items.create(new SzczegolZamowieniaCreate(3).idRecepty(Integer.MAX_VALUE), id));
      assertEquals("Nie znaleziono rekordu", missing.getMessage());
      assertInstanceOf(DataIntegrityViolationException.class, missing.getCause());
      var required = assertRejected(
          IllegalArgumentException.class,
          id,
          () -> items.create(new SzczegolZamowieniaCreate(3), id));
      assertEquals("Wymagana recepta", required.getMessage());
      var wrongCustomer = assertRejected(
          IllegalArgumentException.class,
          id,
          () -> items.create(new SzczegolZamowieniaCreate(3).idRecepty(2), id));
      assertEquals("Recepta należy do innego klienta", wrongCustomer.getMessage());
      var invalidId = assertRejected(
          IllegalArgumentException.class,
          id,
          () -> items.create(new SzczegolZamowieniaCreate(3).idRecepty(0), id));
      assertEquals("Niepoprawne ID", invalidId.getMessage());
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void missingOrderAndProductAreRejectedByDatabaseConstraints() {
    var order = newOrder();
    int id = order.getIdZamowienia();
    try {
      List<Runnable> invalidCreates = List.of(
          () -> items.create(new SzczegolZamowieniaCreate(2), Integer.MAX_VALUE),
          () -> items.create(new SzczegolZamowieniaCreate(Integer.MAX_VALUE), id),
          () -> items.create(new SzczegolZamowieniaCreate(null), id));
      for (var create : invalidCreates) {
        var before = state(id);
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM Szczegol_Zamowienia", Integer.class);
        var error = assertThrows(IllegalArgumentException.class, create::run);
        assertEquals("Błąd integralności danych", error.getMessage());
        assertInstanceOf(DataIntegrityViolationException.class, error.getCause());
        assertEquals(before, state(id));
        assertEquals(count,
            jdbc.queryForObject("SELECT COUNT(*) FROM Szczegol_Zamowienia", Integer.class));
      }
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void fulfilledPrescriptionAllowsReductionButRejectsIncreasesAndReplacement() {
    var prescription = prescriptions.create(new ReceptaCreate(1, "delta-rx", "80011501234"));
    int prescriptionId = prescription.getIdRecepty();
    var order = newOrder();
    int id = order.getIdZamowienia();
    try {
      jdbc.update("UPDATE Recepta SET status_recepty='ZWERYFIKOWANA' WHERE id_recepty=?", prescriptionId);
      var added =
          items.create(new SzczegolZamowieniaCreate(3).idRecepty(prescriptionId).ilosc(2), id);
      int line = added.getIdSzczegolu();
      jdbc.update("UPDATE Recepta SET status_recepty='ZREALIZOWANA' WHERE id_recepty=?", prescriptionId);
      var reduced =
          items.update(
              line, new SzczegolZamowieniaUpdate().idProduktu(3).idRecepty(prescriptionId).ilosc(1), id);
      assertEquals(added.getCenaJednostkowa(), reduced.getCenaJednostkowa());
      items.update(
          line, new SzczegolZamowieniaUpdate().idProduktu(3).idRecepty(prescriptionId).ilosc(1), id);
      assertRejected(
          IllegalArgumentException.class,
          id,
          () -> items.update(
              line, new SzczegolZamowieniaUpdate().idProduktu(3).idRecepty(prescriptionId).ilosc(2), id));
      assertRejected(
          IllegalArgumentException.class,
          id,
          () -> items.update(line, new SzczegolZamowieniaUpdate().idProduktu(3).ilosc(1), id));
      assertRejected(
          IllegalArgumentException.class,
          id,
          () -> items.update(
              line, new SzczegolZamowieniaUpdate().idProduktu(5).idRecepty(prescriptionId).ilosc(1), id));
      var fulfilled = assertRejected(
          IllegalArgumentException.class,
          id,
          () -> items.create(new SzczegolZamowieniaCreate(5).idRecepty(prescriptionId), id));
      assertEquals("Recepta nie jest ważna", fulfilled.getMessage());
      items.delete(line, id);
      assertTotal(id);
    } finally {
      orders.delete(id);
      prescriptions.delete(prescriptionId);
    }
  }

  @Test
  void createWaitsForPrescriptionStatusChangeAndRejectsTheCommittedInvalidStatus() throws Exception {
    var prescription = prescriptions.create(new ReceptaCreate(1, "locked-rx", "80011501234"));
    var order = newOrder();
    int id = order.getIdZamowienia(), prescriptionId = prescription.getIdRecepty();
    var before = state(id);
    var changed = new CountDownLatch(1);
    var commitStatus = new CountDownLatch(1);
    try {
      try (var executor = Executors.newFixedThreadPool(2)) {
        var statusChange = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
          jdbc.update("UPDATE Recepta SET status_recepty='ZREALIZOWANA' WHERE id_recepty=?", prescriptionId);
          changed.countDown();
          try {
            if (!commitStatus.await(30, TimeUnit.SECONDS))
              throw new IllegalStateException("Timed out waiting to commit prescription status");
          } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
          }
        }));
        try {
          assertTrue(changed.await(10, TimeUnit.SECONDS));
          var attempted = new CountDownLatch(1);
          var creation = executor.submit(() -> {
            attempted.countDown();
            return items.create(new SzczegolZamowieniaCreate(3).idRecepty(prescriptionId), id);
          });
          assertTrue(attempted.await(10, TimeUnit.SECONDS));
          assertThrows(TimeoutException.class, () -> creation.get(250, TimeUnit.MILLISECONDS));
          commitStatus.countDown();
          statusChange.get(30, TimeUnit.SECONDS);
          var failure = assertThrows(ExecutionException.class, () -> creation.get(30, TimeUnit.SECONDS));
          var error = assertInstanceOf(IllegalArgumentException.class, failure.getCause());
          assertEquals("Recepta nie jest ważna", error.getMessage());
          assertEquals(before, state(id));
        } finally {
          commitStatus.countDown();
        }
      }
    } finally {
      orders.delete(id);
      prescriptions.delete(prescriptionId);
    }
  }

  @Test
  void optionalPrescriptionCanBeClearedWithoutChangingPriceOrStock() {
    var prescription = prescriptions.create(new ReceptaCreate(1, "clear-rx", "80011501234"));
    var order = newOrder();
    int id = order.getIdZamowienia();
    int line = order.getPozycje().getFirst().getIdSzczegolu();
    try {
      var withPrescription =
          items.update(line,
              new SzczegolZamowieniaUpdate().idProduktu(4).idRecepty(prescription.getIdRecepty()).ilosc(1), id);
      var before = state(id);
      var cleared = items.update(line, new SzczegolZamowieniaUpdate().idProduktu(4).ilosc(1), id);
      assertNull(cleared.getIdRecepty());
      assertEquals(withPrescription.getCenaJednostkowa(), cleared.getCenaJednostkowa());
      assertEquals(before.stocks(), state(id).stocks());
      assertEquals(before.total(), state(id).total());
    } finally {
      orders.delete(id);
      prescriptions.delete(prescription.getIdRecepty());
    }
  }

  @Test
  void quantityIncreaseDoesNotTemporarilyReleaseTheExistingReservation() {
    int initialStock = stock(4);
    var order = newOrder();
    int id = order.getIdZamowienia();
    int line = order.getPozycje().getFirst().getIdSzczegolu();
    try {
      jdbc.update("UPDATE Produkt SET stan_magazynowy=? WHERE id_produktu=4", Integer.MAX_VALUE);
      items.update(line, new SzczegolZamowieniaUpdate().idProduktu(4).ilosc(2), id);
      assertEquals(Integer.MAX_VALUE - 1, stock(4));
      assertTotal(id);
    } finally {
      jdbc.update("UPDATE Produkt SET stan_magazynowy=? WHERE id_produktu=4",
          initialStock - items.get(line, id).getIlosc());
      orders.delete(id);
    }
    assertEquals(initialStock, stock(4));
  }

  @Test
  void concurrentAdditionsToOneOrderPreserveCommittedItemsStockAndTotal() throws Exception {
    int firstStock = stock(1), secondStock = stock(2);
    var order = newOrder();
    int id = order.getIdZamowienia();
    try {
      SzczegolZamowienia firstItem, secondItem;
      try (var executor = Executors.newFixedThreadPool(2)) {
        var start = new CountDownLatch(1);
        var first = executor.submit(() -> { start.await(); return items.create(new SzczegolZamowieniaCreate(1), id); });
        var second = executor.submit(() -> { start.await(); return items.create(new SzczegolZamowieniaCreate(2), id); });
        start.countDown();
        firstItem = committedItem(first);
        secondItem = committedItem(second);
      }
      int firstQuantity = firstItem == null ? 0 : 1;
      int secondQuantity = secondItem == null ? 0 : 1;
      assertTrue(firstQuantity + secondQuantity >= 1);
      var committed = items.list(id);
      assertEquals(1 + firstQuantity + secondQuantity, committed.size());
      assertEquals(firstQuantity, committed.stream().filter(item -> item.getIdProduktu() == 1).count());
      assertEquals(secondQuantity, committed.stream().filter(item -> item.getIdProduktu() == 2).count());
      assertEquals(firstStock - firstQuantity, stock(1));
      assertEquals(secondStock - secondQuantity, stock(2));
      assertTotal(id);
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void concurrentQuantityEditsKeepStockConsistentWithTheFinalQuantity() throws Exception {
    int initialStock = stock(4);
    var order = newOrder();
    int id = order.getIdZamowienia();
    int line = order.getPozycje().getFirst().getIdSzczegolu();
    try {
      try (var executor = Executors.newFixedThreadPool(2)) {
        var start = new CountDownLatch(1);
        var first = executor.submit(() -> { start.await(); return items.update(line, new SzczegolZamowieniaUpdate().idProduktu(4).ilosc(2), id); });
        var second = executor.submit(() -> { start.await(); return items.update(line, new SzczegolZamowieniaUpdate().idProduktu(4).ilosc(3), id); });
        start.countDown();
        assertNotNull(first.get(30, TimeUnit.SECONDS));
        assertNotNull(second.get(30, TimeUnit.SECONDS));
      }
      int finalQuantity = items.get(line, id).getIlosc();
      assertTrue(finalQuantity == 2 || finalQuantity == 3);
      assertEquals(initialStock - finalQuantity, stock(4));
      assertTotal(id);
    } finally {
      orders.delete(id);
    }
    assertEquals(initialStock, stock(4));
  }

  @Test
  void competingOrdersCannotReserveTheSameRemainingStock() throws Exception {
    int available = stock(1);
    var firstOrder = newOrder();
    var secondOrder = newOrder();
    int firstId = firstOrder.getIdZamowienia(), secondId = secondOrder.getIdZamowienia();
    try {
      try (var executor = Executors.newFixedThreadPool(2)) {
        var start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int id : List.of(firstId, secondId)) {
          results.add(executor.submit(() -> {
            start.await();
            try {
              items.create(new SzczegolZamowieniaCreate(1).ilosc(available), id);
              return true;
            } catch (IllegalArgumentException error) {
              assertEquals("Brak produktu na stanie", error.getMessage());
              return false;
            } catch (ConcurrencyFailureException error) {
              return false;
            }
          }));
        }
        start.countDown();
        int successes = 0;
        for (var result : results) if (result.get(30, TimeUnit.SECONDS)) successes++;
        assertEquals(1, successes);
      }
      assertEquals(0, stock(1));
      assertTotal(firstId);
      assertTotal(secondId);
    } finally {
      orders.delete(firstId);
      orders.delete(secondId);
    }
    assertEquals(available, stock(1));
  }
}
