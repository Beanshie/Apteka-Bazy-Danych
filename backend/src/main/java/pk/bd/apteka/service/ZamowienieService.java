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

  private final NamedParameterJdbcTemplate jdbc;

  public ZamowienieService(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Zamowienie get(Integer id) {
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
    Zamowienie order;
    try {
      order =
          jdbc.queryForObject(
              "SELECT " + "id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita" + " FROM Zamowienie WHERE id_zamowienia=:id",
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
    return order;
  }

  public List<Zamowienie> list(Integer idKlienta, String statusZamowienia) {
    StringBuilder sql =
        new StringBuilder("SELECT " + "id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita" + " FROM Zamowienie WHERE 1=1");
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
    return orders;
  }

  @Transactional
  public Zamowienie create(ZamowienieCreate value) {
    if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
    Integer id1 = value.getIdAdresu();
    if (id1 == null || id1 < 1) throw new IllegalArgumentException("Niepoprawne ID");

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
      return get(id);
    } catch (DataIntegrityViolationException e) {
      throw OrderItemConstraintErrors.translateCreate(e);
    }
  }

  @Transactional
  public Zamowienie update(Integer id, ZamowienieUpdate value) {
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
    if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
    Integer id2 = value.getIdKlienta();
    if (id2 == null || id2 < 1) throw new IllegalArgumentException("Niepoprawne ID");
    Integer id1 = value.getIdAdresu();
    if (id1 == null || id1 < 1) throw new IllegalArgumentException("Niepoprawne ID");

    String status =
        value.getStatusZamowienia() == null ? null : value.getStatusZamowienia().getValue();
    Map<String, Object> parameters = new LinkedHashMap<>();
    parameters.put("id_klienta", value.getIdKlienta());
    parameters.put("id_adresu", value.getIdAdresu());
    parameters.put("status_zamowienia", status);
    parameters.put("_id", id);
    try {
      jdbc.update(
          "UPDATE Zamowienie SET id_klienta=:id_klienta,id_adresu=:id_adresu,"
              + "status_zamowienia=:status_zamowienia WHERE id_zamowienia=:_id",
          new MapSqlParameterSource(parameters));
    } catch (DataIntegrityViolationException error) {
      throw OrderItemConstraintErrors.translate(error);
    }
    return get(id);
  }

  @Transactional
  public void delete(Integer id) {
    if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
    jdbc.update(
        "DELETE FROM Zamowienie WHERE id_zamowienia=:id", new MapSqlParameterSource("id", id));
  }
}
