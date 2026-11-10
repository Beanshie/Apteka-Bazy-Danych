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
public class KategoriaService {

    private final NamedParameterJdbcTemplate jdbc;

    public KategoriaService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Kategoria get(Integer id) {
        Kategoria record;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record =
                    jdbc.queryForObject(
                            "SELECT id_kategorii,nazwa_kategorii,opis FROM Kategoria WHERE id_kategorii=:id",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
                            new BeanPropertyRowMapper<>(Kategoria.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        return record;
    }

    public List<Kategoria> list() {
        StringBuilder sql =
                new StringBuilder("SELECT id_kategorii,nazwa_kategorii,opis FROM Kategoria WHERE 1=1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        sql.append(" ORDER BY id_kategorii");
        var result =
                jdbc.query(sql.toString(), parameters, new BeanPropertyRowMapper<>(Kategoria.class));
        return result;
    }

    @Transactional
    public Kategoria create(KategoriaCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getNazwaKategorii() == null)
            throw new IllegalArgumentException("Pole nie może być null: nazwa_kategorii");
        if (value.getNazwaKategorii() != null) {
            if (value.getNazwaKategorii().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getNazwaKategorii().length() > 50)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("nazwa_kategorii", value.getNazwaKategorii());
        parameters.put("opis", value.getOpis());
        var key = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO Kategoria (nazwa_kategorii,opis) VALUES (:nazwa_kategorii,:opis)",
                new MapSqlParameterSource(parameters),
                key,
                new String[]{"id_kategorii"});
        int id = Objects.requireNonNull(key.getKey()).intValue();
        return get(id);
    }

    @Transactional
    public Kategoria update(Integer id, KategoriaUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            jdbc.queryForObject(
                    "SELECT id_kategorii,nazwa_kategorii,opis FROM Kategoria WHERE id_kategorii=:id"
                            + " FOR UPDATE",
                    new MapSqlParameterSource().addValue("id", id).getValues(),
                    new BeanPropertyRowMapper<>(Kategoria.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getNazwaKategorii() == null)
            throw new IllegalArgumentException("Pole nie może być null: nazwa_kategorii");
        if (value.getNazwaKategorii() != null) {
            if (value.getNazwaKategorii().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getNazwaKategorii().length() > 50)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("nazwa_kategorii", value.getNazwaKategorii());
        parameters.put("opis", value.getOpis());
        parameters.put("_id", id);
        jdbc.update(
                "UPDATE Kategoria SET nazwa_kategorii=:nazwa_kategorii,opis=:opis WHERE id_kategorii=:_id",
                new MapSqlParameterSource(parameters));
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update(
                "DELETE FROM Kategoria WHERE id_kategorii=:id", new MapSqlParameterSource("id", id));
    }
}
