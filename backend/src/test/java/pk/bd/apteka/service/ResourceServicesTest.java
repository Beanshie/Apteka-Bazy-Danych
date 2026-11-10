package pk.bd.apteka.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import pk.bd.apteka.dto.*;

class ResourceServicesTest {
  final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
  final KategoriaService categories = new KategoriaService(jdbc);
  final ProduktService products =
      new ProduktService(jdbc, mock(DostawcaService.class), mock(KategoriaService.class));

  @Test
  void invalidIdDoesNotQueryDatabase() {
    assertThrows(IllegalArgumentException.class, () -> products.get(0));
    verifyNoInteractions(jdbc);
  }

  @Test
  void missingRowThrowsHttpIndependentException() {
    when(jdbc.queryForObject(anyString(), anyMap(), any(RowMapper.class)))
        .thenThrow(new EmptyResultDataAccessException(1));
    assertThrows(NoSuchElementException.class, () -> products.get(9));
  }

  @Test
  void incompleteReplacementIsRejected() {
    when(jdbc.queryForObject(anyString(), anyMap(), any(RowMapper.class)))
        .thenReturn(new Kategoria());
    assertThrows(IllegalArgumentException.class, () -> categories.update(1, new KategoriaUpdate()));
    verify(jdbc, never()).update(anyString(), any(SqlParameterSource.class));
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
                (SqlParameterSource parameters) ->
                    parameters.hasValue("opis") && parameters.getValue("opis") == null));
  }

  @Test
  void negativePriceRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> products.create(new ProduktCreate(1, 1, "Test", new BigDecimal("-1"))));
    verifyNoInteractions(jdbc);
  }

  private KlientUpdate customer(String password) {
    return new KlientUpdate()
        .imie("Jan")
        .nazwisko("Nowak")
        .email("test@example.com")
        .haslo(password);
  }

  private void existingCustomer() {
    when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
    when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(new Klient());
  }

  @Test
  void nullPasswordPreservesHashAndClearsPhone() {
    existingCustomer();
    new KlientService(jdbc).update(1, customer(null));
    verify(jdbc)
        .update(
            contains("haslo=COALESCE(:haslo,haslo)"),
            argThat(
                (SqlParameterSource p) ->
                    p.hasValue("haslo")
                        && p.getValue("haslo") == null
                        && p.hasValue("telefon")
                        && p.getValue("telefon") == null));
  }

  @Test
  void replacementPasswordIsHashed() {
    existingCustomer();
    new KlientService(jdbc).update(1, customer("new-password"));
    verify(jdbc)
        .update(
            anyString(),
            argThat(
                (SqlParameterSource p) ->
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                        .matches("new-password", p.getValue("haslo").toString())));
  }

  @Test
  void blankPasswordIsRejected() {
    existingCustomer();
    assertThrows(
        IllegalArgumentException.class, () -> new KlientService(jdbc).update(1, customer(" ")));
    verify(jdbc, never()).update(anyString(), any(SqlParameterSource.class));
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
