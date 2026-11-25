package pk.bd.apteka;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mariadb.MariaDBContainer;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ServiceCleanupTest {
  @Container static final MariaDBContainer database = new MariaDBContainer("mariadb:11.4");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", database::getJdbcUrl);
    properties.add("spring.datasource.username", database::getUsername);
    properties.add("spring.datasource.password", database::getPassword);
  }

  @Autowired PracownikService employees;
  @Autowired ProduktService products;
  @Autowired ReceptaService prescriptions;
  @Autowired PlatnoscService payments;
  @Autowired ZamowienieService orders;
  @Autowired SzczegolZamowieniaService items;
  @Autowired AdresKlientaService addresses;
  @Autowired KategoriaService categories;
  @Autowired DostawcaService suppliers;
  @Autowired KlientService customers;
  @Autowired JdbcTemplate jdbc;

  private Pracownik employee;
  private Produkt product;
  private Recepta prescription;
  private Zamowienie order;
  private Platnosc payment;

  @BeforeEach
  void createResources() {
    employee = employees.create(
        new PracownikCreate("cleanup-" + UUID.randomUUID(), "secret", "Jan", "Nowak"));
    product = products.create(
        new ProduktCreate(1, 1, "Cleanup product", BigDecimal.TEN).stanMagazynowy(10));
    prescription = prescriptions.create(new ReceptaCreate(1, "cleanup", "12345678901"));
    order = orders.create(new ZamowienieCreate(1, 1));
    payment = payments.create(
        new PlatnoscCreate(order.getIdZamowienia(), "KARTA").statusPlatnosci(null));
  }

  @AfterEach
  void deleteResources() {
    if (payment != null) payments.delete(payment.getIdPlatnosci());
    if (order != null) orders.delete(order.getIdZamowienia());
    if (prescription != null) prescriptions.delete(prescription.getIdRecepty());
    if (product != null) products.delete(product.getIdProduktu());
    if (employee != null) employees.delete(employee.getIdPracownika());
  }

  private PracownikUpdate employeeUpdate() {
    return new PracownikUpdate()
        .login(employee.getLogin()).imie(employee.getImie()).nazwisko(employee.getNazwisko())
        .rola(PracownikUpdate.RolaEnum.fromValue(employee.getRola().getValue()))
        .aktywny(employee.getAktywny());
  }

  private ProduktUpdate productUpdate() {
    return new ProduktUpdate()
        .idKategorii(product.getIdKategorii()).idDostawcy(product.getIdDostawcy())
        .nazwaProduktu(product.getNazwaProduktu()).cena(product.getCena())
        .stanMagazynowy(product.getStanMagazynowy()).wymagaRecepty(product.getWymagaRecepty());
  }

  private ReceptaUpdate prescriptionUpdate() {
    return new ReceptaUpdate()
        .idKlienta(prescription.getIdKlienta()).kodRecepty(prescription.getKodRecepty())
        .peselPacjenta(prescription.getPeselPacjenta())
        .statusRecepty(ReceptaUpdate.StatusReceptyEnum.fromValue(
            prescription.getStatusRecepty().getValue()));
  }

  private PlatnoscUpdate paymentUpdate() {
    return new PlatnoscUpdate()
        .idZamowienia(payment.getIdZamowienia()).metodaPlatnosci(payment.getMetodaPlatnosci())
        .statusPlatnosci(PlatnoscUpdate.StatusPlatnosciEnum.fromValue(
            payment.getStatusPlatnosci().getValue()))
        .dataPlatnosci(payment.getDataPlatnosci());
  }

  private String passwordHash() {
    return jdbc.queryForObject(
        "SELECT haslo FROM Pracownik WHERE id_pracownika=?", String.class, employee.getIdPracownika());
  }

  private List<List<Map<String, Object>>> databaseState() {
    return List.of(
        jdbc.queryForList("SELECT * FROM Kategoria ORDER BY id_kategorii"),
        jdbc.queryForList("SELECT * FROM Dostawca ORDER BY id_dostawcy"),
        jdbc.queryForList("SELECT * FROM Klient ORDER BY id_klienta"),
        jdbc.queryForList("SELECT * FROM Adres_Klienta ORDER BY id_adresu"),
        jdbc.queryForList("SELECT * FROM Pracownik ORDER BY id_pracownika"),
        jdbc.queryForList("SELECT * FROM Produkt ORDER BY id_produktu"),
        jdbc.queryForList("SELECT * FROM Recepta ORDER BY id_recepty"),
        jdbc.queryForList("SELECT * FROM Platnosc ORDER BY id_platnosci"),
        jdbc.queryForList("SELECT * FROM Zamowienie ORDER BY id_zamowienia"),
        jdbc.queryForList("SELECT * FROM Szczegol_Zamowienia ORDER BY id_szczegolu"));
  }

  private void assertRejected(Class<? extends Throwable> type, Runnable mutation) {
    var before = databaseState();
    assertThrows(type, mutation::run);
    assertEquals(before, databaseState());
  }

  @Test
  void unchangedUpdatesSucceedAndPreserveDatabaseState() {
    var before = databaseState();
    assertEquals(employee, employees.update(employee.getIdPracownika(), employeeUpdate()));
    assertEquals(product, products.update(product.getIdProduktu(), productUpdate()));
    assertEquals(prescription, prescriptions.update(prescription.getIdRecepty(), prescriptionUpdate()));
    assertEquals(payment, payments.update(payment.getIdPlatnosci(), paymentUpdate()));
    assertEquals(before, databaseState());
  }

  @Test
  void missingUpdatesAndMismatchedPrescriptionOwnerLeaveRecordsUnchanged() {
    assertRejected(NoSuchElementException.class,
        () -> employees.update(Integer.MAX_VALUE, employeeUpdate()));
    assertRejected(NoSuchElementException.class,
        () -> products.update(Integer.MAX_VALUE, productUpdate()));
    assertRejected(NoSuchElementException.class,
        () -> prescriptions.update(Integer.MAX_VALUE, prescriptionUpdate()));
    assertRejected(NoSuchElementException.class,
        () -> payments.update(Integer.MAX_VALUE, paymentUpdate()));
    assertRejected(NoSuchElementException.class,
        () -> prescriptions.update(prescription.getIdRecepty(), prescriptionUpdate().idKlienta(2)));
  }

  @Test
  void employeePasswordIsPreservedWhenOmittedAndHashedWhenReplaced() {
    String originalHash = passwordHash();
    employees.update(employee.getIdPracownika(), employeeUpdate());
    assertEquals(originalHash, passwordHash());

    var request = employeeUpdate().haslo("replacement-secret");
    employees.update(employee.getIdPracownika(), request);
    String replacementHash = passwordHash();
    assertNotEquals(originalHash, replacementHash);
    assertTrue(new BCryptPasswordEncoder().matches("replacement-secret", replacementHash));
    assertEquals("replacement-secret", request.getHaslo());

    employees.update(employee.getIdPracownika(), employeeUpdate().haslo(null));
    assertEquals(replacementHash, passwordHash());
  }

  @Test
  void paymentGetAndListMapUtcTimestampsAndNullableDates() {
    assertEquals(Platnosc.StatusPlatnosciEnum.OCZEKUJACA, payment.getStatusPlatnosci());
    assertNull(payment.getDataPlatnosci());
    var timestamp = OffsetDateTime.parse("2026-11-17T12:30:00+02:00");
    var updated = payments.update(payment.getIdPlatnosci(), paymentUpdate()
        .statusPlatnosci(PlatnoscUpdate.StatusPlatnosciEnum.OPLACONA).dataPlatnosci(timestamp));
    assertEquals(timestamp.withOffsetSameInstant(ZoneOffset.UTC), updated.getDataPlatnosci());
    assertEquals(Platnosc.StatusPlatnosciEnum.OPLACONA, updated.getStatusPlatnosci());
    assertEquals(updated, payments.get(payment.getIdPlatnosci()));
    assertEquals(List.of(updated), payments.list(order.getIdZamowienia()));

    var cleared = payments.update(payment.getIdPlatnosci(), paymentUpdate().dataPlatnosci(null));
    assertNull(cleared.getDataPlatnosci());
    assertEquals(List.of(cleared), payments.list(order.getIdZamowienia()));
  }

  @Test
  void paymentCanMoveToAnotherOrderButCannotDuplicateAnExistingPayment() {
    var otherOrder = orders.create(
        new ZamowienieCreate(1, 1));
    try {
      var moved = payments.update(payment.getIdPlatnosci(),
          paymentUpdate().idZamowienia(otherOrder.getIdZamowienia()));
      assertEquals(otherOrder.getIdZamowienia(), moved.getIdZamowienia());
      assertTrue(payments.list(order.getIdZamowienia()).isEmpty());
      assertEquals(List.of(moved), payments.list(otherOrder.getIdZamowienia()));
      assertRejected(DataIntegrityViolationException.class,
          () -> payments.update(payment.getIdPlatnosci(), paymentUpdate().idZamowienia(1)));
    } finally {
      payments.delete(payment.getIdPlatnosci());
      orders.delete(otherOrder.getIdZamowienia());
    }
  }

  @Test
  void missingUpdateEnumsAreRejectedByDatabaseAndRollBackOtherFields() {
    assertRejected(DataIntegrityViolationException.class,
        () -> employees.update(employee.getIdPracownika(), employeeUpdate().imie("Changed").rola(null)));
    assertRejected(DataIntegrityViolationException.class,
        () -> prescriptions.update(prescription.getIdRecepty(),
            prescriptionUpdate().kodRecepty("changed").statusRecepty(null)));
    assertRejected(DataIntegrityViolationException.class,
        () -> payments.update(payment.getIdPlatnosci(),
            paymentUpdate().metodaPlatnosci("Changed").statusPlatnosci(null)));
  }

  @Test
  void databaseConstraintsRejectInvalidScalarValuesWithoutPartialUpdates() {
    assertRejected(IllegalArgumentException.class,
        () -> products.create(new ProduktCreate(1, 1, "Invalid", BigDecimal.ONE.negate())));
    assertRejected(DataIntegrityViolationException.class,
        () -> products.update(product.getIdProduktu(), productUpdate().cena(BigDecimal.ONE.negate())));
    assertRejected(DataIntegrityViolationException.class,
        () -> products.update(product.getIdProduktu(), productUpdate().stanMagazynowy(-1)));
    assertRejected(DataIntegrityViolationException.class,
        () -> products.update(product.getIdProduktu(), productUpdate().nazwaProduktu(null)));
    assertRejected(DataIntegrityViolationException.class,
        () -> products.update(product.getIdProduktu(), productUpdate().nazwaProduktu("x".repeat(101))));
    assertRejected(DataIntegrityViolationException.class,
        () -> employees.update(employee.getIdPracownika(), employeeUpdate().login("admin_apteka")));
    assertRejected(DataIntegrityViolationException.class,
        () -> prescriptions.update(prescription.getIdRecepty(), prescriptionUpdate().kodRecepty(null)));
    assertRejected(DataIntegrityViolationException.class,
        () -> payments.update(payment.getIdPlatnosci(), paymentUpdate().metodaPlatnosci(null)));
  }

  @Test
  void allCreatesTranslateConstraintFailuresAndRollBack() {
    List<Runnable> invalidCreates = List.of(
        () -> categories.create(new KategoriaCreate().nazwaKategorii("Leki przeciwbólowe")),
        () -> suppliers.create(new DostawcaCreate("Duplicate NIP").nip("6340024921")),
        () -> customers.create(new KlientCreate().imie("Jan").nazwisko("Nowak")
            .email("j.kowalski@example.com").haslo("secret")),
        () -> employees.create(new PracownikCreate(employee.getLogin(), "secret", "Jan", "Nowak")),
        () -> addresses.create(
            new AdresKlientaCreate(Integer.MAX_VALUE, "Testowa", "1", "00-001", "Warszawa")),
        () -> products.create(new ProduktCreate(Integer.MAX_VALUE, 1, "Missing category", BigDecimal.ONE)),
        () -> products.create(new ProduktCreate(1, Integer.MAX_VALUE, "Missing supplier", BigDecimal.ONE)),
        () -> prescriptions.create(new ReceptaCreate(Integer.MAX_VALUE, "missing", "12345678901")),
        () -> payments.create(new PlatnoscCreate(Integer.MAX_VALUE, "KARTA")),
        () -> payments.create(new PlatnoscCreate(order.getIdZamowienia(), "KARTA")),
        () -> orders.create(new ZamowienieCreate(Integer.MAX_VALUE, 1)),
        () -> items.create(new SzczegolZamowieniaCreate(2).ilosc(0), order.getIdZamowienia()));
    for (var create : invalidCreates) {
      var before = databaseState();
      var error = assertThrows(IllegalArgumentException.class, create::run);
      assertEquals("Błąd integralności danych", error.getMessage());
      assertInstanceOf(DataIntegrityViolationException.class, error.getCause());
      assertEquals(before, databaseState());
    }
  }

  @Test
  void foreignRecordLookupsAndInputGuardsRetainTheirErrors() {
    assertRejected(NoSuchElementException.class,
        () -> products.update(product.getIdProduktu(), productUpdate().idKategorii(Integer.MAX_VALUE)));
    assertRejected(NoSuchElementException.class,
        () -> products.update(product.getIdProduktu(), productUpdate().idDostawcy(Integer.MAX_VALUE)));
    assertRejected(NoSuchElementException.class,
        () -> prescriptions.update(prescription.getIdRecepty(),
            prescriptionUpdate().idKlienta(Integer.MAX_VALUE)));
    assertRejected(NoSuchElementException.class,
        () -> payments.update(payment.getIdPlatnosci(), paymentUpdate().idZamowienia(Integer.MAX_VALUE)));
    assertRejected(IllegalArgumentException.class, () -> employees.update(employee.getIdPracownika(), null));
    assertRejected(IllegalArgumentException.class, () -> products.update(product.getIdProduktu(), null));
    assertRejected(IllegalArgumentException.class, () -> prescriptions.update(prescription.getIdRecepty(), null));
    assertRejected(IllegalArgumentException.class, () -> payments.update(payment.getIdPlatnosci(), null));
    assertRejected(IllegalArgumentException.class, () -> employees.update(0, employeeUpdate()));
    assertRejected(IllegalArgumentException.class, () -> products.update(0, productUpdate()));
    assertRejected(IllegalArgumentException.class, () -> prescriptions.update(0, prescriptionUpdate()));
    assertRejected(IllegalArgumentException.class, () -> payments.update(0, paymentUpdate()));
  }
}
