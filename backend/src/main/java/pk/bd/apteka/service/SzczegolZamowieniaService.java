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

  public SzczegolZamowieniaService(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
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
    if (value.getIdRecepty() != null && value.getIdRecepty() < 1)
      throw new IllegalArgumentException("Niepoprawne ID");
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
      return get(id, idZamowienia);
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
    if (value.getIdProduktu() == null || value.getIdProduktu() < 1)
      throw new IllegalArgumentException("Niepoprawne ID");
    if (value.getIdRecepty() != null && value.getIdRecepty() < 1)
      throw new IllegalArgumentException("Niepoprawne ID");
    Map<String, Object> parameters = new LinkedHashMap<>();
    parameters.put("id_produktu", value.getIdProduktu());
    parameters.put("id_recepty", value.getIdRecepty());
    parameters.put("ilosc", value.getIlosc());
    parameters.put("id_zamowienia", idZamowienia);
    parameters.put("_id", id);
    try {
      jdbc.update(
          "UPDATE Szczegol_Zamowienia SET id_produktu=:id_produktu,id_recepty=:id_recepty,"
              + "ilosc=:ilosc,cena_jednostkowa=NULL"
              + " WHERE id_szczegolu=:_id AND id_zamowienia=:id_zamowienia",
          new MapSqlParameterSource(parameters));
    } catch (DataIntegrityViolationException error) {
      throw OrderItemConstraintErrors.translate(error);
    }
    return get(id, idZamowienia);
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
}
