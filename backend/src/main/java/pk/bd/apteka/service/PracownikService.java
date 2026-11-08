package pk.bd.apteka.service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pk.bd.apteka.dto.*;

@Service
public class PracownikService {
  private final NamedParameterJdbcTemplate jdbc;

  public PracownikService(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  static final RowMapper<Pracownik> ROW_MAPPER =
      (rs, index) -> {
        var value = new Pracownik();
        value.setIdPracownika(rs.getObject("id_pracownika", Integer.class));
        value.setLogin(rs.getString("login"));
        value.setImie(rs.getString("imie"));
        value.setNazwisko(rs.getString("nazwisko"));
        value.setRola(Pracownik.RolaEnum.fromValue(rs.getString("rola")));
        value.setAktywny(rs.getBoolean("aktywny"));
        return value;
      };

  Pracownik read(Integer id, boolean lock) {
    requireId(id);
    try {
      return jdbc.queryForObject(
          "SELECT id_pracownika,login,imie,nazwisko,rola,aktywny FROM Pracownik WHERE"
              + " id_pracownika=:id"
              + (lock ? " FOR UPDATE" : ""),
          params("id", id),
          ROW_MAPPER);
    } catch (EmptyResultDataAccessException error) {
      throw missing();
    }
  }

  public Pracownik get(Integer id) {
    return read(id, false);
  }

  public List<Pracownik> list() {
    StringBuilder sql =
        new StringBuilder(
            "SELECT id_pracownika,login,imie,nazwisko,rola,aktywny FROM Pracownik WHERE 1=1");
    Map<String, Object> parameters = new LinkedHashMap<>();
    sql.append(" ORDER BY id_pracownika");
    var result = jdbc.query(sql.toString(), parameters, ROW_MAPPER);
    return result;
  }

  private void validate(PracownikCreate value) {
    requireValue(value, "body");
    requireValue(value.getLogin(), "login");
    if (value.getLogin() != null) checkText(value.getLogin(), 50, true);
    requireValue(value.getHaslo(), "haslo");
    if (value.getHaslo() != null) checkText(value.getHaslo(), 255, true);
    requireValue(value.getImie(), "imie");
    if (value.getImie() != null) checkText(value.getImie(), 50, true);
    requireValue(value.getNazwisko(), "nazwisko");
    if (value.getNazwisko() != null) checkText(value.getNazwisko(), 50, true);
  }

  private Map<String, Object> parameters(PracownikCreate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("login", value.getLogin());
    result.put("haslo", value.getHaslo() == null ? null : hash(value.getHaslo()));
    result.put("imie", value.getImie());
    result.put("nazwisko", value.getNazwisko());
    result.put("rola", value.getRola() == null ? "FARMACEUTA" : (value.getRola().getValue()));
    result.put("aktywny", value.getAktywny() == null ? true : (value.getAktywny()));
    return result;
  }

  private void validate(PracownikUpdate value) {
    requireValue(value, "body");
    requireValue(value.getLogin(), "login");
    if (value.getLogin() != null) checkText(value.getLogin(), 50, true);
    if (value.getHaslo() != null) checkText(value.getHaslo(), 255, true);
    requireValue(value.getImie(), "imie");
    if (value.getImie() != null) checkText(value.getImie(), 50, true);
    requireValue(value.getNazwisko(), "nazwisko");
    if (value.getNazwisko() != null) checkText(value.getNazwisko(), 50, true);
    requireValue(value.getRola(), "rola");
    requireValue(value.getAktywny(), "aktywny");
  }

  private Map<String, Object> parameters(PracownikUpdate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("login", value.getLogin());
    result.put("haslo", value.getHaslo() == null ? null : hash(value.getHaslo()));
    result.put("imie", value.getImie());
    result.put("nazwisko", value.getNazwisko());
    result.put("rola", value.getRola() == null ? null : value.getRola().getValue());
    result.put("aktywny", value.getAktywny());
    return result;
  }

  private int insert(Map<String, Object> data) {
    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Pracownik (login,haslo,imie,nazwisko,rola,aktywny) VALUES"
            + " (:login,:haslo,:imie,:nazwisko,:rola,:aktywny)",
        new MapSqlParameterSource(data),
        key,
        new String[] {"id_pracownika"});
    return Objects.requireNonNull(key.getKey()).intValue();
  }

  private void write(int id, Map<String, Object> data) {
    Map<String, Object> parameters = new LinkedHashMap<>(data);
    parameters.put("_id", id);
    jdbc.update(
        "UPDATE Pracownik SET"
            + " login=:login,haslo=COALESCE(:haslo,haslo),imie=:imie,nazwisko=:nazwisko,rola=:rola,aktywny=:aktywny"
            + " WHERE id_pracownika=:_id",
        parameters);
  }

  @Transactional
  public Pracownik create(PracownikCreate value) {
    int id = insert(parameters(value));
    return get(id);
  }

  @Transactional
  public Pracownik update(Integer id, PracownikUpdate value) {
    read(id, true);
    write(id, parameters(value));
    return get(id);
  }

  @Transactional
  public void delete(Integer id) {
    read(id, true);
    jdbc.update("DELETE FROM Pracownik WHERE id_pracownika=:id", params("id", id));
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

  private static String hash(String text) {
    if (text.getBytes(StandardCharsets.UTF_8).length > 72) throw bad("Hasło przekracza 72 bajty");
    return new BCryptPasswordEncoder().encode(text);
  }
}
