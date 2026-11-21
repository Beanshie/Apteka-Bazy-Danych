package pk.bd.apteka;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mariadb.MariaDBContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class SzczegolZamowieniaTriggersTest {
  @Container static final MariaDBContainer database = new MariaDBContainer("mariadb:11.4");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", database::getJdbcUrl);
    properties.add("spring.datasource.username", database::getUsername);
    properties.add("spring.datasource.password", database::getPassword);
  }

  @Autowired JdbcTemplate jdbc;

  private int order(boolean cancelled) {
    return jdbc.queryForObject(
        "INSERT INTO Zamowienie (id_klienta,id_adresu,status_zamowienia)"
            + " VALUES (1,1,?) RETURNING id_zamowienia",
        Integer.class, cancelled ? "ANULOWANE" : "NOWE");
  }

  private int insert(int order, int product, int quantity, BigDecimal price) {
    jdbc.update(
        "INSERT INTO Szczegol_Zamowienia (id_zamowienia,id_produktu,ilosc,cena_jednostkowa)"
            + " VALUES (?,?,?,?)", order, product, quantity, price);
    return jdbc.queryForObject(
        "SELECT id_szczegolu FROM Szczegol_Zamowienia WHERE id_zamowienia=? AND id_produktu=?",
        Integer.class, order, product);
  }

  private int stock(int product) {
    return jdbc.queryForObject(
        "SELECT stan_magazynowy FROM Produkt WHERE id_produktu=?", Integer.class, product);
  }

  private void assertTotal(int order, String expected) {
    assertEquals(new BigDecimal(expected), jdbc.queryForObject(
        "SELECT suma_calkowita FROM Zamowienie WHERE id_zamowienia=?", BigDecimal.class, order));
  }

  private record State(List<Map<String, Object>> orders, List<Map<String, Object>> items,
                       List<Map<String, Object>> products) {}

  private State state() {
    return new State(
        jdbc.queryForList("SELECT * FROM Zamowienie ORDER BY id_zamowienia"),
        jdbc.queryForList("SELECT * FROM Szczegol_Zamowienia ORDER BY id_szczegolu"),
        jdbc.queryForList("SELECT * FROM Produkt ORDER BY id_produktu"));
  }

  private void assertRejected(Integer businessCode, Runnable mutation) {
    var before = state();
    var error = assertThrows(DataIntegrityViolationException.class, mutation::run);
    if (businessCode != null) {
      var sql = assertInstanceOf(SQLException.class, error.getMostSpecificCause());
      assertEquals(businessCode.intValue(), sql.getErrorCode());
    }
    assertEquals(before, state());
  }

  @Test
  void flywayInstallsFourTriggersAndPreservesSeedReservations() {
    assertEquals(4, jdbc.queryForObject(
        "SELECT COUNT(*) FROM information_schema.TRIGGERS"
            + " WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_OBJECT_TABLE='Szczegol_Zamowienia'",
        Integer.class));
    assertTotal(1, "47.00");
    // V02 defines available stock; installing V03 must not deduct historical items again.
    assertEquals(150, stock(1));
  }

  @Test
  void insertsCopyMissingPriceAndPreserveExplicitSnapshots() {
    int order = order(false);
    int firstStock = stock(1), secondStock = stock(2);
    int line = insert(order, 1, 2, null);
    var price = jdbc.queryForObject("SELECT cena FROM Produkt WHERE id_produktu=1", BigDecimal.class);
    assertEquals(price, jdbc.queryForObject(
        "SELECT cena_jednostkowa FROM Szczegol_Zamowienia WHERE id_szczegolu=?",
        BigDecimal.class, line));
    insert(order, 2, 3, new BigDecimal("1.23"));
    assertEquals(firstStock - 2, stock(1));
    assertEquals(secondStock - 3, stock(2));
    assertTotal(order, price.multiply(BigDecimal.valueOf(2)).add(new BigDecimal("3.69")).toPlainString());
  }

  @Test
  void updatesApplyQuantityPriceAndProductDifferencesAndUnchangedUpdatesDoNothing() {
    int order = order(false);
    int firstStock = stock(1), secondStock = stock(2);
    int line = insert(order, 1, 2, new BigDecimal("4.00"));
    jdbc.update("UPDATE Szczegol_Zamowienia SET ilosc=3 WHERE id_szczegolu=?", line);
    assertEquals(firstStock - 3, stock(1));
    assertTotal(order, "12.00");
    jdbc.update("UPDATE Szczegol_Zamowienia SET ilosc=1,cena_jednostkowa=5 WHERE id_szczegolu=?", line);
    assertEquals(firstStock - 1, stock(1));
    assertTotal(order, "5.00");
    jdbc.update("UPDATE Szczegol_Zamowienia SET cena_jednostkowa=6 WHERE id_szczegolu=?", line);
    assertEquals(firstStock - 1, stock(1));
    assertTotal(order, "6.00");
    var before = state();
    jdbc.update("UPDATE Szczegol_Zamowienia SET ilosc=ilosc WHERE id_szczegolu=?", line);
    assertEquals(before, state());
    jdbc.update("UPDATE Szczegol_Zamowienia SET id_produktu=2,ilosc=4,cena_jednostkowa=2"
        + " WHERE id_szczegolu=?", line);
    assertEquals(firstStock, stock(1));
    assertEquals(secondStock - 4, stock(2));
    assertTotal(order, "8.00");
  }

  @Test
  void movingItemsBetweenOrdersUsesBothStatusesAndUpdatesBothTotals() {
    int first = order(false), second = order(false), cancelled = order(true);
    int firstStock = stock(1), secondStock = stock(2);
    int line = insert(first, 1, 2, new BigDecimal("5.00"));
    jdbc.update("UPDATE Szczegol_Zamowienia SET id_zamowienia=? WHERE id_szczegolu=?", second, line);
    assertTotal(first, "0.00");
    assertTotal(second, "10.00");
    assertEquals(firstStock - 2, stock(1));
    jdbc.update("UPDATE Szczegol_Zamowienia SET id_zamowienia=? WHERE id_szczegolu=?", cancelled, line);
    assertTotal(second, "0.00");
    assertTotal(cancelled, "10.00");
    assertEquals(firstStock, stock(1));
    jdbc.update("UPDATE Szczegol_Zamowienia SET ilosc=3 WHERE id_szczegolu=?", line);
    assertEquals(firstStock, stock(1));
    assertTotal(cancelled, "15.00");
    jdbc.update("UPDATE Szczegol_Zamowienia SET id_zamowienia=? WHERE id_szczegolu=?", first, line);
    assertEquals(firstStock - 3, stock(1));
    assertTotal(cancelled, "0.00");
    assertTotal(first, "15.00");
    jdbc.update("UPDATE Szczegol_Zamowienia SET id_zamowienia=?,id_produktu=2 WHERE id_szczegolu=?",
        cancelled, line);
    assertEquals(firstStock, stock(1));
    assertEquals(secondStock, stock(2));
    assertTotal(first, "0.00");
    assertTotal(cancelled, "15.00");
  }

  @Test
  void deletingFinalActiveOrCancelledItemZerosTotalAndRestoresOnlyActiveStock() {
    int active = order(false), cancelled = order(true);
    int initialStock = stock(1);
    int activeLine = insert(active, 1, 2, BigDecimal.ONE);
    int cancelledLine = insert(cancelled, 1, 3, BigDecimal.ONE);
    assertEquals(initialStock - 2, stock(1));
    jdbc.update("DELETE FROM Szczegol_Zamowienia WHERE id_szczegolu=?", cancelledLine);
    assertEquals(initialStock - 2, stock(1));
    assertTotal(cancelled, "0.00");
    jdbc.update("DELETE FROM Szczegol_Zamowienia WHERE id_szczegolu=?", activeLine);
    assertEquals(initialStock, stock(1));
    assertTotal(active, "0.00");
  }

  @Test
  void constraintsRejectInvalidReferencesQuantitiesAndNegativeStockWithoutChanges() {
    int order = order(false);
    assertRejected(null, () -> insert(order, Integer.MAX_VALUE, 1, null));
    assertRejected(null, () -> insert(order, Integer.MAX_VALUE, 1, BigDecimal.ONE));
    assertRejected(null, () -> insert(Integer.MAX_VALUE, 1, 1, BigDecimal.ONE));
    assertRejected(null, () -> insert(order, 1, 0, BigDecimal.ONE));
    assertRejected(null, () -> jdbc.update("UPDATE Produkt SET stan_magazynowy=-1 WHERE id_produktu=1"));
  }

  @Test
  void insufficientStockAndTotalOverflowHaveDistinctErrorsAndRollBack() {
    int order = order(false);
    int tooMany = stock(1) + 1;
    assertRejected(30001, () -> insert(order, 1, tooMany, BigDecimal.ONE));
    assertRejected(30002, () -> insert(order, 1, 2, new BigDecimal("99999999.99")));
    int line = insert(order, 1, 1, BigDecimal.ONE);
    assertRejected(30001, () -> jdbc.update(
        "UPDATE Szczegol_Zamowienia SET ilosc=? WHERE id_szczegolu=?", tooMany, line));
    assertRejected(30002, () -> jdbc.update(
        "UPDATE Szczegol_Zamowienia SET ilosc=2,cena_jednostkowa=99999999.99 WHERE id_szczegolu=?", line));
  }

  @Test
  void failedMultiRowInsertAndUpdateRollBackEarlierRowTriggerEffects() {
    int order = order(false);
    int tooMany = stock(2) + 1;
    assertRejected(30001, () -> jdbc.update(
        "INSERT INTO Szczegol_Zamowienia (id_zamowienia,id_produktu,ilosc,cena_jednostkowa)"
            + " VALUES (?,1,1,1),(?,2,?,1)", order, order, tooMany));
    insert(order, 1, 1, BigDecimal.ONE);
    insert(order, 2, 1, BigDecimal.ONE);
    assertRejected(30001, () -> jdbc.update(
        "UPDATE Szczegol_Zamowienia SET ilosc=CASE WHEN id_produktu=1 THEN 2 ELSE ? END"
            + " WHERE id_zamowienia=? ORDER BY id_szczegolu", tooMany, order));
  }
}
