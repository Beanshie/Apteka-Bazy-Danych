package pk.bd.apteka.service;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.*;

import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.bd.apteka.dto.*;

@Service
public class PlatnoscService {

    private final NamedParameterJdbcTemplate jdbc;

    private final ZamowienieService zamowienie;

    public PlatnoscService(NamedParameterJdbcTemplate jdbc, ZamowienieService zamowienie) {
        this.jdbc = jdbc;
        this.zamowienie = zamowienie;
    }

    public Platnosc get(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            return jdbc.queryForObject(
                    "SELECT id_platnosci,id_zamowienia,metoda_platnosci,status_platnosci,data_platnosci"
                            + " FROM Platnosc WHERE id_platnosci=:id",
                    new MapSqlParameterSource().addValue("id", id).getValues(),
                    new BeanPropertyRowMapper<>(Platnosc.class) {
                        @Override
                        protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor property)
                                throws SQLException {
                            if ("dataPlatnosci".equals(property.getName())) {
                                var timestamp = rs.getTimestamp(index);
                                return timestamp == null ? null : timestamp.toLocalDateTime().atOffset(ZoneOffset.UTC);
                            }
                            return super.getColumnValue(rs, index, property);
                        }
                    });
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
    }

    public List<Platnosc> list(Integer idZamowienia) {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT id_platnosci,id_zamowienia,metoda_platnosci,status_platnosci,data_platnosci"
                                + " FROM Platnosc");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (idZamowienia != null) {
            sql.append(" WHERE id_zamowienia=:id_zamowienia");
            parameters.put("id_zamowienia", idZamowienia);
        }
        sql.append(" ORDER BY id_platnosci");
        return jdbc.query(
                sql.toString(),
                parameters,
                new BeanPropertyRowMapper<>(Platnosc.class) {
                    @Override
                    protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor property)
                            throws SQLException {
                        if ("dataPlatnosci".equals(property.getName())) {
                            var timestamp = rs.getTimestamp(index);
                            return timestamp == null ? null : timestamp.toLocalDateTime().atOffset(ZoneOffset.UTC);
                        }
                        return super.getColumnValue(rs, index, property);
                    }
                });
    }

    @Transactional
    public Platnosc create(PlatnoscCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_zamowienia", value.getIdZamowienia());
        parameters.put("metoda_platnosci", value.getMetodaPlatnosci());
        parameters.put(
                "status_platnosci",
                value.getStatusPlatnosci() == null
                        ? "OCZEKUJACA"
                        : value.getStatusPlatnosci().getValue());
        parameters.put(
                "data_platnosci",
                value.getDataPlatnosci() == null
                        ? null
                        : Timestamp.valueOf(
                        value.getDataPlatnosci().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()));
        var key = new GeneratedKeyHolder();
        try {
            jdbc.update(
                    "INSERT INTO Platnosc (id_zamowienia,metoda_platnosci,status_platnosci,data_platnosci)"
                            + " VALUES (:id_zamowienia,:metoda_platnosci,:status_platnosci,:data_platnosci)",
                    new MapSqlParameterSource(parameters),
                    key,
                    new String[]{"id_platnosci"});
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Błąd integralności danych", e);
        }
        int id = Objects.requireNonNull(key.getKey()).intValue();
        return get(id);
    }

    @Transactional
    public Platnosc update(Integer id, PlatnoscUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_zamowienia", value.getIdZamowienia());
        parameters.put("metoda_platnosci", value.getMetodaPlatnosci());
        parameters.put(
                "status_platnosci",
                value.getStatusPlatnosci() == null ? null : value.getStatusPlatnosci().getValue());
        parameters.put(
                "data_platnosci",
                value.getDataPlatnosci() == null
                        ? null
                        : Timestamp.valueOf(
                        value.getDataPlatnosci().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()));

        parameters.put("_id", id);
        int rows = jdbc.update(
                "UPDATE Platnosc SET"
                        + " id_zamowienia=:id_zamowienia,metoda_platnosci=:metoda_platnosci,status_platnosci=:status_platnosci,data_platnosci=:data_platnosci"
                        + " WHERE id_platnosci=:_id",
                new MapSqlParameterSource(parameters));
        if (rows == 0) throw new NoSuchElementException("Nie znaleziono rekordu");
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update("DELETE FROM Platnosc WHERE id_platnosci=:id", new MapSqlParameterSource("id", id));
    }

}
