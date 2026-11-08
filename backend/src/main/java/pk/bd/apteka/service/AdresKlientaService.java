package pk.bd.apteka.service;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pk.bd.apteka.dto.*;

@Service
public class AdresKlientaService {
  private final NamedParameterJdbcTemplate jdbc;
  private final KlientService klient;

  public AdresKlientaService(NamedParameterJdbcTemplate jdbc, KlientService klient) {
    this.jdbc = jdbc;
    this.klient = klient;
  }

  static final RowMapper<AdresKlienta> ROW_MAPPER =
      (rs, index) -> {
        var value = new AdresKlienta();
        value.setIdAdresu(rs.getObject("id_adresu", Integer.class));
        value.setIdKlienta(rs.getObject("id_klienta", Integer.class));
        value.setUlica(rs.getString("ulica"));
        value.setNumerDomu(rs.getString("numer_domu"));
        value.setNumerMieszkania(rs.getString("numer_mieszkania"));
        value.setKodPocztowy(rs.getString("kod_pocztowy"));
        value.setMiasto(rs.getString("miasto"));
        return value;
      };

  AdresKlienta read(Integer id, boolean lock) {
    requireId(id);
    try {
      return jdbc.queryForObject(
          "SELECT id_adresu,id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto FROM"
              + " Adres_Klienta WHERE id_adresu=:id"
              + (lock ? " FOR UPDATE" : ""),
          params("id", id),
          ROW_MAPPER);
    } catch (EmptyResultDataAccessException error) {
      throw missing();
    }
  }

  public AdresKlienta get(Integer id) {
    return read(id, false);
  }

