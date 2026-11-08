package pk.bd.apteka.service;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
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
public class KlientService {
  private final NamedParameterJdbcTemplate jdbc;

  public KlientService(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  static final RowMapper<Klient> ROW_MAPPER =
      (rs, index) -> {
        var value = new Klient();
        value.setIdKlienta(rs.getObject("id_klienta", Integer.class));
        value.setImie(rs.getString("imie"));
        value.setNazwisko(rs.getString("nazwisko"));
        value.setEmail(rs.getString("email"));
        value.setTelefon(rs.getString("telefon"));
        value.setDataRejestracji(
            rs.getTimestamp("data_rejestracji") == null
                ? null
                : rs.getTimestamp("data_rejestracji").toLocalDateTime().atOffset(ZoneOffset.UTC));
        return value;
      };

  Klient read(Integer id, boolean lock) {
    if (id == null || id < 1)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawne ID");
    try {
      return jdbc.queryForObject(
          "SELECT id_klienta,imie,nazwisko,email,telefon,data_rejestracji FROM Klient WHERE"
              + " id_klienta=:id"
              + (lock ? " FOR UPDATE" : ""),
          params("id", id),
          ROW_MAPPER);
    } catch (EmptyResultDataAccessException error) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono rekordu");
    }
  }

  public Klient get(Integer id) {
    return read(id, false);
  }

  public List<Klient> list() {
    StringBuilder sql =
        new StringBuilder(
            "SELECT id_klienta,imie,nazwisko,email,telefon,data_rejestracji FROM Klient WHERE 1=1");
    Map<String, Object> parameters = new LinkedHashMap<>();
    sql.append(" ORDER BY id_klienta");
    var result = jdbc.query(sql.toString(), parameters, ROW_MAPPER);
    return result;
  }

  private void validate(KlientCreate value) {
    if (value == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: body");
    if (value.getImie() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: imie");
    if (value.getImie() != null) checkText(value.getImie(), 50, true);
    if (value.getNazwisko() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: nazwisko");
    if (value.getNazwisko() != null) checkText(value.getNazwisko(), 50, true);
    if (value.getEmail() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: email");
    if (value.getEmail() != null) checkText(value.getEmail(), 100, true);
    if (value.getHaslo() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: haslo");
    if (value.getHaslo() != null) checkText(value.getHaslo(), 255, true);
    if (value.getTelefon() != null) checkText(value.getTelefon(), 20, false);
  }

  private Map<String, Object> parameters(KlientCreate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("imie", value.getImie());
    result.put("nazwisko", value.getNazwisko());
    result.put("email", value.getEmail());
    result.put("haslo", value.getHaslo() == null ? null : hash(value.getHaslo()));
    result.put("telefon", value.getTelefon());
    return result;
  }

  private void validate(KlientUpdate value) {
    if (value == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: body");
    if (value.getImie() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: imie");
    if (value.getImie() != null) checkText(value.getImie(), 50, true);
    if (value.getNazwisko() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: nazwisko");
    if (value.getNazwisko() != null) checkText(value.getNazwisko(), 50, true);
    if (value.getEmail() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: email");
    if (value.getEmail() != null) checkText(value.getEmail(), 100, true);
    if (value.getHaslo() != null) checkText(value.getHaslo(), 255, true);
    if (value.getTelefon() != null) checkText(value.getTelefon(), 20, false);
  }

  private Map<String, Object> parameters(KlientUpdate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("imie", value.getImie());
    result.put("nazwisko", value.getNazwisko());
    result.put("email", value.getEmail());
    result.put("haslo", value.getHaslo() == null ? null : hash(value.getHaslo()));
    result.put("telefon", value.getTelefon());
    return result;
  }

  private int insert(Map<String, Object> data) {
    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Klient (imie,nazwisko,email,haslo,telefon) VALUES"
            + " (:imie,:nazwisko,:email,:haslo,:telefon)",
        new MapSqlParameterSource(data),
        key,
        new String[] {"id_klienta"});
    return Objects.requireNonNull(key.getKey()).intValue();
  }

  private void write(int id, Map<String, Object> data) {
    Map<String, Object> parameters = new LinkedHashMap<>(data);
    parameters.put("_id", id);
    jdbc.update(
        "UPDATE Klient SET"
            + " imie=:imie,nazwisko=:nazwisko,email=:email,haslo=COALESCE(:haslo,haslo),telefon=:telefon"
            + " WHERE id_klienta=:_id",
        parameters);
  }

  @Transactional
  public Klient create(KlientCreate value) {
    int id = insert(parameters(value));
    return get(id);
  }

  @Transactional
  public Klient update(Integer id, KlientUpdate value) {
    read(id, true);
    write(id, parameters(value));
    return get(id);
  }

  @Transactional
  public void delete(Integer id) {
    read(id, true);
    jdbc.update("DELETE FROM Klient WHERE id_klienta=:id", params("id", id));
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

  private static String hash(String text) {
    if (text.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hasło przekracza 72 bajty");
    return new BCryptPasswordEncoder().encode(text);
  }
}
