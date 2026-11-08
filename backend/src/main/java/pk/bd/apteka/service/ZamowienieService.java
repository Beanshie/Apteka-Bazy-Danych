package pk.bd.apteka.service;

import java.math.BigDecimal;
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
public class ZamowienieService {
  private final NamedParameterJdbcTemplate jdbc;
  private final AdresKlientaService adresklienta;
  private final KlientService klient;
  private final ProduktService produkt;
  private final ReceptaService recepta;

  public ZamowienieService(
      NamedParameterJdbcTemplate jdbc,
      AdresKlientaService adresklienta,
      KlientService klient,
      ProduktService produkt,
      ReceptaService recepta) {
    this.jdbc = jdbc;
    this.adresklienta = adresklienta;
    this.klient = klient;
    this.produkt = produkt;
    this.recepta = recepta;
  }

  static final RowMapper<Zamowienie> ROW_MAPPER =
      (rs, index) -> {
        var value = new Zamowienie();
        value.setIdZamowienia(rs.getObject("id_zamowienia", Integer.class));
        value.setIdKlienta(rs.getObject("id_klienta", Integer.class));
        value.setIdAdresu(rs.getObject("id_adresu", Integer.class));
        value.setDataZamowienia(
            rs.getTimestamp("data_zamowienia") == null
                ? null
                : rs.getTimestamp("data_zamowienia").toLocalDateTime().atOffset(ZoneOffset.UTC));
        value.setStatusZamowienia(
            Zamowienie.StatusZamowieniaEnum.fromValue(rs.getString("status_zamowienia")));
        value.setSumaCalkowita(rs.getBigDecimal("suma_calkowita"));
        return value;
      };

  Zamowienie read(Integer id, boolean lock) {
    requireId(id);
    try {
      return jdbc.queryForObject(
          "SELECT"
              + " id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita"
              + " FROM Zamowienie WHERE id_zamowienia=:id"
              + (lock ? " FOR UPDATE" : ""),
          params("id", id),
          ROW_MAPPER);
    } catch (EmptyResultDataAccessException error) {
      throw missing();
    }
  }

  public Zamowienie get(Integer id) {
    var result = read(id, false);
    result.setPozycje(lines(id));
    return result;
  }