  public List<AdresKlienta> list(Integer idKlienta) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT id_adresu,id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto FROM"
                + " Adres_Klienta WHERE 1=1");
    Map<String, Object> parameters = new LinkedHashMap<>();
    if (idKlienta != null) {
      sql.append(" AND id_klienta=:id_klienta");
      parameters.put("id_klienta", idKlienta);
    }
    sql.append(" ORDER BY id_adresu");
    var result = jdbc.query(sql.toString(), parameters, ROW_MAPPER);
    return result;
  }

  private void validate(AdresKlientaCreate value) {
    requireValue(value, "body");
    requireValue(value.getIdKlienta(), "id_klienta");
    if (value.getIdKlienta() != null) checkNumber(value.getIdKlienta(), 1, false);
    if (value.getIdKlienta() != null) klient.read(value.getIdKlienta(), false);
    requireValue(value.getUlica(), "ulica");
    if (value.getUlica() != null) checkText(value.getUlica(), 100, true);
    requireValue(value.getNumerDomu(), "numer_domu");
    if (value.getNumerDomu() != null) checkText(value.getNumerDomu(), 10, true);
    if (value.getNumerMieszkania() != null) checkText(value.getNumerMieszkania(), 10, false);
    requireValue(value.getKodPocztowy(), "kod_pocztowy");
    if (value.getKodPocztowy() != null) checkText(value.getKodPocztowy(), 10, true);
    requireValue(value.getMiasto(), "miasto");
    if (value.getMiasto() != null) checkText(value.getMiasto(), 50, true);
  }

  private Map<String, Object> parameters(AdresKlientaCreate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id_klienta", value.getIdKlienta());
    result.put("ulica", value.getUlica());
    result.put("numer_domu", value.getNumerDomu());
    result.put("numer_mieszkania", value.getNumerMieszkania());
    result.put("kod_pocztowy", value.getKodPocztowy());
    result.put("miasto", value.getMiasto());
    return result;
  }

  private void validate(AdresKlientaUpdate value) {
    requireValue(value, "body");
    requireValue(value.getIdKlienta(), "id_klienta");
    if (value.getIdKlienta() != null) checkNumber(value.getIdKlienta(), 1, false);
    if (value.getIdKlienta() != null) klient.read(value.getIdKlienta(), false);
    requireValue(value.getUlica(), "ulica");
    if (value.getUlica() != null) checkText(value.getUlica(), 100, true);
    requireValue(value.getNumerDomu(), "numer_domu");
    if (value.getNumerDomu() != null) checkText(value.getNumerDomu(), 10, true);
    if (value.getNumerMieszkania() != null) checkText(value.getNumerMieszkania(), 10, false);
    requireValue(value.getKodPocztowy(), "kod_pocztowy");
    if (value.getKodPocztowy() != null) checkText(value.getKodPocztowy(), 10, true);
    requireValue(value.getMiasto(), "miasto");
    if (value.getMiasto() != null) checkText(value.getMiasto(), 50, true);
  }

  private Map<String, Object> parameters(AdresKlientaUpdate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id_klienta", value.getIdKlienta());
    result.put("ulica", value.getUlica());
    result.put("numer_domu", value.getNumerDomu());
    result.put("numer_mieszkania", value.getNumerMieszkania());
    result.put("kod_pocztowy", value.getKodPocztowy());
    result.put("miasto", value.getMiasto());
    return result;
  }

  private int insert(Map<String, Object> data) {
    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Adres_Klienta"
            + " (id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto) VALUES"
            + " (:id_klienta,:ulica,:numer_domu,:numer_mieszkania,:kod_pocztowy,:miasto)",
        new MapSqlParameterSource(data),
        key,
        new String[] {"id_adresu"});
    return Objects.requireNonNull(key.getKey()).intValue();
  }

  private void write(int id, Map<String, Object> data) {
    Map<String, Object> parameters = new LinkedHashMap<>(data);
    parameters.put("_id", id);
    jdbc.update(
        "UPDATE Adres_Klienta SET"
            + " id_klienta=:id_klienta,ulica=:ulica,numer_domu=:numer_domu,numer_mieszkania=:numer_mieszkania,kod_pocztowy=:kod_pocztowy,miasto=:miasto"
            + " WHERE id_adresu=:_id",
        parameters);
  }

  @Transactional
  public AdresKlienta create(AdresKlientaCreate value) {
    int id = insert(parameters(value));
    return get(id);
  }

  @Transactional
  public AdresKlienta update(Integer id, AdresKlientaUpdate value) {
    read(id, true);
    write(id, parameters(value));
    ownership(id);
    return get(id);
  }

  @Transactional
  public void delete(Integer id) {
    read(id, true);
    jdbc.update("DELETE FROM Adres_Klienta WHERE id_adresu=:id", params("id", id));
  }

  private void ownership(int id) {
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM Zamowienie z JOIN Adres_Klienta a ON a.id_adresu=z.id_adresu"
                + " WHERE a.id_adresu=:id AND a.id_klienta<>z.id_klienta",
            params("id", id),
            Integer.class)
        > 0) throw bad("Adres należy do innego klienta");
  }

  private static ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private static ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono rekordu");
  }

  private static void requireValue(Object value, String field) {
    if (value == null) throw bad("Pole nie może być null: " + field);
  }

  private static void requireId(Integer id) {
    if (id == null || id < 1) throw bad("Niepoprawne ID");
  }

  private static Map<String, Object> params(Object... pairs) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
    return m;
  }

  private static void checkText(String text, int max, boolean required) {
    if (required && text.isBlank()) throw bad("Puste pole");
    if (max > 0 && text.length() > max) throw bad("Zbyt długie pole");
  }

  private static void checkNumber(Number value, int min, boolean money) {
    BigDecimal d = new BigDecimal(value.toString());
    if (d.compareTo(BigDecimal.valueOf(min)) < 0) throw bad("Niepoprawna liczba");
    if (money
        && (d.stripTrailingZeros().scale() > 2 || d.compareTo(new BigDecimal("99999999.99")) > 0))
      throw bad("Niepoprawna cena");
  }
}
