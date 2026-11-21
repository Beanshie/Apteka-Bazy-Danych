package pk.bd.apteka;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

  private ZamowienieCreate request(SzczegolZamowieniaCreate... items) {
    return new ZamowienieCreate(1, 1, Arrays.asList(items));
  }

  private ZamowienieUpdate replacement(ZamowienieCreate request) {
    return new ZamowienieUpdate()
        .idKlienta(request.getIdKlienta())
        .idAdresu(request.getIdAdresu())
        .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.NOWE)
        .pozycje(request.getPozycje());
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
        order.getPozycje().stream()
            .map(item -> item.getCenaJednostkowa().multiply(BigDecimal.valueOf(item.getIlosc())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertEquals(0, expected.compareTo(order.getSumaCalkowita()));
  }

  @Test
  void creationDefaultsAndListFiltersReturnCompleteOrderedItems() {
    var order = orders.create(request(
        new SzczegolZamowieniaCreate(1).ilosc(null),
        new SzczegolZamowieniaCreate(2).ilosc(2)).statusZamowienia(null));
    int id = order.getIdZamowienia();
    try {
      assertEquals(Zamowienie.StatusZamowieniaEnum.NOWE, order.getStatusZamowienia());
      assertEquals(1, order.getPozycje().getFirst().getIlosc());
      assertEquals(ZoneOffset.UTC, order.getDataZamowienia().getOffset());
      for (var listed : List.of(orders.list(null, null), orders.list(1, null), orders.list(1, "NOWE"))) {
        var ids = listed.stream().map(Zamowienie::getIdZamowienia).toList();
        assertEquals(ids.stream().sorted().toList(), ids);
        var found = listed.stream().filter(value -> value.getIdZamowienia().equals(id)).findFirst().orElseThrow();
        assertEquals(order, found);
        for (var value : listed) {
          assertNotNull(value.getPozycje());
          var lineIds = value.getPozycje().stream().map(SzczegolZamowienia::getIdSzczegolu).toList();
          assertEquals(lineIds.stream().sorted().toList(), lineIds);
          assertTrue(value.getPozycje().stream()
              .allMatch(item -> item.getIdZamowienia().equals(value.getIdZamowienia())));
        }
      }
      assertTrue(orders.list(2, null).stream().allMatch(value -> value.getIdKlienta() == 2));
      assertTrue(orders.list(null, "ANULOWANE").stream()
          .allMatch(value -> value.getStatusZamowienia() == Zamowienie.StatusZamowieniaEnum.ANULOWANE));
      assertTrue(orders.list(Integer.MAX_VALUE, null).isEmpty());
      assertTotal(id);
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void replacementRefreshesPricesAndCanChangeClientWithMatchingAddress() {
    int firstStock = stock(1), secondStock = stock(2);
    var order = orders.create(request(new SzczegolZamowieniaCreate(1)));
    int id = order.getIdZamowienia();
    var originalPrice = order.getPozycje().getFirst().getCenaJednostkowa();
    var newPrice = originalPrice.add(BigDecimal.ONE);
    try {
      jdbc.update("UPDATE Produkt SET cena=? WHERE id_produktu=1", newPrice);
      var updated = orders.update(id, replacement(request(
          new SzczegolZamowieniaCreate(1).ilosc(null),
          new SzczegolZamowieniaCreate(2).ilosc(2))).idKlienta(2).idAdresu(2));
      assertEquals(2, updated.getIdKlienta());
      assertEquals(2, updated.getIdAdresu());
      assertEquals(order.getDataZamowienia(), updated.getDataZamowienia());
      assertNotEquals(order.getPozycje().getFirst().getIdSzczegolu(),
          updated.getPozycje().getFirst().getIdSzczegolu());
      assertEquals(newPrice, updated.getPozycje().getFirst().getCenaJednostkowa());
      assertEquals(1, updated.getPozycje().getFirst().getIlosc());
      assertEquals(firstStock - 1, stock(1));
      assertEquals(secondStock - 2, stock(2));
      assertTotal(id);
    } finally {
      jdbc.update("UPDATE Produkt SET cena=? WHERE id_produktu=1", originalPrice);
      orders.delete(id);
    }
    assertEquals(firstStock, stock(1));
    assertEquals(secondStock, stock(2));
  }

  @Test
  void cancelledCreationReplacementAndReactivationApplyOnlyActiveReservations() {
    int firstStock = stock(1), secondStock = stock(2);
    var order = orders.create(request(new SzczegolZamowieniaCreate(1).ilosc(2))
        .statusZamowienia(ZamowienieCreate.StatusZamowieniaEnum.ANULOWANE));
    int id = order.getIdZamowienia();
    try {
      assertEquals(firstStock, stock(1));
      orders.update(id, replacement(request(new SzczegolZamowieniaCreate(2).ilosc(3)))
          .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
      assertEquals(firstStock, stock(1));
      assertEquals(secondStock, stock(2));
      orders.update(id, replacement(request(new SzczegolZamowieniaCreate(1).ilosc(2))));
      assertEquals(firstStock - 2, stock(1));
      assertEquals(secondStock, stock(2));
      orders.update(id, replacement(request(new SzczegolZamowieniaCreate(2)))
          .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
      assertEquals(firstStock, stock(1));
      assertEquals(secondStock, stock(2));
      assertTotal(id);
    } finally {
      orders.delete(id);
    }
    orders.delete(id);
    assertEquals(firstStock, stock(1));
    assertEquals(secondStock, stock(2));
  }

  @Test
  void deletingActiveOrderRestoresStockAndCascadesItemsAndPayment() {
    int initialStock = stock(1);
    var order = orders.create(request(new SzczegolZamowieniaCreate(1).ilosc(2)));
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
  void malformedBodiesListsAndIdsDoNotWriteAnything() {
    assertRejected(IllegalArgumentException.class, () -> orders.create(null));
    assertRejected(IllegalArgumentException.class, () -> orders.create(new ZamowienieCreate()));
    assertRejected(IllegalArgumentException.class, () -> orders.create(request()));
    assertRejected(IllegalArgumentException.class,
        () -> orders.create(new ZamowienieCreate(1, 1, Collections.singletonList(null))));
    var order = orders.create(request(new SzczegolZamowieniaCreate(4)));
    int id = order.getIdZamowienia();
    try {
      assertRejected(IllegalArgumentException.class, () -> orders.update(id, null));
      assertRejected(IllegalArgumentException.class, () -> orders.update(id, replacement(request())));
      assertRejected(IllegalArgumentException.class,
          () -> orders.update(id, replacement(request()).pozycje(null)));
      assertRejected(IllegalArgumentException.class,
          () -> orders.update(id, replacement(request()).pozycje(Collections.singletonList(null))));
      for (Integer invalid : Arrays.asList(null, 0, -1)) {
        assertThrows(IllegalArgumentException.class, () -> orders.get(invalid));
        assertRejected(IllegalArgumentException.class,
            () -> orders.update(invalid, replacement(request(new SzczegolZamowieniaCreate(1)))));
        assertRejected(IllegalArgumentException.class, () -> orders.delete(invalid));
      }
      assertRejected(NoSuchElementException.class,
          () -> orders.update(Integer.MAX_VALUE, replacement(request(new SzczegolZamowieniaCreate(1)))));
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void quantityDuplicatesAndMissingReplacementStatusAreDatabaseFailures() {
    var order = orders.create(request(new SzczegolZamowieniaCreate(4)));
    int id = order.getIdZamowienia();
    try {
      for (int quantity : new int[] {0, -1}) {
        var invalid = request(new SzczegolZamowieniaCreate(1).ilosc(quantity));
        assertRejected(IllegalArgumentException.class, () -> orders.create(invalid));
        assertRejected(DataIntegrityViolationException.class, () -> orders.update(id, replacement(invalid)));
      }
      var duplicate = request(new SzczegolZamowieniaCreate(1), new SzczegolZamowieniaCreate(1));
      assertRejected(IllegalArgumentException.class, () -> orders.create(duplicate));
      assertRejected(DataIntegrityViolationException.class, () -> orders.update(id, replacement(duplicate)));
      assertRejected(DataIntegrityViolationException.class,
          () -> orders.update(id, replacement(request(new SzczegolZamowieniaCreate(1)))
              .statusZamowienia(null)));
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void missingReferencesAndAddressOrPrescriptionOwnershipErrorsRollBack() {
    var order = orders.create(request(new SzczegolZamowieniaCreate(4)));
    int id = order.getIdZamowienia();
    try {
      var missingClient = request(new SzczegolZamowieniaCreate(1)).idKlienta(Integer.MAX_VALUE);
      assertRejected(IllegalArgumentException.class, () -> orders.create(missingClient));
      assertRejected(NoSuchElementException.class, () -> orders.update(id, replacement(missingClient)));
      for (var invalid : List.of(
          request(new SzczegolZamowieniaCreate(1)).idAdresu(Integer.MAX_VALUE),
          request(new SzczegolZamowieniaCreate(Integer.MAX_VALUE)),
          request(new SzczegolZamowieniaCreate(3).idRecepty(Integer.MAX_VALUE)))) {
        assertRejected(NoSuchElementException.class, () -> orders.create(invalid));
        assertRejected(NoSuchElementException.class, () -> orders.update(id, replacement(invalid)));
      }
      for (var invalid : List.of(
          request(new SzczegolZamowieniaCreate(1)).idAdresu(2),
          request(new SzczegolZamowieniaCreate(3)),
          request(new SzczegolZamowieniaCreate(3).idRecepty(2)))) {
        assertRejected(IllegalArgumentException.class, () -> orders.create(invalid));
        assertRejected(IllegalArgumentException.class, () -> orders.update(id, replacement(invalid)));
      }
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void fullReplacementRevalidatesPrescriptionEvenForReductionOrCancellation() {
    var prescription = prescriptions.create(new ReceptaCreate(1, "order-rx", "80011501234"));
    int prescriptionId = prescription.getIdRecepty();
    var order = orders.create(request(new SzczegolZamowieniaCreate(3).idRecepty(prescriptionId).ilosc(2)));
    int id = order.getIdZamowienia();
    try {
      jdbc.update("UPDATE Recepta SET status_recepty='ZREALIZOWANA' WHERE id_recepty=?", prescriptionId);
      var invalid = request(new SzczegolZamowieniaCreate(3).idRecepty(prescriptionId));
      assertRejected(IllegalArgumentException.class, () -> orders.update(id, replacement(invalid)));
      assertRejected(IllegalArgumentException.class, () -> orders.update(id, replacement(invalid)
          .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE)));
      assertRejected(IllegalArgumentException.class, () -> orders.create(invalid
          .statusZamowienia(ZamowienieCreate.StatusZamowieniaEnum.ANULOWANE)));
      jdbc.update("UPDATE Recepta SET status_recepty='ZWERYFIKOWANA' WHERE id_recepty=?", prescriptionId);
      orders.update(id, replacement(invalid));
      assertTotal(id);
    } finally {
      orders.delete(id);
      prescriptions.delete(prescriptionId);
    }
  }

  @Test
  void insufficientStockAndExcessiveTotalsRollBackIncludingPartialReplacement() {
    var order = orders.create(request(new SzczegolZamowieniaCreate(4)));
    int id = order.getIdZamowienia();
    var originalPrice = jdbc.queryForObject("SELECT cena FROM Produkt WHERE id_produktu=1", BigDecimal.class);
    try {
      var unavailable = request(new SzczegolZamowieniaCreate(1).ilosc(stock(1) + 1));
      assertRejected(IllegalArgumentException.class, () -> orders.create(unavailable));
      assertRejected(IllegalArgumentException.class, () -> orders.update(id, replacement(unavailable)));
      var tooLarge = request(new SzczegolZamowieniaCreate(1).ilosc(Integer.MAX_VALUE))
          .statusZamowienia(ZamowienieCreate.StatusZamowieniaEnum.ANULOWANE);
      assertRejected(IllegalArgumentException.class, () -> orders.create(tooLarge));
      jdbc.update("UPDATE Produkt SET cena=99999999.99 WHERE id_produktu=1");
      var overflow = request(new SzczegolZamowieniaCreate(1), new SzczegolZamowieniaCreate(2));
      assertRejected(IllegalArgumentException.class, () -> orders.create(overflow));
      assertRejected(IllegalArgumentException.class, () -> orders.update(id, replacement(overflow)));
      var maximum = orders.create(request(new SzczegolZamowieniaCreate(1)));
      try {
        assertEquals(new BigDecimal("99999999.99"), maximum.getSumaCalkowita());
      } finally {
        orders.delete(maximum.getIdZamowienia());
      }
    } finally {
      jdbc.update("UPDATE Produkt SET cena=? WHERE id_produktu=1", originalPrice);
      orders.delete(id);
    }
  }

  @Test
  void replacementRollsBackWhenReturningOldStockWouldOverflow() {
    int initialStock = stock(4);
    var order = orders.create(request(new SzczegolZamowieniaCreate(4)));
    int id = order.getIdZamowienia();
    try {
      jdbc.update("UPDATE Produkt SET stan_magazynowy=? WHERE id_produktu=4", Integer.MAX_VALUE);
      assertRejected(DataIntegrityViolationException.class,
          () -> orders.update(id, replacement(request(new SzczegolZamowieniaCreate(4).ilosc(2)))));
      assertTotal(id);
    } finally {
      int quantity = orders.get(id).getPozycje().getFirst().getIlosc();
      jdbc.update("UPDATE Produkt SET stan_magazynowy=? WHERE id_produktu=4", initialStock - quantity);
      orders.delete(id);
    }
    assertEquals(initialStock, stock(4));
  }

  @Test
  void concurrentReplacementAndItemEditLeaveOneConsistentReservation() throws Exception {
    int initialStock = stock(4);
    var order = orders.create(request(new SzczegolZamowieniaCreate(4)));
    int id = order.getIdZamowienia(), line = order.getPozycje().getFirst().getIdSzczegolu();
    try {
      try (var executor = Executors.newFixedThreadPool(2)) {
        var start = new CountDownLatch(1);
        var replace = executor.submit(() -> {
          start.await();
          return orders.update(id, replacement(request(new SzczegolZamowieniaCreate(4).ilosc(2))));
        });
        var edit = executor.submit(() -> {
          start.await();
          try {
            items.update(line, new SzczegolZamowieniaUpdate().idProduktu(4).ilosc(3), id);
          } catch (NoSuchElementException replaced) {
            // Full replacement removes the original line if it wins the order lock first.
          }
          return true;
        });
        start.countDown();
        assertNotNull(replace.get(30, TimeUnit.SECONDS));
        assertTrue(edit.get(30, TimeUnit.SECONDS));
      }
      var current = orders.get(id);
      assertEquals(1, current.getPozycje().size());
      assertEquals(2, current.getPozycje().getFirst().getIlosc());
      assertEquals(initialStock - 2, stock(4));
      assertTotal(id);
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void concurrentCancellationAndAdditionLeaveNoActiveReservations() throws Exception {
    int firstStock = stock(4), secondStock = stock(2);
    var order = orders.create(request(new SzczegolZamowieniaCreate(4)));
    int id = order.getIdZamowienia();
    try {
      try (var executor = Executors.newFixedThreadPool(2)) {
        var start = new CountDownLatch(1);
        var cancel = executor.submit(() -> {
          start.await();
          return orders.update(id, replacement(request(new SzczegolZamowieniaCreate(4)))
              .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
        });
        var add = executor.submit(() -> {
          start.await();
          return items.create(new SzczegolZamowieniaCreate(2).ilosc(2), id);
        });
        start.countDown();
        assertNotNull(cancel.get(30, TimeUnit.SECONDS));
        assertNotNull(add.get(30, TimeUnit.SECONDS));
      }
      assertEquals(Zamowienie.StatusZamowieniaEnum.ANULOWANE, orders.get(id).getStatusZamowienia());
      assertEquals(firstStock, stock(4));
      assertEquals(secondStock, stock(2));
      assertTotal(id);
    } finally {
      orders.delete(id);
    }
  }

  @Test
  void competingCreationsSharingAnAddressFailOnlyForInsufficientStock() throws Exception {
    int available = stock(1);
    List<Zamowienie> successful = new ArrayList<>();
    try {
      try (var executor = Executors.newFixedThreadPool(2)) {
        var start = new CountDownLatch(1);
        List<Future<Zamowienie>> results = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
          results.add(executor.submit(() -> {
            start.await();
            try {
              return orders.create(request(new SzczegolZamowieniaCreate(1).ilosc(available)));
            } catch (IllegalArgumentException shortage) {
              assertEquals("Brak produktu na stanie", shortage.getMessage());
              return null;
            }
          }));
        }
        start.countDown();
        for (var result : results) {
          var created = result.get(30, TimeUnit.SECONDS);
          if (created != null) successful.add(created);
        }
      }
      assertEquals(1, successful.size());
      assertEquals(0, stock(1));
      assertTotal(successful.getFirst().getIdZamowienia());
    } finally {
      for (var order : successful) orders.delete(order.getIdZamowienia());
    }
    assertEquals(available, stock(1));
  }
}
