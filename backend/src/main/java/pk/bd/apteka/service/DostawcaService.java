package pk.bd.apteka.service;

import java.util.*;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.bd.apteka.dto.*;

@Service
public class DostawcaService {

    private final NamedParameterJdbcTemplate jdbc;

    public DostawcaService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Dostawca get(Integer id) {
        Dostawca record;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record =
                    jdbc.queryForObject(
                            "SELECT id_dostawcy,nazwa_firmy,nip,telefon,email FROM Dostawca WHERE"
                                    + " id_dostawcy=:id",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
                            new BeanPropertyRowMapper<>(Dostawca.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        return record;
    }

    public List<Dostawca> list() {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT id_dostawcy,nazwa_firmy,nip,telefon,email FROM Dostawca WHERE 1=1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        sql.append(" ORDER BY id_dostawcy");
        var result =
                jdbc.query(sql.toString(), parameters, new BeanPropertyRowMapper<>(Dostawca.class));
        return result;
    }

    @Transactional
    public Dostawca create(DostawcaCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getNazwaFirmy() == null)
            throw new IllegalArgumentException("Pole nie może być null: nazwa_firmy");
        if (value.getNazwaFirmy() != null) {
            if (value.getNazwaFirmy().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getNazwaFirmy().length() > 100)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getNip() != null) {
            if (value.getNip().length() > 15) throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getTelefon() != null) {
            if (value.getTelefon().length() > 20) throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getEmail() != null) {
            if (value.getEmail().length() > 100) throw new IllegalArgumentException("Zbyt długie pole");
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("nazwa_firmy", value.getNazwaFirmy());
        parameters.put("nip", value.getNip());
        parameters.put("telefon", value.getTelefon());
        parameters.put("email", value.getEmail());
        var key = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO Dostawca (nazwa_firmy,nip,telefon,email) VALUES"
                        + " (:nazwa_firmy,:nip,:telefon,:email)",
                new MapSqlParameterSource(parameters),
                key,
                new String[]{"id_dostawcy"});
        int generatedId = Objects.requireNonNull(key.getKey()).intValue();
        int id = generatedId;
        return get(id);
    }

    @Transactional
    public Dostawca update(Integer id, DostawcaUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            jdbc.queryForObject(
                    "SELECT id_dostawcy,nazwa_firmy,nip,telefon,email FROM Dostawca WHERE id_dostawcy=:id"
                            + " FOR UPDATE",
                    new MapSqlParameterSource().addValue("id", id).getValues(),
                    new BeanPropertyRowMapper<>(Dostawca.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getNazwaFirmy() == null)
            throw new IllegalArgumentException("Pole nie może być null: nazwa_firmy");
        if (value.getNazwaFirmy() != null) {
            if (value.getNazwaFirmy().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getNazwaFirmy().length() > 100)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getNip() != null) {
            if (value.getNip().length() > 15) throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getTelefon() != null) {
            if (value.getTelefon().length() > 20) throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getEmail() != null) {
            if (value.getEmail().length() > 100) throw new IllegalArgumentException("Zbyt długie pole");
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("nazwa_firmy", value.getNazwaFirmy());
        parameters.put("nip", value.getNip());
        parameters.put("telefon", value.getTelefon());
        parameters.put("email", value.getEmail());
        parameters.put("_id", id);
        jdbc.update(
                "UPDATE Dostawca SET nazwa_firmy=:nazwa_firmy,nip=:nip,telefon=:telefon,email=:email WHERE"
                        + " id_dostawcy=:_id",
                new MapSqlParameterSource(parameters));
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update("DELETE FROM Dostawca WHERE id_dostawcy=:id", new MapSqlParameterSource("id", id));
    }
}
