package pk.bd.apteka;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mariadb.MariaDBContainer;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class AptekaApplicationTests {
  @Container static final MariaDBContainer database = new MariaDBContainer("mariadb:11.4");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry p) {
    p.add("spring.datasource.url", database::getJdbcUrl);
    p.add("spring.datasource.username", database::getUsername);
    p.add("spring.datasource.password", database::getPassword);
  }

  @Autowired ZamowienieService orders;
  @Autowired KlientService customers;
  @Autowired JdbcTemplate jdbc;

  private Zamowienie orderWithItem(int product, int quantity) {
    var order = orders.create(new ZamowienieCreate(1, 1));
    try {
      items.create(new SzczegolZamowieniaCreate(product).ilosc(quantity), order.getIdZamowienia());
      return orders.get(order.getIdZamowienia());
    } catch (RuntimeException error) {
      orders.delete(order.getIdZamowienia());
      throw error;
    }
  }

  private int stock() {
    return jdbc.queryForObject(
        "SELECT stan_magazynowy FROM Produkt WHERE id_produktu=1", Integer.class);
  }

  @Test
  void reservationSnapshotsTotalAndDeletion() {
    int stock = stock();
    var order = orderWithItem(1, 2);
    try {
      assertEquals(stock - 2, stock());
      assertEquals(0, new java.math.BigDecimal("25.00").compareTo(order.getSumaCalkowita()));
      assertEquals(1, items.list(order.getIdZamowienia()).size());
    } finally {
      orders.delete(order.getIdZamowienia());
    }
    assertEquals(stock, stock());
  }

  @Test
  void invalidItemRequestPreservesItsOrder() {
    var order = orders.create(new ZamowienieCreate(1, 1));
    int id = order.getIdZamowienia();
    try {
      assertThrows(IllegalArgumentException.class,
          () -> items.create(new SzczegolZamowieniaCreate(3), id));
      assertEquals(order, orders.get(id));
      assertTrue(items.list(id).isEmpty());
    } finally { orders.delete(id); }
  }


  @Test
  void passwordsAreHashedAndNeverReturned() {
    var customer =
        customers.create(
            new KlientCreate("Test", "Patient", "hash-test@example.com", "password123"));
    try {
      String hash =
          jdbc.queryForObject(
              "SELECT haslo FROM Klient WHERE id_klienta=?", String.class, customer.getIdKlienta());
      assertTrue(
          new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
              .matches("password123", hash));
    } finally {
      customers.delete(customer.getIdKlienta());
    }
  }

  @Test
  void competingReservationsCannotOversell() throws Exception {
    int stock = stock();
    try (var executor = Executors.newFixedThreadPool(2)) {
      var jobs =
          executor.invokeAll(
              List.<Callable<Zamowienie>>of(
                  () -> {
                    try {
                      return orderWithItem(1, stock);
                    } catch (RuntimeException e) {
                      return null;
                    }
                  },
                  () -> {
                    try {
                      return orderWithItem(1, stock);
                    } catch (RuntimeException e) {
                      return null;
                    }
                  }));
      List<Zamowienie> successful = new ArrayList<>();
      for (var job : jobs) if (job.get() != null) successful.add(job.get());
      try {
        assertEquals(1, successful.size());
        assertEquals(0, stock());
      } finally {
        for (var order : successful) orders.delete(order.getIdZamowienia());
      }
    }
    assertEquals(stock, stock());
  }

  @Test
  void cancellationAndReactivationAdjustStock() {
    int initial = stock();
    var order = orderWithItem(1, 2);
    int id = order.getIdZamowienia();
    try {
      orders.update(
          id,
          new ZamowienieUpdate()
              .idKlienta(1)
              .idAdresu(1)

              .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
      assertEquals(initial, stock());
      orders.update(
          id,
          new ZamowienieUpdate()
              .idKlienta(1)
              .idAdresu(1)

              .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.NOWE));
      assertEquals(initial - 2, stock());
    } finally {
      orders.delete(id);
    }
    assertEquals(initial, stock());
  }

  @Test
  void headerChangesPreserveExistingPriceSnapshotsAndItems() {
    var order = orderWithItem(1, 2);
    int id = order.getIdZamowienia();
    var before = items.list(id);
    var price = before.getFirst().getCenaJednostkowa();
    int reserved = stock();
    try {
      jdbc.update("UPDATE Produkt SET cena=cena+1 WHERE id_produktu=1");
      var updated = orders.update(id, new ZamowienieUpdate().idKlienta(1).idAdresu(5)
          .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.OPLACONE));
      assertEquals(before, items.list(id));
      assertEquals(order.getSumaCalkowita(), updated.getSumaCalkowita());
      assertEquals(order.getDataZamowienia(), updated.getDataZamowienia());
      assertEquals(reserved, stock());
      assertTrue(orders.list(1, "OPLACONE").contains(updated));
    } finally {
      jdbc.update("UPDATE Produkt SET cena=? WHERE id_produktu=1", price);
      orders.delete(id);
    }
  }


  @Test
  void failedHeaderUpdatePreservesItemsStatusTotalAndStock() {
    var order = orderWithItem(1, 2);
    int id = order.getIdZamowienia();
    var before = items.list(id);
    int reserved = stock();
    try {
      assertThrows(IllegalArgumentException.class,
          () -> orders.update(id, new ZamowienieUpdate().idKlienta(1).idAdresu(2)
              .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE)));
      assertEquals(order, orders.get(id));
      assertEquals(before, items.list(id));
      assertEquals(reserved, stock());
    } finally { orders.delete(id); }
  }


  @Autowired SzczegolZamowieniaService items;

  @Test
  void quantityEditPreservesSnapshotAndRejectsWrongParent() {
    var order = orderWithItem(1, 1);
    int id = order.getIdZamowienia(), line = items.list(order.getIdZamowienia()).getFirst().getIdSzczegolu();
    var price = items.list(order.getIdZamowienia()).getFirst().getCenaJednostkowa();
    jdbc.update("UPDATE Produkt SET cena=99.00 WHERE id_produktu=1");
    try {
      items.update(line, new SzczegolZamowieniaUpdate().idProduktu(1).ilosc(2), id);
      assertEquals(
          0,
          price
              .multiply(new java.math.BigDecimal("2"))
              .compareTo(orders.get(id).getSumaCalkowita()));
      assertThrows(RuntimeException.class, () -> items.get(line, 1));
    } finally {
      jdbc.update("UPDATE Produkt SET cena=? WHERE id_produktu=1", price);
      orders.delete(id);
    }
  }

  @Test
  void rejectedReservationDoesNotLeakStock() {
    int initial = stock();
    assertThrows(RuntimeException.class, () -> orderWithItem(1, initial + 1));
    assertEquals(initial, stock());
  }

  @Test
  void duplicateAndForeignKeyDeletesAreConflicts() {
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "INSERT INTO Kategoria(nazwa_kategorii) SELECT nazwa_kategorii FROM Kategoria WHERE"
                    + " id_kategorii=1"));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> jdbc.update("DELETE FROM Kategoria WHERE id_kategorii=1"));
  }

  @Autowired KategoriaService categories;
  @Autowired DostawcaService suppliers;
  @Autowired PracownikService employees;
  @Autowired AdresKlientaService addresses;
  @Autowired ReceptaService prescriptions;
  @Autowired PlatnoscService payments;
  @Autowired ProduktService products;

  @Test
  void simpleResourcesRoundTripAndRepeatedDeletion() {
    var category =
        categories.create(new KategoriaCreate().nazwaKategorii("Refactor category").opis("old"));
    var supplier = suppliers.create(new DostawcaCreate("Refactor supplier"));
    var employeeRequest = new PracownikCreate("refactor-user", "secret123", "Jan", "Nowak");
    var employee = employees.create(employeeRequest);
    try {
      assertNull(
          categories
              .update(
                  category.getIdKategorii(),
                  new KategoriaUpdate().nazwaKategorii("Refactor category"))
              .getOpis());
      assertEquals("Refactor supplier", suppliers.get(supplier.getIdDostawcy()).getNazwaFirmy());
      assertNotNull(employee.getRola());
      assertNotNull(employee.getAktywny());
      assertEquals("secret123", employeeRequest.getHaslo());
      String stored =
          jdbc.queryForObject(
              "SELECT haslo FROM Pracownik WHERE id_pracownika=?",
              String.class,
              employee.getIdPracownika());
      assertTrue(
          new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
              .matches("secret123", stored));
      employees.update(
          employee.getIdPracownika(),
          new PracownikUpdate()
              .login(employee.getLogin())
              .imie(employee.getImie())
              .nazwisko(employee.getNazwisko())
              .rola(PracownikUpdate.RolaEnum.KIEROWNIK)
              .aktywny(true)
              .haslo(null));
      assertEquals(
          Pracownik.RolaEnum.KIEROWNIK, employees.get(employee.getIdPracownika()).getRola());
      assertTrue(employees.get(employee.getIdPracownika()).getAktywny());
      assertEquals(
          stored,
          jdbc.queryForObject(
              "SELECT haslo FROM Pracownik WHERE id_pracownika=?",
              String.class,
              employee.getIdPracownika()));
      assertFalse(categories.list().isEmpty());
      assertFalse(suppliers.list().isEmpty());
      assertFalse(employees.list().isEmpty());
      assertFalse(products.list(null, null).isEmpty());
    } finally {
      categories.delete(category.getIdKategorii());
      categories.delete(category.getIdKategorii());
      suppliers.delete(supplier.getIdDostawcy());
      suppliers.delete(supplier.getIdDostawcy());
      employees.delete(employee.getIdPracownika());
      employees.delete(employee.getIdPracownika());
    }
    assertThrows(NoSuchElementException.class, () -> categories.get(category.getIdKategorii()));
  }

  @Test
  void relatedResourcesMapEnumsTimestampsAndMissingDeletes() {
    var address = addresses.create(new AdresKlientaCreate(1, "Testowa", "1", "00-001", "Warszawa"));
    var prescription = prescriptions.create(new ReceptaCreate(1, "ref-rx", "12345678901"));
    var order = orderWithItem(1, 1);
    var payment = payments.create(new PlatnoscCreate(order.getIdZamowienia(), "KARTA"));
    try {
      assertEquals(1, addresses.get(address.getIdAdresu()).getIdKlienta());
      assertNotNull(prescription.getStatusRecepty());
      assertEquals(java.time.ZoneOffset.UTC, order.getDataZamowienia().getOffset());
      assertNull(items.list(order.getIdZamowienia()).getFirst().getIdRecepty());
      assertEquals(
          0,
          new java.math.BigDecimal("12.50")
              .compareTo(items.list(order.getIdZamowienia()).getFirst().getCenaJednostkowa()));
      assertNotNull(payment.getStatusPlatnosci());
      assertFalse(payments.list(order.getIdZamowienia()).isEmpty());
      assertFalse(prescriptions.list(1, null, null).isEmpty());
      assertFalse(addresses.list(1).isEmpty());
      int stock = stock();
      var total = orders.get(order.getIdZamowienia()).getSumaCalkowita();
      items.delete(Integer.MAX_VALUE, order.getIdZamowienia());
      assertEquals(stock, stock());
      assertEquals(total, orders.get(order.getIdZamowienia()).getSumaCalkowita());
      assertThrows(NoSuchElementException.class, () -> items.delete(1, Integer.MAX_VALUE));
    } finally {
      payments.delete(payment.getIdPlatnosci());
      payments.delete(payment.getIdPlatnosci());
      orders.delete(order.getIdZamowienia());
      int released = stock();
      orders.delete(order.getIdZamowienia());
      assertEquals(released, stock());
      addresses.delete(address.getIdAdresu());
      addresses.delete(address.getIdAdresu());
      prescriptions.delete(prescription.getIdRecepty());
      prescriptions.delete(prescription.getIdRecepty());
    }
  }

  @Test
  void itemProductChangesAndCancelledEditsPreserveStockAndPrices() {
    int initialFirst = stock();
    int initialSecond =
        jdbc.queryForObject(
            "SELECT stan_magazynowy FROM Produkt WHERE id_produktu=2", Integer.class);
    var secondPrice = products.get(2).getCena();
    var order = orderWithItem(4, 1);
    int orderId = order.getIdZamowienia();
    try {
      var added = items.create(new SzczegolZamowieniaCreate(1), orderId);
      assertEquals(1, added.getIlosc());
      assertEquals(initialFirst - 1, stock());
      var changed =
          items.update(
              added.getIdSzczegolu(),
              new SzczegolZamowieniaUpdate().idProduktu(2).ilosc(2),
              orderId);
      assertEquals(0, secondPrice.compareTo(changed.getCenaJednostkowa()));
      assertEquals(initialFirst, stock());
      assertEquals(initialSecond - 2, products.get(2).getStanMagazynowy());
      assertEquals(
          0,
          order
              .getSumaCalkowita()
              .add(secondPrice.multiply(java.math.BigDecimal.valueOf(2)))
              .compareTo(orders.get(orderId).getSumaCalkowita()));
      items.delete(changed.getIdSzczegolu(), orderId);
      assertEquals(initialSecond, products.get(2).getStanMagazynowy());
      assertEquals(order.getSumaCalkowita(), orders.get(orderId).getSumaCalkowita());
      orders.update(
          orderId,
          new ZamowienieUpdate()
              .idKlienta(1)
              .idAdresu(1)

              .statusZamowienia(ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE));
      var cancelledItem = items.create(new SzczegolZamowieniaCreate(2), orderId);
      items.update(
          cancelledItem.getIdSzczegolu(),
          new SzczegolZamowieniaUpdate().idProduktu(2).ilosc(3),
          orderId);
      assertEquals(initialFirst, stock());
      assertEquals(initialSecond, products.get(2).getStanMagazynowy());
      items.delete(cancelledItem.getIdSzczegolu(), orderId);
      assertEquals(initialSecond, products.get(2).getStanMagazynowy());
    } finally {
      orders.delete(orderId);
    }
    assertEquals(initialFirst, stock());
    assertEquals(initialSecond, products.get(2).getStanMagazynowy());
  }

  @Test
  void failedItemMutationsRollBackStockItemsAndTotals() {
    var order = orderWithItem(1, 1);
    int orderId = order.getIdZamowienia();
    int lineId = items.list(order.getIdZamowienia()).getFirst().getIdSzczegolu();
    int reservedStock = stock();
    int prescriptionStock = products.get(3).getStanMagazynowy();
    try {
      assertThrows(
          IllegalArgumentException.class,
          () -> items.create(new SzczegolZamowieniaCreate(3), orderId));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              items.update(lineId, new SzczegolZamowieniaUpdate().idProduktu(3).ilosc(1), orderId));
      assertThrows(
          IllegalArgumentException.class,
          () -> items.create(new SzczegolZamowieniaCreate(1).ilosc(0), orderId));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              items.update(
                  lineId,
                  new SzczegolZamowieniaUpdate().idProduktu(1).ilosc(Integer.MAX_VALUE),
                  orderId));
      assertEquals(reservedStock, stock());
      assertEquals(prescriptionStock, products.get(3).getStanMagazynowy());
      assertEquals(1, items.list(orderId).size());
      assertEquals(1, items.get(lineId, orderId).getIdProduktu());
      assertEquals(order.getSumaCalkowita(), orders.get(orderId).getSumaCalkowita());
    } finally {
      orders.delete(orderId);
    }
  }
}
