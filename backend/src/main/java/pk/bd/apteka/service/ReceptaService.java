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
public class ReceptaService {

    private final NamedParameterJdbcTemplate jdbc;

    private final KlientService klient;

    public ReceptaService(NamedParameterJdbcTemplate jdbc, KlientService klient) {
        this.jdbc = jdbc;
        this.klient = klient;
    }

    public Recepta get(Integer id) {
        Recepta record;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record =
                    jdbc.queryForObject(
                            "SELECT id_recepty,id_klienta,kod_recepty,pesel_pacjenta,status_recepty FROM Recepta"
                                    + " WHERE id_recepty=:id",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
                            new BeanPropertyRowMapper<>(Recepta.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        return record;
    }

    public List<Recepta> list(Integer idKlienta, String kodRecepty, String statusRecepty) {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT id_recepty,id_klienta,kod_recepty,pesel_pacjenta,status_recepty FROM Recepta"
                                + " WHERE 1=1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (idKlienta != null) {
            sql.append(" AND id_klienta=:id_klienta");
            parameters.put("id_klienta", idKlienta);
        }
        if (kodRecepty != null) {
            sql.append(" AND kod_recepty=:kod_recepty");
            parameters.put("kod_recepty", kodRecepty);
        }
        if (statusRecepty != null) {
            sql.append(" AND status_recepty=:status_recepty");
            parameters.put("status_recepty", statusRecepty);
        }
        sql.append(" ORDER BY id_recepty");
        var result = jdbc.query(sql.toString(), parameters, new BeanPropertyRowMapper<>(Recepta.class));
        return result;
    }

    @Transactional
    public Recepta create(ReceptaCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getIdKlienta() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_klienta");
        if (value.getIdKlienta() != null) {
            BigDecimal d = new BigDecimal(value.getIdKlienta().toString());
            if (d.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdKlienta() != null) klient.get(value.getIdKlienta());
        if (value.getKodRecepty() == null)
            throw new IllegalArgumentException("Pole nie może być null: kod_recepty");
        if (value.getKodRecepty() != null) {
            if (value.getKodRecepty().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getKodRecepty().length() > 10)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getPeselPacjenta() == null)
            throw new IllegalArgumentException("Pole nie może być null: pesel_pacjenta");
        if (value.getPeselPacjenta() != null) {
            if (value.getPeselPacjenta().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getPeselPacjenta().length() > 11)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_klienta", value.getIdKlienta());
        parameters.put("kod_recepty", value.getKodRecepty());
        parameters.put("pesel_pacjenta", value.getPeselPacjenta());
        parameters.put(
                "status_recepty",
                value.getStatusRecepty() == null ? "NOWA" : (value.getStatusRecepty().getValue()));
        var key = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO Recepta (id_klienta,kod_recepty,pesel_pacjenta,status_recepty) VALUES"
                        + " (:id_klienta,:kod_recepty,:pesel_pacjenta,:status_recepty)",
                new MapSqlParameterSource(parameters),
                key,
                new String[]{"id_recepty"});
        int id = Objects.requireNonNull(key.getKey()).intValue();
        return get(id);
    }

    @Transactional
    public Recepta update(Integer id, ReceptaUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            jdbc.queryForObject(
                    "SELECT id_recepty,id_klienta,kod_recepty,pesel_pacjenta,status_recepty FROM Recepta"
                            + " WHERE id_recepty=:id"
                            + " FOR UPDATE",
                    new MapSqlParameterSource().addValue("id", id).getValues(),
                    new BeanPropertyRowMapper<>(Recepta.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getIdKlienta() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_klienta");
        if (value.getIdKlienta() != null) {
            BigDecimal d = new BigDecimal(value.getIdKlienta().toString());
            if (d.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdKlienta() != null) klient.get(value.getIdKlienta());
        if (value.getKodRecepty() == null)
            throw new IllegalArgumentException("Pole nie może być null: kod_recepty");
        if (value.getKodRecepty() != null) {
            if (value.getKodRecepty().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getKodRecepty().length() > 10)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getPeselPacjenta() == null)
            throw new IllegalArgumentException("Pole nie może być null: pesel_pacjenta");
        if (value.getPeselPacjenta() != null) {
            if (value.getPeselPacjenta().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getPeselPacjenta().length() > 11)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getStatusRecepty() == null)
            throw new IllegalArgumentException("Pole nie może być null: status_recepty");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_klienta", value.getIdKlienta());
        parameters.put("kod_recepty", value.getKodRecepty());
        parameters.put("pesel_pacjenta", value.getPeselPacjenta());
        parameters.put(
                "status_recepty",
                value.getStatusRecepty() == null ? null : value.getStatusRecepty().getValue());
        parameters.put("_id", id);
        jdbc.update(
                "UPDATE Recepta SET"
                        + " id_klienta=:id_klienta,kod_recepty=:kod_recepty,pesel_pacjenta=:pesel_pacjenta,status_recepty=:status_recepty"
                        + " WHERE id_recepty=:_id",
                new MapSqlParameterSource(parameters));
        if (jdbc.queryForObject(
                "SELECT COUNT(*) FROM Szczegol_Zamowienia s JOIN Zamowienie z ON"
                        + " z.id_zamowienia=s.id_zamowienia JOIN Recepta r ON r.id_recepty=s.id_recepty"
                        + " WHERE r.id_recepty=:id AND r.id_klienta<>z.id_klienta",
                new MapSqlParameterSource().addValue("id", id).getValues(),
                Integer.class)
                > 0) throw new IllegalArgumentException("Recepta należy do innego klienta");
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update("DELETE FROM Recepta WHERE id_recepty=:id", new MapSqlParameterSource("id", id));
    }
}
