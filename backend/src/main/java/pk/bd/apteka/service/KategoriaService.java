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
public class KategoriaService {
  private final NamedParameterJdbcTemplate jdbc;

  public KategoriaService(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  static final RowMapper<Kategoria> ROW_MAPPER =
      (rs, index) -> {
        var value = new Kategoria();
        value.setIdKategorii(rs.getObject("id_kategorii", Integer.class));
        value.setNazwaKategorii(rs.getString("nazwa_kategorii"));
        value.setOpis(rs.getString("opis"));
        return value;
      };

  Kategoria read(Integer id, boolean lock) {
    requireId(id);
    try {
      return jdbc.queryForObject(
          "SELECT id_kategorii,nazwa_kategorii,opis FROM Kategoria WHERE id_kategorii=:id"
              + (lock ? " FOR UPDATE" : ""),
          params("id", id),
          ROW_MAPPER);
    } catch (EmptyResultDataAccessException error) {
      throw missing();
    }
  }

  public Kategoria get(Integer id) {
    return read(id, false);
  }

  public List<Kategoria> list() {
    StringBuilder sql =
        new StringBuilder("SELECT id_kategorii,nazwa_kategorii,opis FROM Kategoria WHERE 1=1");
    Map<String, Object> parameters = new LinkedHashMap<>();
    sql.append(" ORDER BY id_kategorii");
    var result = jdbc.query(sql.toString(), parameters, ROW_MAPPER);
    return result;
  }

  private void validate(KategoriaCreate value) {
    requireValue(value, "body");
    requireValue(value.getNazwaKategorii(), "nazwa_kategorii");
    if (value.getNazwaKategorii() != null) checkText(value.getNazwaKategorii(), 50, true);
    if (value.getOpis() != null) checkText(value.getOpis(), 0, false);
  }

  private Map<String, Object> parameters(KategoriaCreate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("nazwa_kategorii", value.getNazwaKategorii());
    result.put("opis", value.getOpis());
    return result;
  }

  private void validate(KategoriaUpdate value) {
    requireValue(value, "body");
    requireValue(value.getNazwaKategorii(), "nazwa_kategorii");
    if (value.getNazwaKategorii() != null) checkText(value.getNazwaKategorii(), 50, true);
    if (value.getOpis() != null) checkText(value.getOpis(), 0, false);
  }

  private Map<String, Object> parameters(KategoriaUpdate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("nazwa_kategorii", value.getNazwaKategorii());
    result.put("opis", value.getOpis());
    return result;
  }

  private int insert(Map<String, Object> data) {
    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Kategoria (nazwa_kategorii,opis) VALUES (:nazwa_kategorii,:opis)",
        new MapSqlParameterSource(data),
        key,
        new String[] {"id_kategorii"});
    return Objects.requireNonNull(key.getKey()).intValue();
  }

  private void write(int id, Map<String, Object> data) {
    Map<String, Object> parameters = new LinkedHashMap<>(data);
    parameters.put("_id", id);
    jdbc.update(
        "UPDATE Kategoria SET nazwa_kategorii=:nazwa_kategorii,opis=:opis WHERE id_kategorii=:_id",
        parameters);
  }

  @Transactional
  public Kategoria create(KategoriaCreate value) {
    int id = insert(parameters(value));
    return get(id);
  }

  @Transactional
  public Kategoria update(Integer id, KategoriaUpdate value) {
    read(id, true);
    write(id, parameters(value));
    return get(id);
  }

  @Transactional
  public void delete(Integer id) {
    read(id, true);
    jdbc.update("DELETE FROM Kategoria WHERE id_kategorii=:id", params("id", id));
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
}
