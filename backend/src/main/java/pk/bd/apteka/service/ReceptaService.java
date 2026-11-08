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
public class ReceptaService {
  private final NamedParameterJdbcTemplate jdbc;
  private final KlientService klient;

  public ReceptaService(NamedParameterJdbcTemplate jdbc, KlientService klient) {
    this.jdbc = jdbc;
    this.klient = klient;
  }

  static final RowMapper<Recepta> ROW_MAPPER =
      (rs, index) -> {
        var value = new Recepta();
        value.setIdRecepty(rs.getObject("id_recepty", Integer.class));
        value.setIdKlienta(rs.getObject("id_klienta", Integer.class));
        value.setKodRecepty(rs.getString("kod_recepty"));
        value.setPeselPacjenta(rs.getString("pesel_pacjenta"));
        value.setStatusRecepty(Recepta.StatusReceptyEnum.fromValue(rs.getString("status_recepty")));
        return value;
      };

  Recepta read(Integer id, boolean lock) {
    requireId(id);
    try {
      return jdbc.queryForObject(
          "SELECT id_recepty,id_klienta,kod_recepty,pesel_pacjenta,status_recepty FROM Recepta"
              + " WHERE id_recepty=:id"
              + (lock ? " FOR UPDATE" : ""),
          params("id", id),
          ROW_MAPPER);
    } catch (EmptyResultDataAccessException error) {
      throw missing();
    }
  }

  public Recepta get(Integer id) {
    return read(id, false);
  }

  public List<Recepta> list(Integer idKlienta, String kodRecepty, String statusRecepty) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT id_recepty,id_klienta,kod_recepty,pesel_pacjenta,status_recepty FROM Recepta"
                + " WHERE 1=1");
    Map<String, Object> parameters = new LinkedHashMap<>();
    if (idKlienta != null) {
      sql.append(" AND id_klienta=:id_klienta");
      parameters.put("id_klienta", idKlienta);
    }
    if (kodRecepty != null) {
      sql.append(" AND kod_recepty=:kod_recepty");
      parameters.put("kod_recepty", kodRecepty);
    }
    if (statusRecepty != null) {
      sql.append(" AND status_recepty=:status_recepty");
      parameters.put("status_recepty", statusRecepty);
    }
    sql.append(" ORDER BY id_recepty");
    var result = jdbc.query(sql.toString(), parameters, ROW_MAPPER);
    return result;
  }

  private void validate(ReceptaCreate value) {
    requireValue(value, "body");
    requireValue(value.getIdKlienta(), "id_klienta");
    if (value.getIdKlienta() != null) checkNumber(value.getIdKlienta(), 1, false);
    if (value.getIdKlienta() != null) klient.read(value.getIdKlienta(), false);
    requireValue(value.getKodRecepty(), "kod_recepty");
    if (value.getKodRecepty() != null) checkText(value.getKodRecepty(), 10, true);
    requireValue(value.getPeselPacjenta(), "pesel_pacjenta");
    if (value.getPeselPacjenta() != null) checkText(value.getPeselPacjenta(), 11, true);
  }

  private Map<String, Object> parameters(ReceptaCreate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id_klienta", value.getIdKlienta());
    result.put("kod_recepty", value.getKodRecepty());
    result.put("pesel_pacjenta", value.getPeselPacjenta());
    result.put(
        "status_recepty",
        value.getStatusRecepty() == null ? "NOWA" : (value.getStatusRecepty().getValue()));
    return result;
  }

  private void validate(ReceptaUpdate value) {
    requireValue(value, "body");
    requireValue(value.getIdKlienta(), "id_klienta");
    if (value.getIdKlienta() != null) checkNumber(value.getIdKlienta(), 1, false);
    if (value.getIdKlienta() != null) klient.read(value.getIdKlienta(), false);
    requireValue(value.getKodRecepty(), "kod_recepty");
    if (value.getKodRecepty() != null) checkText(value.getKodRecepty(), 10, true);
    requireValue(value.getPeselPacjenta(), "pesel_pacjenta");
    if (value.getPeselPacjenta() != null) checkText(value.getPeselPacjenta(), 11, true);
    requireValue(value.getStatusRecepty(), "status_recepty");
  }

  private Map<String, Object> parameters(ReceptaUpdate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id_klienta", value.getIdKlienta());
    result.put("kod_recepty", value.getKodRecepty());
    result.put("pesel_pacjenta", value.getPeselPacjenta());
    result.put(
        "status_recepty",
        value.getStatusRecepty() == null ? null : value.getStatusRecepty().getValue());
    return result;
  }

  private int insert(Map<String, Object> data) {
    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Recepta (id_klienta,kod_recepty,pesel_pacjenta,status_recepty) VALUES"
            + " (:id_klienta,:kod_recepty,:pesel_pacjenta,:status_recepty)",
        new MapSqlParameterSource(data),
        key,
        new String[] {"id_recepty"});
    return Objects.requireNonNull(key.getKey()).intValue();
  }

  private void write(int id, Map<String, Object> data) {
    Map<String, Object> parameters = new LinkedHashMap<>(data);
    parameters.put("_id", id);
    jdbc.update(
        "UPDATE Recepta SET"
            + " id_klienta=:id_klienta,kod_recepty=:kod_recepty,pesel_pacjenta=:pesel_pacjenta,status_recepty=:status_recepty"
            + " WHERE id_recepty=:_id",
        parameters);
  }

  @Transactional
  public Recepta create(ReceptaCreate value) {
    int id = insert(parameters(value));
    return get(id);
  }

  @Transactional
  public Recepta update(Integer id, ReceptaUpdate value) {
    read(id, true);
    write(id, parameters(value));
    ownership(id);
    return get(id);
  }

  @Transactional
  public void delete(Integer id) {
    read(id, true);
    jdbc.update("DELETE FROM Recepta WHERE id_recepty=:id", params("id", id));
  }

  private void ownership(int id) {
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM Szczegol_Zamowienia s JOIN Zamowienie z ON"
                + " z.id_zamowienia=s.id_zamowienia JOIN Recepta r ON r.id_recepty=s.id_recepty"
                + " WHERE r.id_recepty=:id AND r.id_klienta<>z.id_klienta",
            params("id", id),
            Integer.class)
        > 0) throw bad("Recepta należy do innego klienta");
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
