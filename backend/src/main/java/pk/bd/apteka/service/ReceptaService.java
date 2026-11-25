package pk.bd.apteka.service;

import java.util.*;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
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
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            return jdbc.queryForObject(
                    "SELECT id_recepty,id_klienta,kod_recepty,pesel_pacjenta,status_recepty FROM Recepta"
                            + " WHERE id_recepty=:id",
                    new MapSqlParameterSource().addValue("id", id).getValues(),
                    new BeanPropertyRowMapper<>(Recepta.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
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
        return jdbc.query(sql.toString(), parameters, new BeanPropertyRowMapper<>(Recepta.class));
    }

    @Transactional
    public Recepta create(ReceptaCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_klienta", value.getIdKlienta());
        parameters.put("kod_recepty", value.getKodRecepty());
        parameters.put("pesel_pacjenta", value.getPeselPacjenta());
        parameters.put(
                "status_recepty",
                value.getStatusRecepty() == null ? "NOWA" : value.getStatusRecepty().getValue());
        var key = new GeneratedKeyHolder();
        try {
            jdbc.update(
                    "INSERT INTO Recepta (id_klienta,kod_recepty,pesel_pacjenta,status_recepty) VALUES"
                            + " (:id_klienta,:kod_recepty,:pesel_pacjenta,:status_recepty)",
                    new MapSqlParameterSource(parameters),
                    key,
                    new String[]{"id_recepty"});
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Błąd integralności danych", e);
        }
        int id = Objects.requireNonNull(key.getKey()).intValue();
        return get(id);
    }

    @Transactional
    public Recepta update(Integer id, ReceptaUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_klienta", value.getIdKlienta());
        parameters.put("kod_recepty", value.getKodRecepty());
        parameters.put("pesel_pacjenta", value.getPeselPacjenta());
        parameters.put(
                "status_recepty",
                value.getStatusRecepty() == null ? null : value.getStatusRecepty().getValue());
        parameters.put("_id", id);
        int rows = jdbc.update(
                "UPDATE Recepta SET"
                        + " kod_recepty=:kod_recepty,pesel_pacjenta=:pesel_pacjenta,status_recepty=:status_recepty"
                        + " WHERE id_recepty=:_id AND id_klienta=:id_klienta",
                new MapSqlParameterSource(parameters));
        if (rows == 0) throw new NoSuchElementException("Nie znaleziono rekordu");
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update("DELETE FROM Recepta WHERE id_recepty=:id", new MapSqlParameterSource("id", id));
    }
}
