package pk.bd.apteka.service;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.bd.apteka.dto.*;

@Service
public class ZamowienieService {

  private static final String ORDER_COLUMNS =
      "id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita";
  private static final String ITEM_COLUMNS =
      "id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa";

  private final NamedParameterJdbcTemplate jdbc;
  private final KlientService klient;
  private final ProduktService produkt;

  public ZamowienieService(
      NamedParameterJdbcTemplate jdbc, KlientService klient, ProduktService produkt) {
    this.jdbc = jdbc;
    this.klient = klient;
    this.produkt = produkt;
  }

  public Zamowienie get(Integer id) {
    validateId(id);
    Zamowienie order;
    try {
      order =
          jdbc.queryForObject(
              "SELECT " + ORDER_COLUMNS + " FROM Zamowienie WHERE id_zamowienia=:id",
              new MapSqlParameterSource("id", id),
              new BeanPropertyRowMapper<>(Zamowienie.class) {
                @Override
                protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor property)
                    throws SQLException {
                  if ("dataZamowienia".equals(property.getName())) {
                    var timestamp = rs.getTimestamp(index);
                    return timestamp == null
                        ? null
                        : timestamp.toLocalDateTime().atOffset(ZoneOffset.UTC);
                  }
                  return super.getColumnValue(rs, index, property);
                }
              });
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono rekordu");
    }
    order.setPozycje(loadItems(id));
    return order;
  }

  public List<Zamowienie> list(Integer idKlienta, String statusZamowienia) {
    StringBuilder sql =
        new StringBuilder("SELECT " + ORDER_COLUMNS + " FROM Zamowienie WHERE 1=1");
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
    var orders =
        jdbc.query(sql.toString(), parameters, new BeanPropertyRowMapper<>(Zamowienie.class) {
          @Override
          protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor property)
              throws SQLException {
            if ("dataZamowienia".equals(property.getName())) {
              var timestamp = rs.getTimestamp(index);
              return timestamp == null
                  ? null
                  : timestamp.toLocalDateTime().atOffset(ZoneOffset.UTC);
            }
            return super.getColumnValue(rs, index, property);
          }
        });
    if (orders.isEmpty()) return orders;

    Map<Integer, Zamowienie> byId = new LinkedHashMap<>();
    for (var order : orders) {
      order.setPozycje(new ArrayList<>());
      byId.put(order.getIdZamowienia(), order);
    }
    var items =
        jdbc.query(
            "SELECT " + ITEM_COLUMNS + " FROM Szczegol_Zamowienia"
                + " WHERE id_zamowienia IN (:orders) ORDER BY id_szczegolu",
            new MapSqlParameterSource("orders", byId.keySet()),
            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
    for (var item : items) {
      byId.get(item.getIdZamowienia()).getPozycje().add(item);
    }
    return orders;
  }

  @Transactional
  public Zamowienie create(ZamowienieCreate value) {
    if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
    validateItems(value.getPozycje());
    // Acquire product locks before the new header's foreign keys lock its address.
    lockProducts();
    validateAddress(value.getIdAdresu(), value.getIdKlienta());

    Map<String, Object> parameters = new LinkedHashMap<>();
    parameters.put("id_klienta", value.getIdKlienta());
    parameters.put("id_adresu", value.getIdAdresu());
    parameters.put(
        "status_zamowienia",
        value.getStatusZamowienia() == null ? "NOWE" : value.getStatusZamowienia().getValue());
    var key = new GeneratedKeyHolder();
    try {
      jdbc.update(
          "INSERT INTO Zamowienie (id_klienta,id_adresu,status_zamowienia)"
              + " VALUES (:id_klienta,:id_adresu,:status_zamowienia)",
          new MapSqlParameterSource(parameters),
          key,
          new String[] {"id_zamowienia"});
      int id = Objects.requireNonNull(key.getKey()).intValue();
      insertItems(id, value.getIdKlienta(), value.getPozycje());
      return get(id);
    } catch (DataIntegrityViolationException e) {
      throw OrderItemConstraintErrors.translateCreate(e);
    }
  }

  @Transactional
  public Zamowienie update(Integer id, ZamowienieUpdate value) {
    validateId(id);
    if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
    validateItems(value.getPozycje());
    lockOrder(id);
    lockProducts();
    klient.get(value.getIdKlienta());
    validateAddress(value.getIdAdresu(), value.getIdKlienta());

    String status =
        value.getStatusZamowienia() == null ? null : value.getStatusZamowienia().getValue();
    Map<String, Object> parameters = new LinkedHashMap<>();
    parameters.put("id_klienta", value.getIdKlienta());
    parameters.put("id_adresu", value.getIdAdresu());
    parameters.put("status_zamowienia", status);
    parameters.put("_id", id);
    try {
      // Release reservations using the old status before inserting under the new status.
      jdbc.update(
          "DELETE FROM Szczegol_Zamowienia WHERE id_zamowienia=:id",
          new MapSqlParameterSource("id", id));
      jdbc.update(
          "UPDATE Zamowienie SET id_klienta=:id_klienta,id_adresu=:id_adresu,"
              + "status_zamowienia=:status_zamowienia WHERE id_zamowienia=:_id",
          new MapSqlParameterSource(parameters));
      insertItems(id, value.getIdKlienta(), value.getPozycje());
    } catch (DataIntegrityViolationException error) {
      throw OrderItemConstraintErrors.translate(error);
    }
    return get(id);
  }

  @Transactional
  public void delete(Integer id) {
    validateId(id);
    try {
      lockOrder(id);
    } catch (NoSuchElementException error) {
      return;
    }
    lockProducts();
    // Foreign-key cascades do not execute the item-delete trigger.
    jdbc.update(
        "DELETE FROM Szczegol_Zamowienia WHERE id_zamowienia=:id",
        new MapSqlParameterSource("id", id));
    jdbc.update(
        "DELETE FROM Zamowienie WHERE id_zamowienia=:id", new MapSqlParameterSource("id", id));
  }

  private void validateId(Integer id) {
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
  }

  private void validateItems(List<SzczegolZamowieniaCreate> items) {
    if (items == null || items.isEmpty()) throw new IllegalArgumentException("Wymagane pozycje");
    for (var item : items) {
      if (item == null) throw new IllegalArgumentException("Pole nie może być null: pozycja");
    }
  }

  private String lockOrder(Integer id) {
    try {
      return jdbc.queryForObject(
          "SELECT status_zamowienia FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
          new MapSqlParameterSource("id", id),
          String.class);
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono rekordu");
    }
  }

  private void lockProducts() {
    // Use the same product lock order as SzczegolZamowieniaService.
    jdbc.queryForList("SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
  }

  private void validateAddress(Integer idAdresu, Integer idKlienta) {
    validateId(idAdresu);
    try {
      Integer owner =
          jdbc.queryForObject(
              "SELECT id_klienta FROM Adres_Klienta WHERE id_adresu=:id FOR UPDATE",
              new MapSqlParameterSource("id", idAdresu),
              Integer.class);
      if (!Objects.equals(owner, idKlienta))
        throw new IllegalArgumentException("Adres należy do innego klienta");
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono rekordu");
    }
  }

  private Recepta lockPrescription(Integer id) {
    validateId(id);
    try {
      return jdbc.queryForObject(
          "SELECT id_klienta,status_recepty FROM Recepta WHERE id_recepty=:id FOR UPDATE",
          new MapSqlParameterSource("id", id),
          new BeanPropertyRowMapper<>(Recepta.class));
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono rekordu");
    }
  }

  private void insertItems(
      Integer id, Integer idKlienta, List<SzczegolZamowieniaCreate> items) {
    Map<Integer, Produkt> products = new HashMap<>();
    Map<Integer, Recepta> prescriptions = new HashMap<>();
    for (var item : items) {
      var selectedProduct = products.computeIfAbsent(item.getIdProduktu(), produkt::get);
      Recepta prescription =
          item.getIdRecepty() == null
              ? null
              : prescriptions.computeIfAbsent(item.getIdRecepty(), this::lockPrescription);
      validatePrescription(idKlienta, selectedProduct, prescription);
      Map<String, Object> parameters = new LinkedHashMap<>();
      parameters.put("id_zamowienia", id);
      parameters.put("id_produktu", item.getIdProduktu());
      parameters.put("id_recepty", item.getIdRecepty());
      parameters.put("ilosc", item.getIlosc() == null ? 1 : item.getIlosc());
      jdbc.update(
          "INSERT INTO Szczegol_Zamowienia"
              + " (id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa)"
              + " VALUES (:id_zamowienia,:id_produktu,:id_recepty,:ilosc,NULL)",
          new MapSqlParameterSource(parameters));
    }
  }

  private void validatePrescription(Integer idKlienta, Produkt product, Recepta prescription) {
    if (prescription != null) {
      if (!Objects.equals(prescription.getIdKlienta(), idKlienta))
        throw new IllegalArgumentException("Recepta należy do innego klienta");
      if (prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.NOWA
          && prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.ZWERYFIKOWANA)
        throw new IllegalArgumentException("Recepta nie jest ważna");
    }
    if (product.getWymagaRecepty() && prescription == null)
      throw new IllegalArgumentException("Wymagana recepta");
  }

  private List<SzczegolZamowienia> loadItems(Integer id) {
    return jdbc.query(
        "SELECT " + ITEM_COLUMNS + " FROM Szczegol_Zamowienia"
            + " WHERE id_zamowienia=:id ORDER BY id_szczegolu",
        new MapSqlParameterSource("id", id),
        new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
  }
}
