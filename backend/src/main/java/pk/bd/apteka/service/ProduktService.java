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
public class ProduktService {
  private final NamedParameterJdbcTemplate jdbc;
  private final DostawcaService dostawca;
  private final KategoriaService kategoria;

  public ProduktService(
      NamedParameterJdbcTemplate jdbc, DostawcaService dostawca, KategoriaService kategoria) {
    this.jdbc = jdbc;
    this.dostawca = dostawca;
    this.kategoria = kategoria;
  }

  static final RowMapper<Produkt> ROW_MAPPER =
      (rs, index) -> {
        var value = new Produkt();
        value.setIdProduktu(rs.getObject("id_produktu", Integer.class));
        value.setIdKategorii(rs.getObject("id_kategorii", Integer.class));
        value.setIdDostawcy(rs.getObject("id_dostawcy", Integer.class));
        value.setNazwaProduktu(rs.getString("nazwa_produktu"));
        value.setCena(rs.getBigDecimal("cena"));
        value.setStanMagazynowy(rs.getObject("stan_magazynowy", Integer.class));
        value.setWymagaRecepty(rs.getBoolean("wymaga_recepty"));
        return value;
      };

  Produkt read(Integer id, boolean lock) {
    if (id == null || id < 1)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawne ID");
    try {
      return jdbc.queryForObject(
          "SELECT"
              + " id_produktu,id_kategorii,id_dostawcy,nazwa_produktu,cena,stan_magazynowy,wymaga_recepty"
              + " FROM Produkt WHERE id_produktu=:id"
              + (lock ? " FOR UPDATE" : ""),
          params("id", id),
          ROW_MAPPER);
    } catch (EmptyResultDataAccessException error) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono rekordu");
    }
  }

  public Produkt get(Integer id) {
    return read(id, false);
  }

  public List<Produkt> list(Integer idKategorii, Integer idDostawcy) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT"
                + " id_produktu,id_kategorii,id_dostawcy,nazwa_produktu,cena,stan_magazynowy,wymaga_recepty"
                + " FROM Produkt WHERE 1=1");
    Map<String, Object> parameters = new LinkedHashMap<>();
    if (idKategorii != null) {
      sql.append(" AND id_kategorii=:id_kategorii");
      parameters.put("id_kategorii", idKategorii);
    }
    if (idDostawcy != null) {
      sql.append(" AND id_dostawcy=:id_dostawcy");
      parameters.put("id_dostawcy", idDostawcy);
    }
    sql.append(" ORDER BY id_produktu");
    var result = jdbc.query(sql.toString(), parameters, ROW_MAPPER);
    return result;
  }

  private void validate(ProduktCreate value) {
    if (value == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: body");
    if (value.getIdKategorii() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: id_kategorii");
    if (value.getIdKategorii() != null) checkNumber(value.getIdKategorii(), 1, false);
    if (value.getIdKategorii() != null) kategoria.read(value.getIdKategorii(), false);
    if (value.getIdDostawcy() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: id_dostawcy");
    if (value.getIdDostawcy() != null) checkNumber(value.getIdDostawcy(), 1, false);
    if (value.getIdDostawcy() != null) dostawca.read(value.getIdDostawcy(), false);
    if (value.getNazwaProduktu() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: nazwa_produktu");
    if (value.getNazwaProduktu() != null) checkText(value.getNazwaProduktu(), 100, true);
    if (value.getCena() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: cena");
    if (value.getCena() != null) checkNumber(value.getCena(), 0, true);
    if (value.getStanMagazynowy() != null) checkNumber(value.getStanMagazynowy(), 0, false);
  }

  private Map<String, Object> parameters(ProduktCreate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id_kategorii", value.getIdKategorii());
    result.put("id_dostawcy", value.getIdDostawcy());
    result.put("nazwa_produktu", value.getNazwaProduktu());
    result.put("cena", value.getCena());
    result.put(
        "stan_magazynowy", value.getStanMagazynowy() == null ? 0 : (value.getStanMagazynowy()));
    result.put(
        "wymaga_recepty", value.getWymagaRecepty() == null ? false : (value.getWymagaRecepty()));
    return result;
  }

  private void validate(ProduktUpdate value) {
    if (value == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: body");
    if (value.getIdKategorii() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: id_kategorii");
    if (value.getIdKategorii() != null) checkNumber(value.getIdKategorii(), 1, false);
    if (value.getIdKategorii() != null) kategoria.read(value.getIdKategorii(), false);
    if (value.getIdDostawcy() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: id_dostawcy");
    if (value.getIdDostawcy() != null) checkNumber(value.getIdDostawcy(), 1, false);
    if (value.getIdDostawcy() != null) dostawca.read(value.getIdDostawcy(), false);
    if (value.getNazwaProduktu() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: nazwa_produktu");
    if (value.getNazwaProduktu() != null) checkText(value.getNazwaProduktu(), 100, true);
    if (value.getCena() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: cena");
    if (value.getCena() != null) checkNumber(value.getCena(), 0, true);
    if (value.getStanMagazynowy() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: stan_magazynowy");
    if (value.getStanMagazynowy() != null) checkNumber(value.getStanMagazynowy(), 0, false);
    if (value.getWymagaRecepty() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: wymaga_recepty");
  }

  private Map<String, Object> parameters(ProduktUpdate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id_kategorii", value.getIdKategorii());
    result.put("id_dostawcy", value.getIdDostawcy());
    result.put("nazwa_produktu", value.getNazwaProduktu());
    result.put("cena", value.getCena());
    result.put("stan_magazynowy", value.getStanMagazynowy());
    result.put("wymaga_recepty", value.getWymagaRecepty());
    return result;
  }

  private int insert(Map<String, Object> data) {
    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Produkt"
            + " (id_kategorii,id_dostawcy,nazwa_produktu,cena,stan_magazynowy,wymaga_recepty)"
            + " VALUES"
            + " (:id_kategorii,:id_dostawcy,:nazwa_produktu,:cena,:stan_magazynowy,:wymaga_recepty)",
        new MapSqlParameterSource(data),
        key,
        new String[] {"id_produktu"});
    return Objects.requireNonNull(key.getKey()).intValue();
  }

  private void write(int id, Map<String, Object> data) {
    Map<String, Object> parameters = new LinkedHashMap<>(data);
    parameters.put("_id", id);
    jdbc.update(
        "UPDATE Produkt SET"
            + " id_kategorii=:id_kategorii,id_dostawcy=:id_dostawcy,nazwa_produktu=:nazwa_produktu,cena=:cena,stan_magazynowy=:stan_magazynowy,wymaga_recepty=:wymaga_recepty"
            + " WHERE id_produktu=:_id",
        parameters);
  }

  @Transactional
  public Produkt create(ProduktCreate value) {
    int id = insert(parameters(value));
    return get(id);
  }

  @Transactional
  public Produkt update(Integer id, ProduktUpdate value) {
    read(id, true);
    write(id, parameters(value));
    return get(id);
  }

  @Transactional
  public void delete(Integer id) {
    read(id, true);
    jdbc.update("DELETE FROM Produkt WHERE id_produktu=:id", params("id", id));
  }

  void release(List<SzczegolZamowienia> lines) {
    jdbc.queryForList("SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
    for (var l : lines)
      jdbc.update(
          "UPDATE Produkt SET stan_magazynowy=stan_magazynowy+:q WHERE id_produktu=:p",
          params("q", l.getIlosc(), "p", l.getIdProduktu()));
  }

  void reserve(List<SzczegolZamowienia> lines) {
    for (var l : lines)
      if (jdbc.update(
              "UPDATE Produkt SET stan_magazynowy=stan_magazynowy-:q WHERE id_produktu=:p AND"
                  + " stan_magazynowy>=:q",
              params("q", l.getIlosc(), "p", l.getIdProduktu()))
          == 0)
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brak produktu na stanie");
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
