package pk.bd.apteka.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.*;

import org.springframework.dao.EmptyResultDataAccessException;
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
        Platnosc record;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record =
                    jdbc.queryForObject(
                            "SELECT id_platnosci,id_zamowienia,metoda_platnosci,status_platnosci,data_platnosci"
                                    + " FROM Platnosc WHERE id_platnosci=:id",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
                            (rs, index) -> {
                                var mappedRecord = new Platnosc();
                                mappedRecord.setIdPlatnosci(rs.getObject("id_platnosci", Integer.class));
                                mappedRecord.setIdZamowienia(rs.getObject("id_zamowienia", Integer.class));
                                mappedRecord.setMetodaPlatnosci(rs.getString("metoda_platnosci"));
                                mappedRecord.setStatusPlatnosci(
                                        Platnosc.StatusPlatnosciEnum.fromValue(rs.getString("status_platnosci")));
                                mappedRecord.setDataPlatnosci(
                                        rs.getTimestamp("data_platnosci") == null
                                                ? null
                                                : rs.getTimestamp("data_platnosci")
                                                .toLocalDateTime()
                                                .atOffset(java.time.ZoneOffset.UTC));
                                return mappedRecord;
                            });
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        return record;
    }

    public List<Platnosc> list(Integer idZamowienia) {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT id_platnosci,id_zamowienia,metoda_platnosci,status_platnosci,data_platnosci"
                                + " FROM Platnosc WHERE 1=1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (idZamowienia != null) {
            sql.append(" AND id_zamowienia=:id_zamowienia");
            parameters.put("id_zamowienia", idZamowienia);
        }
        sql.append(" ORDER BY id_platnosci");
        var result =
                jdbc.query(
                        sql.toString(),
                        parameters,
                        (rs, index) -> {
                            var mappedRecord = new Platnosc();
                            mappedRecord.setIdPlatnosci(rs.getObject("id_platnosci", Integer.class));
                            mappedRecord.setIdZamowienia(rs.getObject("id_zamowienia", Integer.class));
                            mappedRecord.setMetodaPlatnosci(rs.getString("metoda_platnosci"));
                            mappedRecord.setStatusPlatnosci(
                                    Platnosc.StatusPlatnosciEnum.fromValue(rs.getString("status_platnosci")));
                            mappedRecord.setDataPlatnosci(
                                    rs.getTimestamp("data_platnosci") == null
                                            ? null
                                            : rs.getTimestamp("data_platnosci")
                                            .toLocalDateTime()
                                            .atOffset(java.time.ZoneOffset.UTC));
                            return mappedRecord;
                        });
        return result;
    }

    @Transactional
    public Platnosc create(PlatnoscCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getIdZamowienia() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_zamowienia");
        if (value.getIdZamowienia() != null) {
            BigDecimal d = new BigDecimal(value.getIdZamowienia().toString());
            if (d.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdZamowienia() != null) zamowienie.get(value.getIdZamowienia());
        if (value.getMetodaPlatnosci() == null)
            throw new IllegalArgumentException("Pole nie może być null: metoda_platnosci");
        if (value.getMetodaPlatnosci() != null) {
            if (value.getMetodaPlatnosci().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getMetodaPlatnosci().length() > 30)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_zamowienia", value.getIdZamowienia());
        parameters.put("metoda_platnosci", value.getMetodaPlatnosci());
        parameters.put(
                "status_platnosci",
                value.getStatusPlatnosci() == null
                        ? "OCZEKUJACA"
                        : (value.getStatusPlatnosci().getValue()));
        parameters.put(
                "data_platnosci",
                value.getDataPlatnosci() == null
                        ? null
                        : Timestamp.valueOf(
                        value.getDataPlatnosci().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()));
        var key = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO Platnosc (id_zamowienia,metoda_platnosci,status_platnosci,data_platnosci)"
                        + " VALUES (:id_zamowienia,:metoda_platnosci,:status_platnosci,:data_platnosci)",
                new MapSqlParameterSource(parameters),
                key,
                new String[]{"id_platnosci"});
        int id = Objects.requireNonNull(key.getKey()).intValue();
        return get(id);
    }

    @Transactional
    public Platnosc update(Integer id, PlatnoscUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            jdbc.queryForObject(
                    "SELECT id_platnosci,id_zamowienia,metoda_platnosci,status_platnosci,data_platnosci FROM"
                            + " Platnosc WHERE id_platnosci=:id"
                            + " FOR UPDATE",
                    new MapSqlParameterSource().addValue("id", id).getValues(),
                    (rs, index) -> {
                        var mappedRecord = new Platnosc();
                        mappedRecord.setIdPlatnosci(rs.getObject("id_platnosci", Integer.class));
                        mappedRecord.setIdZamowienia(rs.getObject("id_zamowienia", Integer.class));
                        mappedRecord.setMetodaPlatnosci(rs.getString("metoda_platnosci"));
                        mappedRecord.setStatusPlatnosci(
                                Platnosc.StatusPlatnosciEnum.fromValue(rs.getString("status_platnosci")));
                        mappedRecord.setDataPlatnosci(
                                rs.getTimestamp("data_platnosci") == null
                                        ? null
                                        : rs.getTimestamp("data_platnosci")
                                        .toLocalDateTime()
                                        .atOffset(java.time.ZoneOffset.UTC));
                        return mappedRecord;
                    });
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getIdZamowienia() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_zamowienia");
        if (value.getIdZamowienia() != null) {
            BigDecimal d = new BigDecimal(value.getIdZamowienia().toString());
            if (d.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdZamowienia() != null) zamowienie.get(value.getIdZamowienia());
        if (value.getMetodaPlatnosci() == null)
            throw new IllegalArgumentException("Pole nie może być null: metoda_platnosci");
        if (value.getMetodaPlatnosci() != null) {
            if (value.getMetodaPlatnosci().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getMetodaPlatnosci().length() > 30)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getStatusPlatnosci() == null)
            throw new IllegalArgumentException("Pole nie może być null: status_platnosci");
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
        jdbc.update(
                "UPDATE Platnosc SET"
                        + " id_zamowienia=:id_zamowienia,metoda_platnosci=:metoda_platnosci,status_platnosci=:status_platnosci,data_platnosci=:data_platnosci"
                        + " WHERE id_platnosci=:_id",
                new MapSqlParameterSource(parameters));
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update("DELETE FROM Platnosc WHERE id_platnosci=:id", new MapSqlParameterSource("id", id));
    }
}
