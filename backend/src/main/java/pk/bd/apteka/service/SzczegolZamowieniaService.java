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

  public SzczegolZamowienia get(Integer id, Integer idZamowienia) {
    zamowienie.get(idZamowienia);
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
    try {
      return jdbc.queryForObject(
          "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa"
              + " FROM Szczegol_Zamowienia WHERE id_szczegolu=:id AND id_zamowienia=:order",
          new MapSqlParameterSource("id", id).addValue("order", idZamowienia),
          new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono rekordu");
    }
  }

  public List<SzczegolZamowienia> list(Integer idZamowienia) {
    zamowienie.get(idZamowienia);
    return jdbc.query(
        "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa"
            + " FROM Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
        new MapSqlParameterSource("id", idZamowienia),
        new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
  }

  @Transactional
  public SzczegolZamowienia create(SzczegolZamowieniaCreate value, Integer idZamowienia) {
    if (idZamowienia == null || idZamowienia < 1)
      throw new IllegalArgumentException("Niepoprawne ID");
    Zamowienie order;
    try {
      order =
          jdbc.queryForObject(
              "SELECT id_klienta,id_adresu,status_zamowienia"
                  + " FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
              new MapSqlParameterSource("id", idZamowienia),
              new BeanPropertyRowMapper<>(Zamowienie.class));
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono zamówienia");
    }
    boolean active = order.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE;
    jdbc.queryForList("SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
    if (active) {
      for (var item :
          jdbc.query(
              "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa"
                  + " FROM Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
              new MapSqlParameterSource("id", idZamowienia),
              new BeanPropertyRowMapper<>(SzczegolZamowienia.class))) {
        jdbc.update(
            "UPDATE Produkt SET stan_magazynowy=stan_magazynowy+:quantity WHERE"
                + " id_produktu=:product",
            new MapSqlParameterSource("quantity", item.getIlosc())
                .addValue("product", item.getIdProduktu()));
      }
    }
    if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
    if (value.getIdProduktu() == null || value.getIdProduktu() < 1)
      throw new IllegalArgumentException("Niepoprawne ID produktu");
    var selectedProduct = produkt.get(value.getIdProduktu());
    if (value.getIdRecepty() != null) {
      if (value.getIdRecepty() < 1) throw new IllegalArgumentException("Niepoprawne ID recepty");
      recepta.get(value.getIdRecepty());
    }
    if (value.getIlosc() != null && value.getIlosc() < 1)
      throw new IllegalArgumentException("Niepoprawna ilość");
    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Szczegol_Zamowienia"
            + " (id_produktu,id_recepty,ilosc,id_zamowienia,cena_jednostkowa)"
            + " VALUES (:product,:prescription,:quantity,:order,:price)",
        new MapSqlParameterSource("product", value.getIdProduktu())
            .addValue("prescription", value.getIdRecepty())
            .addValue("quantity", value.getIlosc() == null ? 1 : value.getIlosc())
            .addValue("order", idZamowienia)
            .addValue("price", selectedProduct.getCena()),
        key,
        new String[] {"id_szczegolu"});
    int id = Objects.requireNonNull(key.getKey()).intValue();
    try {
      if (!Objects.equals(
          jdbc.queryForObject(
              "SELECT id_klienta FROM Adres_Klienta WHERE id_adresu=:id FOR UPDATE",
              new MapSqlParameterSource("id", order.getIdAdresu()),
              Integer.class),
          order.getIdKlienta()))
        throw new IllegalArgumentException("Adres należy do innego klienta");
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono adresu");
    }
    var orderItems =
        jdbc.query(
            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa"
                + " FROM Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
            new MapSqlParameterSource("id", idZamowienia),
            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
    for (var item : orderItems) {
      boolean strict = Objects.equals(item.getIdSzczegolu(), id);
      if (item.getIdRecepty() != null) {
        Recepta prescription;
        try {
          prescription =
              jdbc.queryForObject(
                  "SELECT id_klienta,status_recepty FROM Recepta WHERE id_recepty=:id"
                      + " FOR UPDATE",
                  new MapSqlParameterSource("id", item.getIdRecepty()),
                  new BeanPropertyRowMapper<>(Recepta.class));
        } catch (EmptyResultDataAccessException error) {
          throw new NoSuchElementException("Nie znaleziono recepty");
        }
        if (!Objects.equals(prescription.getIdKlienta(), order.getIdKlienta()))
          throw new IllegalArgumentException("Recepta należy do innego klienta");
        if (strict
            && prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.NOWA
            && prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.ZWERYFIKOWANA)
          throw new IllegalArgumentException("Recepta nie jest ważna");
      }
      if (strict && selectedProduct.getWymagaRecepty() && item.getIdRecepty() == null)
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
        new MapSqlParameterSource("total", total).addValue("id", idZamowienia));
    return get(id, idZamowienia);
  }

  @Transactional
  public SzczegolZamowienia update(
      Integer id, SzczegolZamowieniaUpdate value, Integer idZamowienia) {
    if (idZamowienia == null || idZamowienia < 1)
      throw new IllegalArgumentException("Niepoprawne ID");
    Zamowienie order;
    try {
      order =
          jdbc.queryForObject(
              "SELECT id_klienta,id_adresu,status_zamowienia"
                  + " FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
              new MapSqlParameterSource("id", idZamowienia),
              new BeanPropertyRowMapper<>(Zamowienie.class));
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono zamówienia");
    }
    boolean active = order.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE;
    jdbc.queryForList("SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
    if (active) {
      for (var item :
          jdbc.query(
              "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa"
                  + " FROM Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
              new MapSqlParameterSource("id", idZamowienia),
              new BeanPropertyRowMapper<>(SzczegolZamowienia.class))) {
        jdbc.update(
            "UPDATE Produkt SET stan_magazynowy=stan_magazynowy+:quantity WHERE"
                + " id_produktu=:product",
            new MapSqlParameterSource("quantity", item.getIlosc())
                .addValue("product", item.getIdProduktu()));
      }
    }
    var previousItem = get(id, idZamowienia);
    if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
    if (value.getIdProduktu() == null || value.getIdProduktu() < 1)
      throw new IllegalArgumentException("Niepoprawne ID produktu");
    var selectedProduct = produkt.get(value.getIdProduktu());
    if (value.getIdRecepty() != null) {
      if (value.getIdRecepty() < 1) throw new IllegalArgumentException("Niepoprawne ID recepty");
      recepta.get(value.getIdRecepty());
    }
    if (value.getIlosc() == null || value.getIlosc() < 1)
      throw new IllegalArgumentException("Niepoprawna ilość");
    boolean productChanged = !Objects.equals(value.getIdProduktu(), previousItem.getIdProduktu());
    jdbc.update(
        "UPDATE Szczegol_Zamowienia SET id_produktu=:product,id_recepty=:prescription,"
            + "ilosc=:quantity,cena_jednostkowa=:price WHERE id_szczegolu=:id",
        new MapSqlParameterSource("id", id)
            .addValue("product", value.getIdProduktu())
            .addValue("prescription", value.getIdRecepty())
            .addValue("quantity", value.getIlosc())
            .addValue(
                "price",
                productChanged ? selectedProduct.getCena() : previousItem.getCenaJednostkowa()));
    boolean requiresValidation =
        productChanged
            || !Objects.equals(value.getIdRecepty(), previousItem.getIdRecepty())
            || value.getIlosc() > previousItem.getIlosc();
    try {
      if (!Objects.equals(
          jdbc.queryForObject(
              "SELECT id_klienta FROM Adres_Klienta WHERE id_adresu=:id FOR UPDATE",
              new MapSqlParameterSource("id", order.getIdAdresu()),
              Integer.class),
          order.getIdKlienta()))
        throw new IllegalArgumentException("Adres należy do innego klienta");
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono adresu");
    }
    var orderItems =
        jdbc.query(
            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa"
                + " FROM Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
            new MapSqlParameterSource("id", idZamowienia),
            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
    for (var item : orderItems) {
      boolean strict = requiresValidation && Objects.equals(item.getIdSzczegolu(), id);
      if (item.getIdRecepty() != null) {
        Recepta prescription;
        try {
          prescription =
              jdbc.queryForObject(
                  "SELECT id_klienta,status_recepty FROM Recepta WHERE id_recepty=:id"
                      + " FOR UPDATE",
                  new MapSqlParameterSource("id", item.getIdRecepty()),
                  new BeanPropertyRowMapper<>(Recepta.class));
        } catch (EmptyResultDataAccessException error) {
          throw new NoSuchElementException("Nie znaleziono recepty");
        }
        if (!Objects.equals(prescription.getIdKlienta(), order.getIdKlienta()))
          throw new IllegalArgumentException("Recepta należy do innego klienta");
        if (strict
            && prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.NOWA
            && prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.ZWERYFIKOWANA)
          throw new IllegalArgumentException("Recepta nie jest ważna");
      }
      if (strict && selectedProduct.getWymagaRecepty() && item.getIdRecepty() == null)
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
        new MapSqlParameterSource("total", total).addValue("id", idZamowienia));
    return get(id, idZamowienia);
  }

  @Transactional
  public void delete(Integer id, Integer idZamowienia) {
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
    if (idZamowienia == null || idZamowienia < 1)
      throw new IllegalArgumentException("Niepoprawne ID");
    Zamowienie order;
    try {
      order =
          jdbc.queryForObject(
              "SELECT id_klienta,id_adresu,status_zamowienia"
                  + " FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
              new MapSqlParameterSource("id", idZamowienia),
              new BeanPropertyRowMapper<>(Zamowienie.class));
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono zamówienia");
    }
    if (jdbc.queryForList(
            "SELECT id_szczegolu FROM Szczegol_Zamowienia WHERE id_szczegolu=:id AND"
                + " id_zamowienia=:order",
            new MapSqlParameterSource("id", id).addValue("order", idZamowienia))
        .isEmpty()) return;
    boolean active = order.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE;
    jdbc.queryForList("SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
    if (active) {
      for (var item :
          jdbc.query(
              "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa"
                  + " FROM Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
              new MapSqlParameterSource("id", idZamowienia),
              new BeanPropertyRowMapper<>(SzczegolZamowienia.class))) {
        jdbc.update(
            "UPDATE Produkt SET stan_magazynowy=stan_magazynowy+:quantity WHERE"
                + " id_produktu=:product",
            new MapSqlParameterSource("quantity", item.getIlosc())
                .addValue("product", item.getIdProduktu()));
      }
    }
    jdbc.update(
        "DELETE FROM Szczegol_Zamowienia WHERE id_szczegolu=:id AND id_zamowienia=:order",
        new MapSqlParameterSource("id", id).addValue("order", idZamowienia));
    try {
      if (!Objects.equals(
          jdbc.queryForObject(
              "SELECT id_klienta FROM Adres_Klienta WHERE id_adresu=:id FOR UPDATE",
              new MapSqlParameterSource("id", order.getIdAdresu()),
              Integer.class),
          order.getIdKlienta()))
        throw new IllegalArgumentException("Adres należy do innego klienta");
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono adresu");
    }
    var orderItems =
        jdbc.query(
            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa"
                + " FROM Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
            new MapSqlParameterSource("id", idZamowienia),
            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
    for (var item : orderItems) {
      if (item.getIdRecepty() != null) {
        try {
          if (!Objects.equals(
              jdbc.queryForObject(
                  "SELECT id_klienta FROM Recepta WHERE id_recepty=:id FOR UPDATE",
                  new MapSqlParameterSource("id", item.getIdRecepty()),
                  Integer.class),
              order.getIdKlienta()))
            throw new IllegalArgumentException("Recepta należy do innego klienta");
        } catch (EmptyResultDataAccessException error) {
          throw new NoSuchElementException("Nie znaleziono recepty");
        }
      }
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
        new MapSqlParameterSource("total", total).addValue("id", idZamowienia));
  }
}
