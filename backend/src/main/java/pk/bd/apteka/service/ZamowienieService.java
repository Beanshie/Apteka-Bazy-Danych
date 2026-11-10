package pk.bd.apteka.service;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

  public Zamowienie get(Integer id) {
    Zamowienie record;
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
    try {
      record =
          jdbc.queryForObject(
              "SELECT"
                  + " id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita"
                  + " FROM Zamowienie WHERE id_zamowienia=:id",
              new MapSqlParameterSource("id", id),
              (rs, index) -> {
                var mappedRecord = new Zamowienie();
                mappedRecord.setIdZamowienia(rs.getObject("id_zamowienia", Integer.class));
                mappedRecord.setIdKlienta(rs.getObject("id_klienta", Integer.class));
                mappedRecord.setIdAdresu(rs.getObject("id_adresu", Integer.class));
                mappedRecord.setDataZamowienia(
                    rs.getTimestamp("data_zamowienia") == null
                        ? null
                        : rs.getTimestamp("data_zamowienia")
                            .toLocalDateTime()
                            .atOffset(java.time.ZoneOffset.UTC));
                mappedRecord.setStatusZamowienia(
                    Zamowienie.StatusZamowieniaEnum.fromValue(rs.getString("status_zamowienia")));
                mappedRecord.setSumaCalkowita(rs.getBigDecimal("suma_calkowita"));
                return mappedRecord;
              });
    } catch (EmptyResultDataAccessException errorItem) {
      throw new NoSuchElementException("Nie znaleziono rekordu");
    }
    record.setPozycje(
        jdbc.query(
            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
            new MapSqlParameterSource("id", id),
            new BeanPropertyRowMapper<>(SzczegolZamowienia.class)));
    return record;
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
    var result =
        jdbc.query(
            sql.toString(),
            parameters,
            (rs, index) -> {
              var mappedRecord = new Zamowienie();
              mappedRecord.setIdZamowienia(rs.getObject("id_zamowienia", Integer.class));
              mappedRecord.setIdKlienta(rs.getObject("id_klienta", Integer.class));
              mappedRecord.setIdAdresu(rs.getObject("id_adresu", Integer.class));
              mappedRecord.setDataZamowienia(
                  rs.getTimestamp("data_zamowienia") == null
                      ? null
                      : rs.getTimestamp("data_zamowienia")
                          .toLocalDateTime()
                          .atOffset(java.time.ZoneOffset.UTC));
              mappedRecord.setStatusZamowienia(
                  Zamowienie.StatusZamowieniaEnum.fromValue(rs.getString("status_zamowienia")));
              mappedRecord.setSumaCalkowita(rs.getBigDecimal("suma_calkowita"));
              return mappedRecord;
            });
    for (var value : result) {
      value.setPozycje(
          jdbc.query(
              "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                  + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
              new MapSqlParameterSource("id", value.getIdZamowienia()),
              new BeanPropertyRowMapper<>(SzczegolZamowienia.class)));
    }
    return result;
  }

  @Transactional
  public Zamowienie create(ZamowienieCreate value) {

    if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
    if (value.getIdKlienta() == null)
      throw new IllegalArgumentException("Pole nie może być null: id_klienta");
    if (value.getIdKlienta() < 1) throw new IllegalArgumentException("Niepoprawna liczba");
    klient.get(value.getIdKlienta());
    if (value.getIdAdresu() == null)
      throw new IllegalArgumentException("Pole nie może być null: id_adresu");
    if (value.getIdAdresu() < 1) throw new IllegalArgumentException("Niepoprawna liczba");
    adresklienta.get(value.getIdAdresu());

    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Zamowienie (id_klienta,id_adresu,status_zamowienia)"
            + " VALUES (:client,:address,:status)",
        new MapSqlParameterSource("client", value.getIdKlienta())
            .addValue("address", value.getIdAdresu())
            .addValue(
                "status",
                value.getStatusZamowienia() == null
                    ? "NOWE"
                    : value.getStatusZamowienia().getValue()),
        key,
        new String[] {"id_zamowienia"});
    int id = Objects.requireNonNull(key.getKey()).intValue();
    jdbc.queryForList("SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
    boolean active = value.getStatusZamowienia() != ZamowienieCreate.StatusZamowieniaEnum.ANULOWANE;

    if (value.getPozycje() == null || value.getPozycje().isEmpty())
      throw new IllegalArgumentException("Wymagane pozycje");
    for (var item : value.getPozycje()) {
      if (item == null) throw new IllegalArgumentException("Pole nie może być null: pozycja");
      if (item.getIdProduktu() == null)
        throw new IllegalArgumentException("Pole nie może być null: id_produktu");
      if (item.getIdProduktu() < 1) throw new IllegalArgumentException("Niepoprawne ID");
      int quantity = item.getIlosc() == null ? 1 : item.getIlosc();
      if (quantity < 1) throw new IllegalArgumentException("Niepoprawna liczba");
      if (item.getIdRecepty() != null) recepta.get(item.getIdRecepty());
      jdbc.update(
          "INSERT INTO Szczegol_Zamowienia"
              + " (id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa)"
              + " VALUES (:order,:product,:prescription,:quantity,:price)",
          new MapSqlParameterSource("order", id)
              .addValue("product", item.getIdProduktu())
              .addValue("prescription", item.getIdRecepty())
              .addValue("quantity", quantity)
              .addValue("price", produkt.get(item.getIdProduktu()).getCena()));
    }
    try {
      if (!Objects.equals(
          jdbc.queryForObject(
              "SELECT id_klienta FROM Adres_Klienta WHERE id_adresu=:id FOR UPDATE",
              new MapSqlParameterSource("id", value.getIdAdresu()),
              Integer.class),
          value.getIdKlienta()))
        throw new IllegalArgumentException("Adres należy do innego klienta");
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono rekordu");
    }
    var orderItems =
        jdbc.query(
            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa"
                + " FROM Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
            new MapSqlParameterSource("id", id),
            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
    for (var item : orderItems) {
      var selectedProduct = produkt.get(item.getIdProduktu());
      if (item.getIdRecepty() != null) {
        if (item.getIdRecepty() < 1) throw new IllegalArgumentException("Niepoprawne ID");
        Recepta prescription;
        try {
          prescription =
              jdbc.queryForObject(
                  "SELECT id_klienta,status_recepty FROM Recepta WHERE id_recepty=:id FOR UPDATE",
                  new MapSqlParameterSource("id", item.getIdRecepty()),
                  new BeanPropertyRowMapper<>(Recepta.class));
        } catch (EmptyResultDataAccessException error) {
          throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (!Objects.equals(prescription.getIdKlienta(), value.getIdKlienta()))
          throw new IllegalArgumentException("Recepta należy do innego klienta");
        if (prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.NOWA
            && prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.ZWERYFIKOWANA)
          throw new IllegalArgumentException("Recepta nie jest ważna");
      }
      if (selectedProduct.getWymagaRecepty() && item.getIdRecepty() == null)
        throw new IllegalArgumentException("Wymagana recepta");
    }
    if (active) {
      for (var item : orderItems) {
        if (jdbc.update(
                "UPDATE Produkt SET stan_magazynowy=stan_magazynowy-:quantity"
                    + " WHERE id_produktu=:product AND stan_magazynowy>=:quantity",
                new MapSqlParameterSource("quantity", item.getIlosc())
                    .addValue("product", item.getIdProduktu()))
            == 0) throw new IllegalArgumentException("Brak produktu na stanie");
      }
    }
    BigDecimal total =
        orderItems.stream()
            .map(item -> item.getCenaJednostkowa().multiply(BigDecimal.valueOf(item.getIlosc())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(new BigDecimal("99999999.99")) > 0)
      throw new IllegalArgumentException("Zbyt duża suma");
    jdbc.update(
        "UPDATE Zamowienie SET suma_calkowita=:total WHERE id_zamowienia=:id",
        new MapSqlParameterSource("total", total).addValue("id", id));
    return get(id);
  }

  @Transactional
  public Zamowienie update(Integer id, ZamowienieUpdate value) {
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
    String previousStatus;
    try {
      previousStatus =
          jdbc.queryForObject(
              "SELECT status_zamowienia FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
              new MapSqlParameterSource("id", id),
              String.class);
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono rekordu");
    }

    jdbc.queryForList("SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
    if (!"ANULOWANE".equals(previousStatus)) {
      for (var item :
          jdbc.query(
              "SELECT id_produktu,ilosc FROM Szczegol_Zamowienia"
                  + " WHERE id_zamowienia=:id ORDER BY id_szczegolu",
              new MapSqlParameterSource("id", id),
              new BeanPropertyRowMapper<>(SzczegolZamowienia.class))) {
        jdbc.update(
            "UPDATE Produkt SET stan_magazynowy=stan_magazynowy+:quantity WHERE"
                + " id_produktu=:product",
            new MapSqlParameterSource("quantity", item.getIlosc())
                .addValue("product", item.getIdProduktu()));
      }
    }

    if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
    if (value.getIdKlienta() == null)
      throw new IllegalArgumentException("Pole nie może być null: id_klienta");
    if (value.getIdKlienta() < 1) throw new IllegalArgumentException("Niepoprawna liczba");
    klient.get(value.getIdKlienta());
    if (value.getIdAdresu() == null)
      throw new IllegalArgumentException("Pole nie może być null: id_adresu");
    if (value.getIdAdresu() < 1) throw new IllegalArgumentException("Niepoprawna liczba");
    adresklienta.get(value.getIdAdresu());

    if (value.getStatusZamowienia() == null)
      throw new IllegalArgumentException("Pole nie może być null: status_zamowienia");
    jdbc.update(
        "UPDATE Zamowienie SET id_klienta=:client,id_adresu=:address,status_zamowienia=:status"
            + " WHERE id_zamowienia=:id",
        new MapSqlParameterSource("id", id)
            .addValue("client", value.getIdKlienta())
            .addValue("address", value.getIdAdresu())
            .addValue("status", value.getStatusZamowienia().getValue()));
    if (value.getPozycje() == null || value.getPozycje().isEmpty())
      throw new IllegalArgumentException("Wymagane pozycje");
    jdbc.update(
        "DELETE FROM Szczegol_Zamowienia WHERE id_zamowienia=:id",
        new MapSqlParameterSource("id", id));
    boolean active = value.getStatusZamowienia() != ZamowienieUpdate.StatusZamowieniaEnum.ANULOWANE;

    for (var item : value.getPozycje()) {
      if (item == null) throw new IllegalArgumentException("Pole nie może być null: pozycja");
      if (item.getIdProduktu() == null)
        throw new IllegalArgumentException("Pole nie może być null: id_produktu");
      if (item.getIdProduktu() < 1) throw new IllegalArgumentException("Niepoprawne ID");
      int quantity = item.getIlosc() == null ? 1 : item.getIlosc();
      if (quantity < 1) throw new IllegalArgumentException("Niepoprawna liczba");
      if (item.getIdRecepty() != null) recepta.get(item.getIdRecepty());
      jdbc.update(
          "INSERT INTO Szczegol_Zamowienia"
              + " (id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa)"
              + " VALUES (:order,:product,:prescription,:quantity,:price)",
          new MapSqlParameterSource("order", id)
              .addValue("product", item.getIdProduktu())
              .addValue("prescription", item.getIdRecepty())
              .addValue("quantity", quantity)
              .addValue("price", produkt.get(item.getIdProduktu()).getCena()));
    }
    try {
      if (!Objects.equals(
          jdbc.queryForObject(
              "SELECT id_klienta FROM Adres_Klienta WHERE id_adresu=:id FOR UPDATE",
              new MapSqlParameterSource("id", value.getIdAdresu()),
              Integer.class),
          value.getIdKlienta()))
        throw new IllegalArgumentException("Adres należy do innego klienta");
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono rekordu");
    }
    var orderItems =
        jdbc.query(
            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa"
                + " FROM Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
            new MapSqlParameterSource("id", id),
            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
    for (var item : orderItems) {
      var selectedProduct = produkt.get(item.getIdProduktu());
      if (item.getIdRecepty() != null) {
        if (item.getIdRecepty() < 1) throw new IllegalArgumentException("Niepoprawne ID");
        Recepta prescription;
        try {
          prescription =
              jdbc.queryForObject(
                  "SELECT id_klienta,status_recepty FROM Recepta WHERE id_recepty=:id FOR UPDATE",
                  new MapSqlParameterSource("id", item.getIdRecepty()),
                  new BeanPropertyRowMapper<>(Recepta.class));
        } catch (EmptyResultDataAccessException error) {
          throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (!Objects.equals(prescription.getIdKlienta(), value.getIdKlienta()))
          throw new IllegalArgumentException("Recepta należy do innego klienta");
        if (prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.NOWA
            && prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.ZWERYFIKOWANA)
          throw new IllegalArgumentException("Recepta nie jest ważna");
      }
      if (selectedProduct.getWymagaRecepty() && item.getIdRecepty() == null)
        throw new IllegalArgumentException("Wymagana recepta");
    }
    if (active) {
      for (var item : orderItems) {
        if (jdbc.update(
                "UPDATE Produkt SET stan_magazynowy=stan_magazynowy-:quantity"
                    + " WHERE id_produktu=:product AND stan_magazynowy>=:quantity",
                new MapSqlParameterSource("quantity", item.getIlosc())
                    .addValue("product", item.getIdProduktu()))
            == 0) throw new IllegalArgumentException("Brak produktu na stanie");
      }
    }
    BigDecimal total =
        orderItems.stream()
            .map(item -> item.getCenaJednostkowa().multiply(BigDecimal.valueOf(item.getIlosc())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(new BigDecimal("99999999.99")) > 0)
      throw new IllegalArgumentException("Zbyt duża suma");
    jdbc.update(
        "UPDATE Zamowienie SET suma_calkowita=:total WHERE id_zamowienia=:id",
        new MapSqlParameterSource("total", total).addValue("id", id));
    return get(id);
  }

  @Transactional
  public void delete(Integer id) {
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
    String previousStatus;
    try {
      previousStatus =
          jdbc.queryForObject(
              "SELECT status_zamowienia FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
              new MapSqlParameterSource("id", id),
              String.class);
    } catch (EmptyResultDataAccessException error) {
      return;
    }

    jdbc.queryForList("SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
    if (!"ANULOWANE".equals(previousStatus)) {
      for (var item :
          jdbc.query(
              "SELECT id_produktu,ilosc FROM Szczegol_Zamowienia"
                  + " WHERE id_zamowienia=:id ORDER BY id_szczegolu",
              new MapSqlParameterSource("id", id),
              new BeanPropertyRowMapper<>(SzczegolZamowienia.class))) {
        jdbc.update(
            "UPDATE Produkt SET stan_magazynowy=stan_magazynowy+:quantity WHERE"
                + " id_produktu=:product",
            new MapSqlParameterSource("quantity", item.getIlosc())
                .addValue("product", item.getIdProduktu()));
      }
    }

    jdbc.update(
        "DELETE FROM Zamowienie WHERE id_zamowienia=:id", new MapSqlParameterSource("id", id));
  }
}