  public List<Zamowienie> list(Integer idKlienta, String statusZamowienia) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT"
                + " id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita"
                + " FROM Zamowienie WHERE 1=1");
    Map<String, Object> parameters = new LinkedHashMap<>();
    if (idKlienta != null) {
      sql.append(" AND id_klienta=:id_klienta");
      parameters.put("id_klienta", idKlienta);
    }
    if (statusZamowienia != null) {
      sql.append(" AND status_zamowienia=:status_zamowienia");
      parameters.put("status_zamowienia", statusZamowienia);
    }
    sql.append(" ORDER BY id_zamowienia");
    var result = jdbc.query(sql.toString(), parameters, ROW_MAPPER);
    result.forEach(value -> value.setPozycje(lines(value.getIdZamowienia())));
    return result;
  }

  private void validate(ZamowienieCreate value) {
    requireValue(value, "body");
    requireValue(value.getIdKlienta(), "id_klienta");
    if (value.getIdKlienta() != null) checkNumber(value.getIdKlienta(), 1, false);
    if (value.getIdKlienta() != null) klient.read(value.getIdKlienta(), false);
    requireValue(value.getIdAdresu(), "id_adresu");
    if (value.getIdAdresu() != null) checkNumber(value.getIdAdresu(), 1, false);
    if (value.getIdAdresu() != null) adresklienta.read(value.getIdAdresu(), false);
  }

  private Map<String, Object> parameters(ZamowienieCreate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id_klienta", value.getIdKlienta());
    result.put("id_adresu", value.getIdAdresu());
    result.put(
        "status_zamowienia",
        value.getStatusZamowienia() == null ? "NOWE" : (value.getStatusZamowienia().getValue()));
    return result;
  }

  private void validate(ZamowienieUpdate value) {
    requireValue(value, "body");
    requireValue(value.getIdKlienta(), "id_klienta");
    if (value.getIdKlienta() != null) checkNumber(value.getIdKlienta(), 1, false);
    if (value.getIdKlienta() != null) klient.read(value.getIdKlienta(), false);
    requireValue(value.getIdAdresu(), "id_adresu");
    if (value.getIdAdresu() != null) checkNumber(value.getIdAdresu(), 1, false);
    if (value.getIdAdresu() != null) adresklienta.read(value.getIdAdresu(), false);
    requireValue(value.getStatusZamowienia(), "status_zamowienia");
  }

  private Map<String, Object> parameters(ZamowienieUpdate value) {
    validate(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id_klienta", value.getIdKlienta());
    result.put("id_adresu", value.getIdAdresu());
    result.put(
        "status_zamowienia",
        value.getStatusZamowienia() == null ? null : value.getStatusZamowienia().getValue());
    return result;
  }

  private int insert(Map<String, Object> data) {
    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Zamowienie (id_klienta,id_adresu,status_zamowienia) VALUES"
            + " (:id_klienta,:id_adresu,:status_zamowienia)",
        new MapSqlParameterSource(data),
        key,
        new String[] {"id_zamowienia"});
    return Objects.requireNonNull(key.getKey()).intValue();
  }

  private void write(int id, Map<String, Object> data) {
    Map<String, Object> parameters = new LinkedHashMap<>(data);
    parameters.put("_id", id);
    jdbc.update(
        "UPDATE Zamowienie SET"
            + " id_klienta=:id_klienta,id_adresu=:id_adresu,status_zamowienia=:status_zamowienia"
            + " WHERE id_zamowienia=:_id",
        parameters);
  }

  Zamowienie begin(Integer id) {
    var before = read(id, true);
    produkt.lockProducts();
    if (before.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE)
      produkt.release(lines(id));
    return before;
  }

  void finish(Integer id, Zamowienie before, Set<Integer> check) {
    var next = read(id, false);
    var address = adresklienta.read(next.getIdAdresu(), true);
    if (!Objects.equals(address.getIdKlienta(), next.getIdKlienta()))
      throw bad("Adres należy do innego klienta");
    boolean active = next.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE;
    var after = lines(id);
    for (var line : after) {
      boolean strict =
          check.contains(line.getIdSzczegolu())
              || active
                  && before.getStatusZamowienia() == Zamowienie.StatusZamowieniaEnum.ANULOWANE;
      var product = produkt.read(line.getIdProduktu(), false);
      if (line.getIdRecepty() != null) {
        var prescription = recepta.read(line.getIdRecepty(), true);
        if (!Objects.equals(prescription.getIdKlienta(), next.getIdKlienta()))
          throw bad("Recepta należy do innego klienta");
        if (strict
            && prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.NOWA
            && prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.ZWERYFIKOWANA)
          throw bad("Recepta nie jest ważna");
      }
      if (strict && product.getWymagaRecepty() && line.getIdRecepty() == null)
        throw bad("Wymagana recepta");
    }
    if (active) produkt.reserve(after);
    BigDecimal total =
        after.stream()
            .map(line -> line.getCenaJednostkowa().multiply(BigDecimal.valueOf(line.getIlosc())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(new BigDecimal("99999999.99")) > 0) throw bad("Zbyt duża suma");
    jdbc.update(
        "UPDATE Zamowienie SET suma_calkowita=:total WHERE id_zamowienia=:id",
        params("total", total, "id", id));
  }

  private List<SzczegolZamowienia> lines(Integer id) {
    return jdbc.query(
        "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
            + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
        params("id", id),
        SzczegolZamowieniaService.ROW_MAPPER);
  }

  @Transactional
  public Zamowienie create(ZamowienieCreate value) {
    int id = insert(parameters(value));
    var before = begin(id);
    var check = replaceLines(id, value.getPozycje());
    finish(id, before, check);
    return get(id);
  }

  @Transactional
  public Zamowienie update(Integer id, ZamowienieUpdate value) {
    var before = begin(id);
    write(id, parameters(value));
    var check = replaceLines(id, value.getPozycje());
    finish(id, before, check);
    return get(id);
  }

  @Transactional
  public void delete(Integer id) {
    begin(id);
    jdbc.update("DELETE FROM Zamowienie WHERE id_zamowienia=:id", params("id", id));
  }

  private Set<Integer> replaceLines(Integer id, List<SzczegolZamowieniaCreate> items) {
    if (items == null || items.isEmpty()) throw bad("Wymagane pozycje");
    jdbc.update("DELETE FROM Szczegol_Zamowienia WHERE id_zamowienia=:id", params("id", id));
    Set<Integer> check = new HashSet<>();
    for (var item : items) {
      requireValue(item, "pozycja");
      requireValue(item.getIdProduktu(), "id_produktu");
      requireId(item.getIdProduktu());
      int quantity = item.getIlosc() == null ? 1 : item.getIlosc();
      checkNumber(quantity, 1, false);
      if (item.getIdRecepty() != null) recepta.read(item.getIdRecepty(), false);
      var price = produkt.read(item.getIdProduktu(), false).getCena();
      var key = new GeneratedKeyHolder();
      jdbc.update(
          "INSERT INTO Szczegol_Zamowienia"
              + " (id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa) VALUES"
              + " (:order,:product,:recipe,:quantity,:price)",
          new MapSqlParameterSource(
              params(
                  "order",
                  id,
                  "product",
                  item.getIdProduktu(),
                  "recipe",
                  item.getIdRecepty(),
                  "quantity",
                  quantity,
                  "price",
                  price)),
          key,
          new String[] {"id_szczegolu"});
      check.add(Objects.requireNonNull(key.getKey()).intValue());
    }
    return check;
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

  private static void checkNumber(Number value, int min, boolean money) {
    BigDecimal d = new BigDecimal(value.toString());
    if (d.compareTo(BigDecimal.valueOf(min)) < 0) throw bad("Niepoprawna liczba");
    if (money
        && (d.stripTrailingZeros().scale() > 2 || d.compareTo(new BigDecimal("99999999.99")) > 0))
      throw bad("Niepoprawna cena");
  }
}
