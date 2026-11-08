package pk.bd.apteka.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import pk.bd.apteka.dto.*;

class ResourceServicesTest {
  final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
  final KategoriaService categories = new KategoriaService(jdbc);
  final ProduktService products =
      new ProduktService(jdbc, mock(DostawcaService.class), mock(KategoriaService.class));

  @Test
  void invalidIdDoesNotQueryDatabase() {
    assertThrows(ResponseStatusException.class, () -> products.get(0));
    verifyNoInteractions(jdbc);
  }

  @Test
  void missingRowReturns404() {
    when(jdbc.queryForObject(anyString(), anyMap(), any(RowMapper.class)))
        .thenThrow(new EmptyResultDataAccessException(1));
    assertEquals(
        404,
        assertThrows(ResponseStatusException.class, () -> products.get(9)).getStatusCode().value());
  }

  @Test
  void incompleteReplacementIsRejected() {
    when(jdbc.queryForObject(anyString(), anyMap(), any(RowMapper.class)))
        .thenReturn(new Kategoria());
    assertThrows(ResponseStatusException.class, () -> categories.update(1, new KategoriaUpdate()));
    verify(jdbc, never()).update(anyString(), anyMap());
  }

  @Test
  void replacementClearsNullableField() {
    when(jdbc.queryForObject(anyString(), anyMap(), any(RowMapper.class)))
        .thenReturn(new Kategoria());
    categories.update(1, new KategoriaUpdate().nazwaKategorii("Changed"));
    verify(jdbc)
        .update(
            contains("opis=:opis"),
            argThat(
                (Map<String, ?> parameters) ->
                    parameters.containsKey("opis") && parameters.get("opis") == null));
  }

  @Test
  void negativePriceRejected() {
    assertThrows(
        ResponseStatusException.class,
        () -> products.create(new ProduktCreate(1, 1, "Test", new BigDecimal("-1"))));
    verifyNoInteractions(jdbc);
  }

  @Test
  void stockReservationIsConditionalAndRejectsShortage() {
    when(jdbc.update(anyString(), anyMap())).thenReturn(0);
    assertEquals(
        400,
        assertThrows(
                ResponseStatusException.class,
                () -> products.reserve(List.of(new SzczegolZamowienia().idProduktu(1).ilosc(5))))
            .getStatusCode()
            .value());
    verify(jdbc)
        .update(
            contains("stan_magazynowy>=:q"),
            argThat((Map<String, ?> m) -> m.get("q").equals(5) && m.get("p").equals(1)));
  }

  private KlientUpdate customer(String password) {
    return new KlientUpdate()
        .imie("Jan")
        .nazwisko("Nowak")
        .email("test@example.com")
        .haslo(password);
  }

  private void existingCustomer() {
    when(jdbc.queryForObject(anyString(), anyMap(), any(RowMapper.class))).thenReturn(new Klient());
  }

  @Test
  void nullPasswordPreservesHashAndClearsPhone() {
    existingCustomer();
    new KlientService(jdbc).update(1, customer(null));
    verify(jdbc)
        .update(
            contains("haslo=COALESCE(:haslo,haslo)"),
            argThat(
                (Map<String, ?> p) ->
                    p.containsKey("haslo")
                        && p.get("haslo") == null
                        && p.containsKey("telefon")
                        && p.get("telefon") == null));
  }

  @Test
  void replacementPasswordIsHashed() {
    existingCustomer();
    new KlientService(jdbc).update(1, customer("new-password"));
    verify(jdbc)
        .update(
            anyString(),
            argThat(
                (Map<String, ?> p) ->
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                        .matches("new-password", p.get("haslo").toString())));
  }

  @Test
  void blankPasswordIsRejected() {
    existingCustomer();
    assertThrows(
        ResponseStatusException.class, () -> new KlientService(jdbc).update(1, customer(" ")));
    verify(jdbc, never()).update(anyString(), anyMap());
  }

  @Test
  void typedFiltersUseBoundParameters() {
    when(jdbc.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(List.of());
    products.list(2, 3);
    verify(jdbc)
        .query(
            contains("AND id_kategorii=:id_kategorii"),
            argThat((Map<String, ?> p) -> p.equals(Map.of("id_kategorii", 2, "id_dostawcy", 3))),
            any(RowMapper.class));
  }

  @Test
  void employeeMapperConvertsEnumsAndFlags() throws Exception {
    var rs = mock(ResultSet.class);
    when(rs.getString("rola")).thenReturn("KIEROWNIK");
    when(rs.getBoolean("aktywny")).thenReturn(true);
    var employee = PracownikService.ROW_MAPPER.mapRow(rs, 0);
    assertEquals(Pracownik.RolaEnum.KIEROWNIK, employee.getRola());
    assertTrue(employee.getAktywny());
    verify(rs, never()).getString("haslo");
  }

  @Test
  void itemMapperPreservesMoneyAndNullPrescription() throws Exception {
    var rs = mock(ResultSet.class);
    when(rs.getBigDecimal("cena_jednostkowa")).thenReturn(new BigDecimal("12.50"));
    var item = SzczegolZamowieniaService.ROW_MAPPER.mapRow(rs, 0);
    assertNull(item.getIdRecepty());
    assertEquals(new BigDecimal("12.50"), item.getCenaJednostkowa());
  }

  @Test
  void customerMapperUsesUtc() throws Exception {
    var rs = mock(ResultSet.class);
    when(rs.getTimestamp("data_rejestracji")).thenReturn(Timestamp.valueOf("2026-11-08 12:00:00"));
    var value = KlientService.ROW_MAPPER.mapRow(rs, 0);
    assertEquals("2026-11-08T12:00Z", value.getDataRejestracji().toString());
  }

  @Test
  void explicitInsertSuppliesStockDefaults() {
    when(jdbc.queryForObject(anyString(), anyMap(), any(RowMapper.class)))
        .thenReturn(new Produkt());
    doAnswer(
            call -> {
              org.springframework.jdbc.support.KeyHolder key = call.getArgument(2);
              key.getKeyList().add(Map.of("id_produktu", 7));
              return 1;
            })
        .when(jdbc)
        .update(
            anyString(),
            any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class),
            any(org.springframework.jdbc.support.KeyHolder.class),
            any(String[].class));
    products.create(
        new ProduktCreate(1, 1, "Test", BigDecimal.ONE).stanMagazynowy(null).wymagaRecepty(null));
    verify(jdbc)
        .update(
            contains(
                "INSERT INTO Produkt"
                    + " (id_kategorii,id_dostawcy,nazwa_produktu,cena,stan_magazynowy,wymaga_recepty)"),
            argThat(
                (org.springframework.jdbc.core.namedparam.SqlParameterSource params) ->
                    params.getValue("stan_magazynowy").equals(0)
                        && params.getValue("wymaga_recepty").equals(false)),
            any(org.springframework.jdbc.support.KeyHolder.class),
            any(String[].class));
  }
}
