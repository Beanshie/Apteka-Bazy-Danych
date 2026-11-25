package pk.bd.apteka;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.ZoneOffset;
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
class ZamowienieServiceTest {
  @Container static final MariaDBContainer database = new MariaDBContainer("mariadb:11.4");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", database::getJdbcUrl);
    properties.add("spring.datasource.username", database::getUsername);
    properties.add("spring.datasource.password", database::getPassword);
  }

  @Autowired ZamowienieService orders;
  @Autowired SzczegolZamowieniaService items;
  @Autowired ReceptaService prescriptions;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager transactions;

  private Zamowienie orderWithItems(SzczegolZamowieniaCreate... values) {
    var order = orders.create(new ZamowienieCreate(1, 1));
    try {
      for (var value : values) items.create(value, order.getIdZamowienia());
      return orders.get(order.getIdZamowienia());
    } catch (RuntimeException error) {
      orders.delete(order.getIdZamowienia());
      throw error;
    }
  }

  private ZamowienieUpdate updateRequest() {
    return new ZamowienieUpdate().idKlienta(1).idAdresu(1)
        .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.NOWE);
  }

  private int stock(int product) {
    return jdbc.queryForObject(
        "SELECT stan_magazynowy FROM Produkt WHERE id_produktu=?", Integer.class, product);
  }

  private record State(
      List<Map<String, Object>> orders,
      List<Map<String, Object>> items,
      List<Map<String, Object>> payments,
      List<Integer> stocks) {}

  private State state() {
    return new State(
        jdbc.queryForList("SELECT * FROM Zamowienie ORDER BY id_zamowienia"),
        jdbc.queryForList("SELECT * FROM Szczegol_Zamowienia ORDER BY id_szczegolu"),
        jdbc.queryForList("SELECT * FROM Platnosc ORDER BY id_platnosci"),
        jdbc.queryForList("SELECT stan_magazynowy FROM Produkt ORDER BY id_produktu", Integer.class));
  }

  private void assertRejected(Class<? extends Throwable> exception, Runnable mutation) {
    var before = state();
    assertThrows(exception, mutation::run);
    assertEquals(before, state());
  }

  private void assertTotal(int id) {
    var order = orders.get(id);
    var expected =
        items.list(id).stream()
            .map(item -> item.getCenaJednostkowa().multiply(BigDecimal.valueOf(item.getIlosc())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertEquals(0, expected.compareTo(order.getSumaCalkowita()));
  }

  @Test
  void emptyCreationDefaultsAndListFiltersReturnHeaders() {
    int initial = stock(1);
    var order = orders.create(new ZamowienieCreate(1, 1).statusZamowienia(null));
    int id = order.getIdZamowienia();
    try {
      assertEquals(Zamowienie.StatusZamowieniaEnum.NOWE, order.getStatusZamowienia());
      assertEquals(0, order.getSumaCalkowita().signum());
      assertEquals(ZoneOffset.UTC, order.getDataZamowienia().getOffset());
      assertTrue(items.list(id).isEmpty());
      assertEquals(initial, stock(1));
      for (var listed : List.of(orders.list(null, null), orders.list(1, "NOWE"))) {
        var ids = listed.stream().map(Zamowienie::getIdZamowienia).toList();
        assertEquals(ids.stream().sorted().toList(), ids);
        assertTrue(listed.contains(order));
      }
      assertTrue(orders.list(2, null).stream().allMatch(value -> value.getIdKlienta() == 2));
      assertTrue(orders.list(null, "ANULOWANE").stream()
          .allMatch(value -> value.getStatusZamowienia() == Zamowienie.StatusZamowieniaEnum.ANULOWANE));
      assertTrue(orders.list(Integer.MAX_VALUE, null).isEmpty());
      assertEquals(order, orders.update(id, updateRequest()));
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void headerEditsPreserveItemsIdsPricesTotalsAndTimestamp() {
    var order = orderWithItems(new SzczegolZamowieniaCreate(1).ilosc(2));
    int id = order.getIdZamowienia();
    var before = items.list(id);
    var price = before.getFirst().getCenaJednostkowa();
    int reserved = stock(1);
    try {
      jdbc.update("UPDATE Produkt SET cena=cena+1 WHERE id_produktu=1");
      var changed = orders.update(id, updateRequest().idKlienta(2).idAdresu(2)
          .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.OPLACONE));
      assertEquals(2, changed.getIdKlienta());
      assertEquals(2, changed.getIdAdresu());
      assertEquals(order.getDataZamowienia(), changed.getDataZamowienia());
      assertEquals(order.getSumaCalkowita(), changed.getSumaCalkowita());
      assertEquals(before, items.list(id));
      assertEquals(reserved, stock(1));
      assertTotal(id);
    } finally {
      jdbc.update("UPDATE Produkt SET cena=? WHERE id_produktu=1", price);
      orders.delete(id);
    }
  }

  @Test
  void cancellationReactivationAndRepeatedStatusesPreserveItemsAndTotals() {
    int first = stock(1), second = stock(2);
    var order = orders.create(new ZamowienieCreate(1, 1)
        .statusZamowienia(ZamowienieCreate.StatusZamowieniaEnum.ANULOWANE));
    int id = order.getIdZamowienia();
    try {
      items.create(new SzczegolZamowieniaCreate(1).ilosc(2), id);
      items.create(new SzczegolZamowieniaCreate(2).ilosc(3), id);
      var before = items.list(id);
      var total = orders.get(id).getSumaCalkowita();
      assertEquals(first, stock(1));
      assertEquals(second, stock(2));
      for (int iteration = 0; iteration < 2; iteration++) {
        orders.update(id, updateRequest());
        orders.update(id, updateRequest().statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.OPLACONE));
        assertEquals(first - 2, stock(1));
        assertEquals(second - 3, stock(2));
        orders.update(id, updateRequest().statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
        orders.update(id, updateRequest().statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
        assertEquals(first, stock(1));
        assertEquals(second, stock(2));
        assertEquals(before, items.list(id));
        assertEquals(total, orders.get(id).getSumaCalkowita());
      }
    } finally {
      orders.delete(id);
    }
    assertEquals(first, stock(1));
    assertEquals(second, stock(2));
  }

  @Test
  void failedItemRequestLeavesItsEmptyOrderIntact() {
    var order = orders.create(new ZamowienieCreate(1, 1));
    int id = order.getIdZamowienia();
    try {
      assertRejected(IllegalArgumentException.class,
          () -> items.create(new SzczegolZamowieniaCreate(3), id));
      assertEquals(order, orders.get(id));
      assertTrue(items.list(id).isEmpty());
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void malformedHeadersMissingReferencesAndOwnershipErrorsDoNotWrite() {
    assertRejected(IllegalArgumentException.class, () -> orders.create(null));
    assertRejected(IllegalArgumentException.class, () -> orders.create(new ZamowienieCreate()));
    var order = orderWithItems(new SzczegolZamowieniaCreate(4));
    int id = order.getIdZamowienia();
    try {
      assertRejected(IllegalArgumentException.class, () -> orders.update(id, null));
      for (Integer invalid : Arrays.asList(null, 0, -1)) {
        assertThrows(IllegalArgumentException.class, () -> orders.get(invalid));
        assertRejected(IllegalArgumentException.class, () -> orders.update(invalid, updateRequest()));
        assertRejected(IllegalArgumentException.class, () -> orders.delete(invalid));
        assertRejected(IllegalArgumentException.class,
            () -> orders.create(new ZamowienieCreate(1, invalid)));
        assertRejected(IllegalArgumentException.class,
            () -> orders.update(id, updateRequest().idAdresu(invalid)));
        assertRejected(IllegalArgumentException.class,
            () -> orders.update(id, updateRequest().idKlienta(invalid)));
      }
      assertRejected(NoSuchElementException.class, () -> orders.update(Integer.MAX_VALUE, updateRequest()));
      assertRejected(IllegalArgumentException.class,
          () -> orders.create(new ZamowienieCreate(Integer.MAX_VALUE, 1)));
      assertRejected(DataIntegrityViolationException.class,
          () -> orders.update(id, updateRequest().idKlienta(Integer.MAX_VALUE)));
      assertRejected(NoSuchElementException.class,
          () -> orders.create(new ZamowienieCreate(1, Integer.MAX_VALUE)));
      assertRejected(NoSuchElementException.class,
          () -> orders.update(id, updateRequest().idAdresu(Integer.MAX_VALUE)));
      assertRejected(IllegalArgumentException.class, () -> orders.create(new ZamowienieCreate(1, 2)));
      assertRejected(IllegalArgumentException.class, () -> orders.update(id, updateRequest().idAdresu(2)));
      assertRejected(DataIntegrityViolationException.class,
          () -> orders.update(id, updateRequest().statusZamowienia(null)));
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void invalidPrescriptionStatusAllowsHeaderEditsAndCancellationButBlocksReactivation() {
    for (String status : List.of("ZREALIZOWANA", "ODRZUCONA")) {
      var prescription = prescriptions.create(new ReceptaCreate(1, "header-rx", "80011501234"));
      int rx = prescription.getIdRecepty();
      int id = orderWithItems(new SzczegolZamowieniaCreate(3).idRecepty(rx)).getIdZamowienia();
      try {
        jdbc.update("UPDATE Recepta SET status_recepty=? WHERE id_recepty=?", status, rx);
        var before = items.list(id);
        orders.update(id, updateRequest().idAdresu(5));
        orders.update(id, updateRequest().statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
        assertRejected(IllegalArgumentException.class, () -> orders.update(id, updateRequest()));
        assertRejected(IllegalArgumentException.class,
            () -> orders.update(id, updateRequest().idKlienta(2).idAdresu(2)
                .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE)));
        assertEquals(before, items.list(id));
        jdbc.update("UPDATE Recepta SET status_recepty='ZWERYFIKOWANA' WHERE id_recepty=?", rx);
        orders.update(id, updateRequest());
        assertEquals(before, items.list(id));
      } finally {
        orders.delete(id);
        prescriptions.delete(rx);
      }
    }
  }

  @Test
  void reactivationAndCancellationFailuresRollBackAllProductsAndTheHeader() {
    int first = stock(1), second = stock(2);
    var order = orderWithItems(new SzczegolZamowieniaCreate(1), new SzczegolZamowieniaCreate(2));
    int id = order.getIdZamowienia();
    try {
      jdbc.update("UPDATE Produkt SET stan_magazynowy=? WHERE id_produktu=2", Integer.MAX_VALUE);
      assertRejected(DataIntegrityViolationException.class,
          () -> orders.update(id, updateRequest().statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE)));
      jdbc.update("UPDATE Produkt SET stan_magazynowy=? WHERE id_produktu=2", second - 1);
      orders.update(id, updateRequest().statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
      jdbc.update("UPDATE Produkt SET stan_magazynowy=0 WHERE id_produktu=2");
      assertRejected(IllegalArgumentException.class, () -> orders.update(id, updateRequest()));
      assertEquals(first, stock(1));
    } finally {
      jdbc.update("UPDATE Produkt SET stan_magazynowy=? WHERE id_produktu=2", second);
      orders.delete(id);
    }
    assertEquals(first, stock(1));
    assertEquals(second, stock(2));
  }

  @Test
  void reactivationWaitsForPrescriptionStatusChangeAndRejectsTheCommittedStatus() throws Exception {
    var prescription = prescriptions.create(new ReceptaCreate(1, "resume-rx", "80011501234"));
    int rx = prescription.getIdRecepty();
    int id = orderWithItems(new SzczegolZamowieniaCreate(3).idRecepty(rx)).getIdZamowienia();
    var release = new CountDownLatch(1);
    try {
      orders.update(id, updateRequest().statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
      var before = state();
      try (var executor = Executors.newFixedThreadPool(2)) {
        var locked = new CountDownLatch(1);
        var change = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
          jdbc.update("UPDATE Recepta SET status_recepty='ODRZUCONA' WHERE id_recepty=?", rx);
          locked.countDown();
          try {
            assertTrue(release.await(10, TimeUnit.SECONDS));
          } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
          }
        }));
        try {
          assertTrue(locked.await(5, TimeUnit.SECONDS));
          var started = new CountDownLatch(1);
          var resume = executor.submit(() -> {
            started.countDown();
            return orders.update(id, updateRequest());
          });
          assertTrue(started.await(5, TimeUnit.SECONDS));
          assertThrows(TimeoutException.class, () -> resume.get(250, TimeUnit.MILLISECONDS));
          release.countDown();
          change.get(10, TimeUnit.SECONDS);
          var failure = assertThrows(ExecutionException.class, () -> resume.get(10, TimeUnit.SECONDS));
          var error = assertInstanceOf(IllegalArgumentException.class, failure.getCause());
          assertEquals("Recepta nie jest ważna", error.getMessage());
          assertEquals(before, state());
        } finally {
          release.countDown();
          change.get(10, TimeUnit.SECONDS);
        }
      }
    } finally {
      release.countDown();
      orders.delete(id);
      prescriptions.delete(rx);
    }
  }

  @Test
  void concurrentStatusChangesAndItemMutationsPreserveCommittedReservations() throws Exception {
    for (boolean reactivating : new boolean[] {false, true}) {
      for (String action : List.of("create", "update", "delete")) {
        int baseline = stock(4), secondStock = stock(2);
        int id = orderWithItems(new SzczegolZamowieniaCreate(4)).getIdZamowienia();
        int line = items.list(id).getFirst().getIdSzczegolu();
        if (reactivating) orders.update(id, updateRequest()
            .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
        try {
          try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var cancel = executor.submit(() -> {
              start.await();
              try {
                orders.update(id, updateRequest().statusZamowienia(reactivating
                    ? ZamowienieUpdate.StatusZamowieniaEnum.NOWE : ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
                return true;
              } catch (ConcurrencyFailureException error) { return false; }
            });
            var mutation = executor.submit(() -> {
              start.await();
              try {
                switch (action) {
                  case "create" -> items.create(new SzczegolZamowieniaCreate(2), id);
                  case "update" -> items.update(line, new SzczegolZamowieniaUpdate().idProduktu(4).ilosc(3), id);
                  case "delete" -> items.delete(line, id);
                }
                return true;
              } catch (ConcurrencyFailureException error) { return false; }
            });
            start.countDown();
            boolean cancelled = cancel.get(30, TimeUnit.SECONDS);
            boolean changed = mutation.get(30, TimeUnit.SECONDS);
            assertTrue(cancelled || changed);
          }
          boolean active = orders.get(id).getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE;
          int quantity = items.list(id).stream().filter(item -> item.getIdProduktu() == 4)
              .mapToInt(SzczegolZamowienia::getIlosc).sum();
          assertEquals(baseline - (active ? quantity : 0), stock(4));
          int secondQuantity = items.list(id).stream().filter(item -> item.getIdProduktu() == 2)
              .mapToInt(SzczegolZamowienia::getIlosc).sum();
          assertEquals(secondStock - (active ? secondQuantity : 0), stock(2));
          assertTotal(id);
        } finally { orders.delete(id); }
        assertEquals(baseline, stock(4));
        assertEquals(secondStock, stock(2));
      }
    }
  }

  @Test
  void deletingActiveOrderRestoresStockAndCascadesItemsAndPayment() {
    int initialStock = stock(1);
    var order = orderWithItems(new SzczegolZamowieniaCreate(1).ilosc(2));
    int id = order.getIdZamowienia();
    try {
      jdbc.update("INSERT INTO Platnosc(id_zamowienia,metoda_platnosci) VALUES (?, 'KARTA')", id);
      orders.delete(id);
      assertThrows(NoSuchElementException.class, () -> orders.get(id));
      assertEquals(0, jdbc.queryForObject(
          "SELECT COUNT(*) FROM Szczegol_Zamowienia WHERE id_zamowienia=?", Integer.class, id));
      assertEquals(0, jdbc.queryForObject(
          "SELECT COUNT(*) FROM Platnosc WHERE id_zamowienia=?", Integer.class, id));
      assertEquals(initialStock, stock(1));
      var after = state();
      orders.delete(id);
      assertEquals(after, state());
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void deletionRollsBackItemsHeaderAndPaymentWhenStockRestorationOverflows() {
    int firstStock = stock(1), initialStock = stock(4);
    var order = orderWithItems(new SzczegolZamowieniaCreate(1).ilosc(2), new SzczegolZamowieniaCreate(4));
    int id = order.getIdZamowienia();
    try {
      jdbc.update("INSERT INTO Platnosc(id_zamowienia,metoda_platnosci) VALUES (?,'KARTA')", id);
      jdbc.update("UPDATE Produkt SET stan_magazynowy=? WHERE id_produktu=4", Integer.MAX_VALUE);
      assertRejected(DataIntegrityViolationException.class, () -> orders.delete(id));
    } finally {
      jdbc.update("UPDATE Produkt SET stan_magazynowy=? WHERE id_produktu=4", initialStock - 1);
      orders.delete(id);
    }
    assertEquals(firstStock, stock(1));
    assertEquals(initialStock, stock(4));
  }

  @Test
  void mutationsDoNotWaitForAnUnrelatedProductLock() {
    int initialStock = stock(1);
    try (var executor = Executors.newSingleThreadExecutor()) {
      new TransactionTemplate(transactions).executeWithoutResult(status -> {
        jdbc.queryForObject("SELECT id_produktu FROM Produkt WHERE id_produktu=10 FOR UPDATE", Integer.class);
        var mutation = executor.submit(() -> {
          var order = orderWithItems(new SzczegolZamowieniaCreate(1));
          int id = order.getIdZamowienia();
          try {
            orders.update(id, updateRequest().idAdresu(5));
            assertTotal(id);
          } finally {
            orders.delete(id);
          }
          assertThrows(NoSuchElementException.class, () -> orders.get(id));
        });
        assertDoesNotThrow(() -> mutation.get(5, TimeUnit.SECONDS));
      });
    }
    assertEquals(initialStock, stock(1));
  }

  @Test
  void headerMutationsWaitForAddressChangesAndRejectCommittedOwnershipMismatch() throws Exception {
    int id = orderWithItems(new SzczegolZamowieniaCreate(4)).getIdZamowienia();
    try {
      for (boolean updating : new boolean[] {false, true}) {
        var before = state();
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
          var addressChange = executor.submit(() ->
              new TransactionTemplate(transactions).executeWithoutResult(status -> {
                jdbc.update("UPDATE Adres_Klienta SET id_klienta=2 WHERE id_adresu=5");
                locked.countDown();
                try {
                  assertTrue(release.await(10, TimeUnit.SECONDS));
                } catch (InterruptedException error) {
                  Thread.currentThread().interrupt();
                  throw new IllegalStateException(error);
                }
              }));
          try {
            assertTrue(locked.await(5, TimeUnit.SECONDS));
            var started = new CountDownLatch(1);
            var mutation = executor.submit(() -> {
              started.countDown();
              var request = new ZamowienieCreate(1, 5);
              return updating ? orders.update(id, new ZamowienieUpdate().idKlienta(request.getIdKlienta()).idAdresu(request.getIdAdresu())
                  .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.NOWE)) : orders.create(request);
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> mutation.get(250, TimeUnit.MILLISECONDS));
            release.countDown();
            addressChange.get(10, TimeUnit.SECONDS);
            var failure = assertThrows(ExecutionException.class, () -> mutation.get(10, TimeUnit.SECONDS));
            var error = assertInstanceOf(IllegalArgumentException.class, failure.getCause());
            assertEquals("Adres należy do innego klienta", error.getMessage());
            assertEquals(before, state());
          } finally {
            release.countDown();
            addressChange.get(10, TimeUnit.SECONDS);
          }
        } finally {
          jdbc.update("UPDATE Adres_Klienta SET id_klienta=1 WHERE id_adresu=5");
        }
      }
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void concurrentOrderDeletionsRestoreStockExactlyOnce() throws Exception {
    int initialStock = stock(4);
    int id = orderWithItems(new SzczegolZamowieniaCreate(4).ilosc(2)).getIdZamowienia();
    try {
      jdbc.update("INSERT INTO Platnosc(id_zamowienia,metoda_platnosci) VALUES (?,'KARTA')", id);
      try (var executor = Executors.newFixedThreadPool(2)) {
        var start = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
          results.add(executor.submit(() -> {
            start.await();
            orders.delete(id);
            return null;
          }));
        }
        start.countDown();
        for (var result : results) result.get(30, TimeUnit.SECONDS);
      }
      assertThrows(NoSuchElementException.class, () -> orders.get(id));
      assertEquals(0, jdbc.queryForObject(
          "SELECT COUNT(*) FROM Szczegol_Zamowienia WHERE id_zamowienia=?", Integer.class, id));
      assertEquals(0, jdbc.queryForObject(
          "SELECT COUNT(*) FROM Platnosc WHERE id_zamowienia=?", Integer.class, id));
      assertEquals(initialStock, stock(4));
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void concurrentOrderDeletionAndExistingItemOrStatusChangesPreserveCommittedReservations() throws Exception {
    for (boolean cancelled : new boolean[] {false, true}) {
      for (String action : List.of("update", "delete", "status")) {
        int firstStock = stock(4), secondStock = stock(2);
        int id = orderWithItems(new SzczegolZamowieniaCreate(4).ilosc(2)).getIdZamowienia();
        int line = items.list(id).getFirst().getIdSzczegolu();
        if (cancelled) orders.update(id, updateRequest()
            .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
        try {
          boolean deleted;
          boolean changed;
          try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var deletion = executor.submit(() -> {
              start.await();
              try {
                orders.delete(id);
                return true;
              } catch (ConcurrencyFailureException rolledBack) {
                return false;
              }
            });
            var mutation = executor.submit(() -> {
              start.await();
              try {
                switch (action) {
                  case "update" -> items.update(line, new SzczegolZamowieniaUpdate().idProduktu(2).ilosc(3), id);
                  case "delete" -> items.delete(line, id);
                  case "status" -> orders.update(id, updateRequest().statusZamowienia(cancelled
                      ? ZamowienieUpdate.StatusZamowieniaEnum.NOWE : ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
                }
                return true;
              } catch (NoSuchElementException missingAfterDeletion) {
                assertEquals("Nie znaleziono rekordu", missingAfterDeletion.getMessage());
                return false;
              } catch (ConcurrencyFailureException rolledBack) {
                return false;
              }
            });
            start.countDown();
            deleted = deletion.get(30, TimeUnit.SECONDS);
            changed = mutation.get(30, TimeUnit.SECONDS);
          }
          assertTrue(deleted || changed);
          if (deleted) {
            assertThrows(NoSuchElementException.class, () -> orders.get(id));
            assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM Szczegol_Zamowienia WHERE id_zamowienia=?", Integer.class, id));
            assertEquals(firstStock, stock(4));
            assertEquals(secondStock, stock(2));
          } else {
            boolean active = action.equals("status") ? cancelled : !cancelled;
            assertEquals(active, orders.get(id).getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE);
            var remaining = items.list(id);
            assertEquals(action.equals("delete") ? 0 : 1, remaining.size());
            if (!remaining.isEmpty()) {
              assertEquals(action.equals("update") ? 2 : 4, remaining.getFirst().getIdProduktu());
              assertEquals(action.equals("update") ? 3 : 2, remaining.getFirst().getIlosc());
            }
            assertEquals(firstStock - (active && action.equals("status") ? 2 : 0), stock(4));
            assertEquals(secondStock - (active && action.equals("update") ? 3 : 0), stock(2));
            assertTotal(id);
          }
        } finally {
          orders.delete(id);
        }
        assertEquals(firstStock, stock(4));
        assertEquals(secondStock, stock(2));
      }
    }
  }

  @Test
  void concurrentOrderDeletionAndItemCreationPreserveOnlyCommittedReservations() throws Exception {
    int firstStock = stock(4), secondStock = stock(2);
    int id = orderWithItems(new SzczegolZamowieniaCreate(4)).getIdZamowienia();
    try {
      boolean deleted;
      boolean added;
      try (var executor = Executors.newFixedThreadPool(2)) {
        var start = new CountDownLatch(1);
        var deletion = executor.submit(() -> {
          start.await();
          try {
            orders.delete(id);
            return true;
          } catch (ConcurrencyFailureException rolledBack) {
            return false;
          }
        });
        var addition = executor.submit(() -> {
          start.await();
          try {
            items.create(new SzczegolZamowieniaCreate(2).ilosc(2), id);
            return true;
          } catch (IllegalArgumentException missingParent) {
            assertEquals("Błąd integralności danych", missingParent.getMessage());
            return false;
          } catch (ConcurrencyFailureException rolledBack) {
            return false;
          }
        });
        start.countDown();
        deleted = deletion.get(30, TimeUnit.SECONDS);
        added = addition.get(30, TimeUnit.SECONDS);
      }
      assertTrue(deleted || added);
      if (deleted) {
        assertThrows(NoSuchElementException.class, () -> orders.get(id));
        assertEquals(0, jdbc.queryForObject(
            "SELECT COUNT(*) FROM Szczegol_Zamowienia WHERE id_zamowienia=?", Integer.class, id));
        assertEquals(firstStock, stock(4));
        assertEquals(secondStock, stock(2));
      } else {
        assertEquals(2, items.list(id).size());
        assertEquals(firstStock - 1, stock(4));
        assertEquals(secondStock - 2, stock(2));
        assertTotal(id);
      }
    } finally {
      orders.delete(id);
    }
    assertEquals(firstStock, stock(4));
    assertEquals(secondStock, stock(2));
  }

}
