package pk.bd.apteka.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pk.bd.apteka.dto.Klient;
import pk.bd.apteka.dto.KlientCreate;
import pk.bd.apteka.dto.KlientUpdate;

class KlientServiceTest {
  private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
  private final KlientService service = new KlientService(jdbc);

  @Test
  void invalidIdsNeverAccessDatabase() {
    for (Integer id : new Integer[] {null, 0, -1}) {
      assertThrows(IllegalArgumentException.class, () -> service.get(id));
      assertThrows(IllegalArgumentException.class, () -> service.delete(id));
      assertThrows(IllegalArgumentException.class, () -> service.update(id, null));
    }
    verifyNoInteractions(jdbc);
  }

  @Test
  void missingClientUsesDatabaseExceptionWithoutHttpStatus() {
    when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenThrow(new EmptyResultDataAccessException(1));
    assertThrows(java.util.NoSuchElementException.class, () -> service.get(7));
  }

  @Test
  void deleteExecutesOnlyDelete() {
    when(jdbc.update(anyString(), anyMap())).thenReturn(1);
    service.delete(7);
    verify(jdbc).update("DELETE FROM Klient WHERE id_klienta=:id", Map.of("id", 7));
    verifyNoMoreInteractions(jdbc);
  }

  @Test
  void missingDeleteSucceedsWithoutReading() {
    when(jdbc.update(anyString(), anyMap())).thenReturn(0);
    assertDoesNotThrow(() -> service.delete(7));
    verify(jdbc).update("DELETE FROM Klient WHERE id_klienta=:id", Map.of("id", 7));
    verifyNoMoreInteractions(jdbc);
  }

  @Test
  void createBindsHashedCopyAndReturnsPersistedClient() {
    var request =
        new KlientCreate()
            .imie("Jan")
            .nazwisko("Nowak")
            .email("jan@example.com")
            .haslo("secret")
            .telefon(null);
    var persisted =
        new Klient()
            .idKlienta(7)
            .dataRejestracji(java.time.OffsetDateTime.parse("2026-11-09T12:00:00Z"));
    doAnswer(
            call -> {
              SqlParameterSource parameters = call.getArgument(1);

              assertEquals("Jan", parameters.getValue("imie"));
              assertEquals("Nowak", parameters.getValue("nazwisko"));
              assertEquals("jan@example.com", parameters.getValue("email"));
              assertNull(parameters.getValue("telefon"));
              assertTrue(
                  new BCryptPasswordEncoder()
                      .matches("secret", (String) parameters.getValue("haslo")));
              KeyHolder keyHolder = call.getArgument(2);
              keyHolder.getKeyList().add(Map.of("id_klienta", 7));
              return 1;
            })
        .when(jdbc)
        .update(
            startsWith("INSERT INTO Klient"), any(SqlParameterSource.class),
            any(KeyHolder.class), aryEq(new String[] {"id_klienta"}));
    when(jdbc.queryForObject(
            anyString(),
            argThat((SqlParameterSource p) -> p.getValue("idKlienta").equals(7)),
            any(RowMapper.class)))
        .thenReturn(persisted);

    assertSame(persisted, service.create(request));
    assertEquals("secret", request.getHaslo());
    verify(jdbc)
        .queryForObject(
            anyString(),
            argThat((SqlParameterSource p) -> p.getValue("idKlienta").equals(7)),
            any(RowMapper.class));
  }

  @Test
  void updateHashesPasswordAndPreservesRequest() {
    var request =
        new KlientUpdate()
            .imie("Jan")
            .nazwisko("Nowak")
            .email("jan@example.com")
            .haslo("new-secret");
    var persisted = new Klient().idKlienta(7);
    when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
    when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(persisted);
    assertSame(persisted, service.update(7, request));
    assertEquals("new-secret", request.getHaslo());
    verify(jdbc)
        .update(
            startsWith("UPDATE Klient"),
            argThat(
                (SqlParameterSource p) ->
                    new BCryptPasswordEncoder()
                        .matches("new-secret", (String) p.getValue("haslo"))));
  }

  @Test
  void missingUpdateStopsBeforeReading() {
    when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(0);
    var request = new KlientUpdate().imie("Jan").nazwisko("Nowak").email("jan@example.com");
    assertThrows(java.util.NoSuchElementException.class, () -> service.update(7, request));
    verify(jdbc, never())
        .queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
  }

  @Test
  void invalidCreateNeverWrites() {
    assertThrows(IllegalArgumentException.class, () -> service.create(null));
    assertThrows(IllegalArgumentException.class, () -> service.create(new KlientCreate()));
    var value =
        new KlientCreate()
            .imie("Jan")
            .nazwisko("Nowak")
            .email("jan@example.com")
            .haslo("ą".repeat(37));
    assertThrows(IllegalArgumentException.class, () -> service.create(value));
    value.haslo("secret").imie(" ");
    assertThrows(IllegalArgumentException.class, () -> service.create(value));
    verifyNoInteractions(jdbc);
  }
}
