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
        final var sql = "SELECT id_dostawcy,nazwa_firmy,nip,telefon,email FROM Dostawca WHERE 1=1 ORDER BY id_dostawcy";
        return jdbc.query(sql, new BeanPropertyRowMapper<>(Dostawca.class));
    }

    @Transactional
    public Dostawca create(DostawcaCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("nazwa_firmy", value.getNazwaFirmy());
        parameters.put("nip", value.getNip());
        parameters.put("telefon", value.getTelefon());
        parameters.put("email", value.getEmail());
        var key = new GeneratedKeyHolder();
        try {
            jdbc.update(
                    "INSERT INTO Dostawca (nazwa_firmy,nip,telefon,email) VALUES"
                            + " (:nazwa_firmy,:nip,:telefon,:email)",
                    new MapSqlParameterSource(parameters),
                    key,
                    new String[]{"id_dostawcy"});
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Błąd integralności danych", e);
        }
        int id = Objects.requireNonNull(key.getKey()).intValue();
        return get(id);
    }

    @Transactional
    public Dostawca update(Integer id, DostawcaUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("nazwa_firmy", value.getNazwaFirmy());
        parameters.put("nip", value.getNip());
        parameters.put("telefon", value.getTelefon());
        parameters.put("email", value.getEmail());
        parameters.put("_id", id);
        int rows =jdbc.update(
                "UPDATE Dostawca SET nazwa_firmy=:nazwa_firmy,nip=:nip,telefon=:telefon,email=:email WHERE"
                        + " id_dostawcy=:_id",
                new MapSqlParameterSource(parameters));
        if(rows==0)
            throw new NoSuchElementException("Nie znaleziono rekordu");
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update("DELETE FROM Dostawca WHERE id_dostawcy=:id", new MapSqlParameterSource("id", id));
    }
}
