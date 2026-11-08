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
public class SzczegolZamowieniaService {
  private final NamedParameterJdbcTemplate jdbc;
  private final ProduktService produkt;
  private final ReceptaService recepta;
  private final ZamowienieService zamowienie;

  public SzczegolZamowieniaService(
      NamedParameterJdbcTemplate jdbc,
      ProduktService produkt,
      ReceptaService recepta,
      ZamowienieService zamowienie) {
    this.jdbc = jdbc;
    this.produkt = produkt;
    this.recepta = recepta;
    this.zamowienie = zamowienie;
  }

  static final RowMapper<SzczegolZamowienia> ROW_MAPPER =
      (rs, index) -> {
        var value = new SzczegolZamowienia();
        value.setIdSzczegolu(rs.getObject("id_szczegolu", Integer.class));
        value.setIdZamowienia(rs.getObject("id_zamowienia", Integer.class));
        value.setIdProduktu(rs.getObject("id_produktu", Integer.class));
        value.setIdRecepty(rs.getObject("id_recepty", Integer.class));
        value.setIlosc(rs.getObject("ilosc", Integer.class));
        value.setCenaJednostkowa(rs.getBigDecimal("cena_jednostkowa"));
        return value;
      };

  SzczegolZamowienia read(Integer id, boolean lock) {
    if (id == null || id < 1)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawne ID");
    try {
      return jdbc.queryForObject(
          "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
              + " Szczegol_Zamowienia WHERE id_szczegolu=:id"
              + (lock ? " FOR UPDATE" : ""),
          params("id", id),
          ROW_MAPPER);
    } catch (EmptyResultDataAccessException error) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono rekordu");
    }
  }

  public SzczegolZamowienia get(Integer id, Integer idZamowienia) {
    zamowienie.read(idZamowienia, false);
    return itemInOrder(id, idZamowienia);
  }

  private SzczegolZamowienia itemInOrder(Integer id, Integer idZamowienia) {
    var result = read(id, false);
    if (!Objects.equals(result.getIdZamowienia(), idZamowienia))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono rekordu");
    return result;
  }

  public List<SzczegolZamowienia> list(Integer idZamowienia) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                + " Szczegol_Zamowienia WHERE 1=1");
    Map<String, Object> parameters = new LinkedHashMap<>();
    zamowienie.read(idZamowienia, false);
    sql.append(" AND id_zamowienia=:id");
    parameters.put("id", idZamowienia);
    sql.append(" ORDER BY id_szczegolu");
    var result = jdbc.query(sql.toString(), parameters, ROW_MAPPER);
    return result;
  }

  private void validate(SzczegolZamowieniaCreate value) {
    if (value == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: body");
    if (value.getIdProduktu() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: id_produktu");
    if (value.getIdProduktu() != null) checkNumber(value.getIdProduktu(), 1, false);
    if (value.getIdProduktu() != null) produkt.read(value.getIdProduktu(), false);
    if (value.getIdRecepty() != null) checkNumber(value.getIdRecepty(), 1, false);
    if (value.getIdRecepty() != null) recepta.read(value.getIdRecepty(), false);
    if (value.getIlosc() != null) checkNumber(value.getIlosc(), 1, false);
  }

  private Map<String, Object> parameters(SzczegolZamowieniaCreate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id_produktu", value.getIdProduktu());
    result.put("id_recepty", value.getIdRecepty());
    result.put("ilosc", value.getIlosc() == null ? 1 : (value.getIlosc()));
    return result;
  }

  private void validate(SzczegolZamowieniaUpdate value) {
    if (value == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: body");
    if (value.getIdProduktu() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Pole nie może być null: id_produktu");
    if (value.getIdProduktu() != null) checkNumber(value.getIdProduktu(), 1, false);
    if (value.getIdProduktu() != null) produkt.read(value.getIdProduktu(), false);
    if (value.getIdRecepty() != null) checkNumber(value.getIdRecepty(), 1, false);
    if (value.getIdRecepty() != null) recepta.read(value.getIdRecepty(), false);
    if (value.getIlosc() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole nie może być null: ilosc");
    if (value.getIlosc() != null) checkNumber(value.getIlosc(), 1, false);
  }

  private Map<String, Object> parameters(SzczegolZamowieniaUpdate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id_produktu", value.getIdProduktu());
    result.put("id_recepty", value.getIdRecepty());
    result.put("ilosc", value.getIlosc());
    return result;
  }

  private int insert(Map<String, Object> data) {
    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Szczegol_Zamowienia"
            + " (id_produktu,id_recepty,ilosc,id_zamowienia,cena_jednostkowa) VALUES"
            + " (:id_produktu,:id_recepty,:ilosc,:id_zamowienia,:cena_jednostkowa)",
        new MapSqlParameterSource(data),
        key,
        new String[] {"id_szczegolu"});
    return Objects.requireNonNull(key.getKey()).intValue();
  }

  private void write(int id, Map<String, Object> data) {
    Map<String, Object> parameters = new LinkedHashMap<>(data);
    parameters.put("_id", id);
    jdbc.update(
        "UPDATE Szczegol_Zamowienia SET"
            + " id_produktu=:id_produktu,id_recepty=:id_recepty,ilosc=:ilosc,cena_jednostkowa=:cena_jednostkowa"
            + " WHERE id_szczegolu=:_id",
        parameters);
  }

  @Transactional
  public SzczegolZamowienia create(SzczegolZamowieniaCreate value, Integer idZamowienia) {
    var before = zamowienie.begin(idZamowienia);
    var data = parameters(value);
    data.put("id_zamowienia", idZamowienia);
    data.put("cena_jednostkowa", produkt.read(value.getIdProduktu(), false).getCena());
    int id = insert(data);
    zamowienie.finish(idZamowienia, before, Set.of(id));
    return get(id, idZamowienia);
  }

  @Transactional
  public SzczegolZamowienia update(
      Integer id, SzczegolZamowieniaUpdate value, Integer idZamowienia) {
    var before = zamowienie.begin(idZamowienia);
    var previous = itemInOrder(id, idZamowienia);
    var data = parameters(value);
    boolean changed = !Objects.equals(value.getIdProduktu(), previous.getIdProduktu());
    data.put(
        "cena_jednostkowa",
        changed
            ? produkt.read(value.getIdProduktu(), false).getCena()
            : previous.getCenaJednostkowa());
    write(id, data);
    boolean strict =
        changed
            || !Objects.equals(value.getIdRecepty(), previous.getIdRecepty())
            || value.getIlosc() > previous.getIlosc();
    zamowienie.finish(idZamowienia, before, strict ? Set.of(id) : Set.of());
    return get(id, idZamowienia);
  }

  @Transactional
  public void delete(Integer id, Integer idZamowienia) {
    var before = zamowienie.begin(idZamowienia);
    itemInOrder(id, idZamowienia);
    jdbc.update(
        "DELETE FROM Szczegol_Zamowienia WHERE id_szczegolu=:id AND id_zamowienia=:order",
        params("id", id, "order", idZamowienia));
    zamowienie.finish(idZamowienia, before, Set.of());
  }

  private static Map<String, Object> params(Object... pairs) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
    return m;
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
