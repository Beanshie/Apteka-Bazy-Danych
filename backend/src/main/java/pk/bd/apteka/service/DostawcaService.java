package pk.bd.apteka.service;

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
public class DostawcaService {
  private final NamedParameterJdbcTemplate jdbc;

  public DostawcaService(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  static final RowMapper<Dostawca> ROW_MAPPER =
      (rs, index) -> {
        var value = new Dostawca();
        value.setIdDostawcy(rs.getObject("id_dostawcy", Integer.class));
        value.setNazwaFirmy(rs.getString("nazwa_firmy"));
        value.setNip(rs.getString("nip"));
        value.setTelefon(rs.getString("telefon"));
        value.setEmail(rs.getString("email"));
        return value;
      };

  Dostawca read(Integer id, boolean lock) {
    if (id == null || id < 1)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawne ID");
    try {
      return jdbc.queryForObject(
          "SELECT id_dostawcy,nazwa_firmy,nip,telefon,email FROM Dostawca WHERE id_dostawcy=:id"
              + (lock ? " FOR UPDATE" : ""),
          params("id", id),
          ROW_MAPPER);
    } catch (EmptyResultDataAccessException error) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono rekordu");
    }
  }

  public Dostawca get(Integer id) {
    return read(id, false);
  }

  public List<Dostawca> list() {
    StringBuilder sql =
        new StringBuilder(
            "SELECT id_dostawcy,nazwa_firmy,nip,telefon,email FROM Dostawca WHERE 1=1");
    Map<String, Object> parameters = new LinkedHashMap<>();
    sql.append(" ORDER BY id_dostawcy");
    var result = jdbc.query(sql.toString(), parameters, ROW_MAPPER);
    return result;
  }

  private void validate(DostawcaCreate value) {
    if (value == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: body");
    if (value.getNazwaFirmy() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: nazwa_firmy");
    if (value.getNazwaFirmy() != null) checkText(value.getNazwaFirmy(), 100, true);
    if (value.getNip() != null) checkText(value.getNip(), 15, false);
    if (value.getTelefon() != null) checkText(value.getTelefon(), 20, false);
    if (value.getEmail() != null) checkText(value.getEmail(), 100, false);
  }

  private Map<String, Object> parameters(DostawcaCreate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("nazwa_firmy", value.getNazwaFirmy());
    result.put("nip", value.getNip());
    result.put("telefon", value.getTelefon());
    result.put("email", value.getEmail());
    return result;
  }

  private void validate(DostawcaUpdate value) {
    if (value == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: body");
    if (value.getNazwaFirmy() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: nazwa_firmy");
    if (value.getNazwaFirmy() != null) checkText(value.getNazwaFirmy(), 100, true);
    if (value.getNip() != null) checkText(value.getNip(), 15, false);
    if (value.getTelefon() != null) checkText(value.getTelefon(), 20, false);
    if (value.getEmail() != null) checkText(value.getEmail(), 100, false);
  }

  private Map<String, Object> parameters(DostawcaUpdate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("nazwa_firmy", value.getNazwaFirmy());
    result.put("nip", value.getNip());
    result.put("telefon", value.getTelefon());
    result.put("email", value.getEmail());
    return result;
  }

  private int insert(Map<String, Object> data) {
    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Dostawca (nazwa_firmy,nip,telefon,email) VALUES"
            + " (:nazwa_firmy,:nip,:telefon,:email)",
        new MapSqlParameterSource(data),
        key,
        new String[] {"id_dostawcy"});
    return Objects.requireNonNull(key.getKey()).intValue();
  }

  private void write(int id, Map<String, Object> data) {
    Map<String, Object> parameters = new LinkedHashMap<>(data);
    parameters.put("_id", id);
    jdbc.update(
        "UPDATE Dostawca SET nazwa_firmy=:nazwa_firmy,nip=:nip,telefon=:telefon,email=:email WHERE"
            + " id_dostawcy=:_id",
        parameters);
  }

  @Transactional
  public Dostawca create(DostawcaCreate value) {
    int id = insert(parameters(value));
    return get(id);
  }

  @Transactional
  public Dostawca update(Integer id, DostawcaUpdate value) {
    read(id, true);
    write(id, parameters(value));
    return get(id);
  }

  @Transactional
  public void delete(Integer id) {
    read(id, true);
    jdbc.update("DELETE FROM Dostawca WHERE id_dostawcy=:id", params("id", id));
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
}
