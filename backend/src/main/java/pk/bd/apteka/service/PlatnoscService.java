package pk.bd.apteka.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
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
public class PlatnoscService {
  private final NamedParameterJdbcTemplate jdbc;
  private final ZamowienieService zamowienie;

  public PlatnoscService(NamedParameterJdbcTemplate jdbc, ZamowienieService zamowienie) {
    this.jdbc = jdbc;
    this.zamowienie = zamowienie;
  }

  static final RowMapper<Platnosc> ROW_MAPPER =
      (rs, index) -> {
        var value = new Platnosc();
        value.setIdPlatnosci(rs.getObject("id_platnosci", Integer.class));
        value.setIdZamowienia(rs.getObject("id_zamowienia", Integer.class));
        value.setMetodaPlatnosci(rs.getString("metoda_platnosci"));
        value.setStatusPlatnosci(
            Platnosc.StatusPlatnosciEnum.fromValue(rs.getString("status_platnosci")));
        value.setDataPlatnosci(
            rs.getTimestamp("data_platnosci") == null
                ? null
                : rs.getTimestamp("data_platnosci").toLocalDateTime().atOffset(ZoneOffset.UTC));
        return value;
      };

  Platnosc read(Integer id, boolean lock) {
    if (id == null || id < 1)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawne ID");
    try {
      return jdbc.queryForObject(
          "SELECT id_platnosci,id_zamowienia,metoda_platnosci,status_platnosci,data_platnosci FROM"
              + " Platnosc WHERE id_platnosci=:id"
              + (lock ? " FOR UPDATE" : ""),
          params("id", id),
          ROW_MAPPER);
    } catch (EmptyResultDataAccessException error) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono rekordu");
    }
  }

  public Platnosc get(Integer id) {
    return read(id, false);
  }

  public List<Platnosc> list(Integer idZamowienia) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT id_platnosci,id_zamowienia,metoda_platnosci,status_platnosci,data_platnosci"
                + " FROM Platnosc WHERE 1=1");
    Map<String, Object> parameters = new LinkedHashMap<>();
    if (idZamowienia != null) {
      sql.append(" AND id_zamowienia=:id_zamowienia");
      parameters.put("id_zamowienia", idZamowienia);
    }
    sql.append(" ORDER BY id_platnosci");
    var result = jdbc.query(sql.toString(), parameters, ROW_MAPPER);
    return result;
  }

  private void validate(PlatnoscCreate value) {
    if (value == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: body");
    if (value.getIdZamowienia() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: id_zamowienia");
    if (value.getIdZamowienia() != null) checkNumber(value.getIdZamowienia(), 1, false);
    if (value.getIdZamowienia() != null) zamowienie.read(value.getIdZamowienia(), false);
    if (value.getMetodaPlatnosci() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: metoda_platnosci");
    if (value.getMetodaPlatnosci() != null) checkText(value.getMetodaPlatnosci(), 30, true);
  }

  private Map<String, Object> parameters(PlatnoscCreate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id_zamowienia", value.getIdZamowienia());
    result.put("metoda_platnosci", value.getMetodaPlatnosci());
    result.put(
        "status_platnosci",
        value.getStatusPlatnosci() == null
            ? "OCZEKUJACA"
            : (value.getStatusPlatnosci().getValue()));
    result.put(
        "data_platnosci",
        value.getDataPlatnosci() == null
            ? null
            : Timestamp.valueOf(
                value.getDataPlatnosci().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()));
    return result;
  }

  private void validate(PlatnoscUpdate value) {
    if (value == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: body");
    if (value.getIdZamowienia() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: id_zamowienia");
    if (value.getIdZamowienia() != null) checkNumber(value.getIdZamowienia(), 1, false);
    if (value.getIdZamowienia() != null) zamowienie.read(value.getIdZamowienia(), false);
    if (value.getMetodaPlatnosci() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: metoda_platnosci");
    if (value.getMetodaPlatnosci() != null) checkText(value.getMetodaPlatnosci(), 30, true);
    if (value.getStatusPlatnosci() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: status_platnosci");
  }

  private Map<String, Object> parameters(PlatnoscUpdate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id_zamowienia", value.getIdZamowienia());
    result.put("metoda_platnosci", value.getMetodaPlatnosci());
    result.put(
        "status_platnosci",
        value.getStatusPlatnosci() == null ? null : value.getStatusPlatnosci().getValue());
    result.put(
        "data_platnosci",
        value.getDataPlatnosci() == null
            ? null
            : Timestamp.valueOf(
                value.getDataPlatnosci().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()));
    return result;
  }

  private int insert(Map<String, Object> data) {
    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Platnosc (id_zamowienia,metoda_platnosci,status_platnosci,data_platnosci)"
            + " VALUES (:id_zamowienia,:metoda_platnosci,:status_platnosci,:data_platnosci)",
        new MapSqlParameterSource(data),
        key,
        new String[] {"id_platnosci"});
    return Objects.requireNonNull(key.getKey()).intValue();
  }

  private void write(int id, Map<String, Object> data) {
    Map<String, Object> parameters = new LinkedHashMap<>(data);
    parameters.put("_id", id);
    jdbc.update(
        "UPDATE Platnosc SET"
            + " id_zamowienia=:id_zamowienia,metoda_platnosci=:metoda_platnosci,status_platnosci=:status_platnosci,data_platnosci=:data_platnosci"
            + " WHERE id_platnosci=:_id",
        parameters);
  }

  @Transactional
  public Platnosc create(PlatnoscCreate value) {
    int id = insert(parameters(value));
    return get(id);
  }

  @Transactional
  public Platnosc update(Integer id, PlatnoscUpdate value) {
    read(id, true);
    write(id, parameters(value));
    return get(id);
  }

  @Transactional
  public void delete(Integer id) {
    read(id, true);
    jdbc.update("DELETE FROM Platnosc WHERE id_platnosci=:id", params("id", id));
  }

  private static Map<String, Object> params(Object... pairs) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
    return m;
  }

  private static void checkText(String text, int max, boolean required) {
    if (required && text.isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Puste pole");
    if (max > 0 && text.length() > max)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zbyt długie pole");
  }

  private static void checkNumber(Number value, int min, boolean money) {
    BigDecimal d = new BigDecimal(value.toString());
    if (d.compareTo(BigDecimal.valueOf(min)) < 0)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawna liczba");
    if (money
        && (d.stripTrailingZeros().scale() > 2 || d.compareTo(new BigDecimal("99999999.99")) > 0))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawna cena");
  }
}
