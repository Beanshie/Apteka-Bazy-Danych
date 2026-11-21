package pk.bd.apteka.service;

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
public class SzczegolZamowieniaService {

  private final NamedParameterJdbcTemplate jdbc;
  private final ProduktService produkt;

  public SzczegolZamowieniaService(NamedParameterJdbcTemplate jdbc, ProduktService produkt) {
    this.jdbc = jdbc;
    this.produkt = produkt;
  }

  public SzczegolZamowienia get(Integer id, Integer idZamowienia) {
    if (idZamowienia == null || idZamowienia < 1) throw new IllegalArgumentException("Niepoprawne ID");
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
    try {
      return jdbc.queryForObject(
          "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa"
              + " FROM Szczegol_Zamowienia WHERE id_szczegolu=:id AND id_zamowienia=:id_zamowienia",
          new MapSqlParameterSource("id", id).addValue("id_zamowienia", idZamowienia),
          new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono rekordu");
    }
  }

  public List<SzczegolZamowienia> list(Integer idZamowienia) {
    if (idZamowienia == null || idZamowienia < 1) throw new IllegalArgumentException("Niepoprawne ID");
    return jdbc.query(
        "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa"
            + " FROM Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
        new MapSqlParameterSource("id", idZamowienia),
        new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
  }

  @Transactional
  public SzczegolZamowienia create(SzczegolZamowieniaCreate value, Integer idZamowienia) {
    if (idZamowienia == null || idZamowienia < 1) throw new IllegalArgumentException("Niepoprawne ID");
    if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
    var prescription = value.getIdRecepty() == null ? null : lockPrescription(value.getIdRecepty());
    Map<String, Object> parameters = new LinkedHashMap<>();
    parameters.put("id_produktu", value.getIdProduktu());
    parameters.put("id_recepty", value.getIdRecepty());
    parameters.put("ilosc", value.getIlosc() == null ? 1 : value.getIlosc());
    parameters.put("id_zamowienia", idZamowienia);
    var key = new GeneratedKeyHolder();
    try {
      jdbc.update(
          "INSERT INTO Szczegol_Zamowienia"
              + " (id_produktu,id_recepty,ilosc,id_zamowienia,cena_jednostkowa)"
              + " VALUES (:id_produktu,:id_recepty,:ilosc,:id_zamowienia,NULL)",
          new MapSqlParameterSource(parameters),
          key,
          new String[] {"id_szczegolu"});
      int id = Objects.requireNonNull(key.getKey()).intValue();
      var item = get(id, idZamowienia);
      var order =
          jdbc.queryForObject(
              "SELECT id_klienta,status_zamowienia FROM Zamowienie WHERE id_zamowienia=:id",
              new MapSqlParameterSource("id", idZamowienia),
              new BeanPropertyRowMapper<>(Zamowienie.class));
      var selectedProduct = produkt.get(value.getIdProduktu());
      validatePrescription(order, selectedProduct, prescription, true);
      return item;
    } catch (DataIntegrityViolationException e) {
      throw OrderItemConstraintErrors.translateCreate(e);
    }
  }

  @Transactional
  public SzczegolZamowienia update(
      Integer id, SzczegolZamowieniaUpdate value, Integer idZamowienia) {
    if (idZamowienia == null || idZamowienia < 1) throw new IllegalArgumentException("Niepoprawne ID");
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
    if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
    var order = lockOrder(idZamowienia);
    lockProducts();
    var previousItem = get(id, idZamowienia);
    var selectedProduct = produkt.get(value.getIdProduktu());
    var prescription = value.getIdRecepty() == null ? null : lockPrescription(value.getIdRecepty());
    boolean productChanged = !Objects.equals(value.getIdProduktu(), previousItem.getIdProduktu());
    Map<String, Object> parameters = new LinkedHashMap<>();
    parameters.put("id_produktu", value.getIdProduktu());
    parameters.put("id_recepty", value.getIdRecepty());
    parameters.put("ilosc", value.getIlosc());
    parameters.put(
        "cena_jednostkowa",
        productChanged ? selectedProduct.getCena() : previousItem.getCenaJednostkowa());
    parameters.put("id_zamowienia", idZamowienia);
    parameters.put("_id", id);
    try {
      jdbc.update(
          "UPDATE Szczegol_Zamowienia SET id_produktu=:id_produktu,id_recepty=:id_recepty,"
              + "ilosc=:ilosc,cena_jednostkowa=:cena_jednostkowa"
              + " WHERE id_szczegolu=:_id AND id_zamowienia=:id_zamowienia",
          new MapSqlParameterSource(parameters));
    } catch (DataIntegrityViolationException error) {
      throw OrderItemConstraintErrors.translate(error);
    }
    // Read the quantity only after the database has checked NOT NULL and CHECK constraints.
    var item = get(id, idZamowienia);
    boolean requiresValidation =
        productChanged
            || !Objects.equals(item.getIdRecepty(), previousItem.getIdRecepty())
            || item.getIlosc() > previousItem.getIlosc();
    validatePrescription(order, selectedProduct, prescription, requiresValidation);
    return item;
  }

  @Transactional
  public void delete(Integer id, Integer idZamowienia) {
    if (idZamowienia == null || idZamowienia < 1) throw new IllegalArgumentException("Niepoprawne ID");
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
    lockOrder(idZamowienia);
    lockProducts();
    jdbc.update(
        "DELETE FROM Szczegol_Zamowienia WHERE id_szczegolu=:id AND id_zamowienia=:id_zamowienia",
        new MapSqlParameterSource("id", id).addValue("id_zamowienia", idZamowienia));
  }

  private Zamowienie lockOrder(Integer id) {
    try {
      return jdbc.queryForObject(
          "SELECT id_klienta,status_zamowienia FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
          new MapSqlParameterSource("id", id),
          new BeanPropertyRowMapper<>(Zamowienie.class));
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono rekordu");
    }
  }

  private void lockProducts() {
    // Use the same product lock order as ZamowienieService.
    jdbc.queryForList("SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
  }

  private Recepta lockPrescription(Integer id) {
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
    try {
      return jdbc.queryForObject(
          "SELECT id_klienta,status_recepty FROM Recepta WHERE id_recepty=:id FOR UPDATE",
          new MapSqlParameterSource("id", id),
          new BeanPropertyRowMapper<>(Recepta.class));
    } catch (EmptyResultDataAccessException error) {
      throw new NoSuchElementException("Nie znaleziono rekordu");
    }
  }

  private void validatePrescription(
      Zamowienie order, Produkt selectedProduct, Recepta prescription, boolean requiresValidation) {
    if (prescription != null) {
      if (!Objects.equals(prescription.getIdKlienta(), order.getIdKlienta()))
        throw new IllegalArgumentException("Recepta należy do innego klienta");
      if (requiresValidation
          && prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.NOWA
          && prescription.getStatusRecepty() != Recepta.StatusReceptyEnum.ZWERYFIKOWANA)
        throw new IllegalArgumentException("Recepta nie jest ważna");
    }
    if (requiresValidation && selectedProduct.getWymagaRecepty() && prescription == null)
      throw new IllegalArgumentException("Wymagana recepta");
  }
}
