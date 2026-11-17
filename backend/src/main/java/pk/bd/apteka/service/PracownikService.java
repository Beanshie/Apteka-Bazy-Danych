package pk.bd.apteka.service;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.bd.apteka.dto.*;

@Service
public class PracownikService {

    private final NamedParameterJdbcTemplate jdbc;

    public PracownikService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Pracownik get(Integer id) {
        Pracownik record;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record =
                    jdbc.queryForObject(
                            "SELECT id_pracownika,login,imie,nazwisko,rola,aktywny FROM Pracownik WHERE"
                                    + " id_pracownika=:id",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
                            new BeanPropertyRowMapper<>(Pracownik.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        return record;
    }

    public List<Pracownik> list() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        return jdbc.query("SELECT id_pracownika,login,imie,nazwisko,rola,aktywny FROM Pracownik"
                + " ORDER BY id_pracownika", parameters, new BeanPropertyRowMapper<>(Pracownik.class));
    }

    @Transactional
    public Pracownik create(PracownikCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("login", value.getLogin());
        String passwordHash = new BCryptPasswordEncoder().encode(value.getHaslo());
        parameters.put("haslo", passwordHash);
        parameters.put("imie", value.getImie());
        parameters.put("nazwisko", value.getNazwisko());
        parameters.put("rola", value.getRola() == null ? "FARMACEUTA" : (value.getRola().getValue()));
        parameters.put("aktywny", value.getAktywny() == null || (value.getAktywny()));
        var key = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO Pracownik (login,haslo,imie,nazwisko,rola,aktywny) VALUES"
                        + " (:login,:haslo,:imie,:nazwisko,:rola,:aktywny)",
                new MapSqlParameterSource(parameters),
                key,
                new String[]{"id_pracownika"});
        int id = Objects.requireNonNull(key.getKey()).intValue();
        return get(id);
    }

    @Transactional
    public Pracownik update(Integer id, PracownikUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            jdbc.queryForObject(
                    "SELECT id_pracownika,login,imie,nazwisko,rola,aktywny FROM Pracownik WHERE"
                            + " id_pracownika=:id",
                    new MapSqlParameterSource().addValue("id", id).getValues(),
                    new BeanPropertyRowMapper<>(Pracownik.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("login", value.getLogin());
        String passwordHash = null;
        passwordHash = new BCryptPasswordEncoder().encode(value.getHaslo());
        parameters.put("haslo", passwordHash);
        parameters.put("imie", value.getImie());
        parameters.put("nazwisko", value.getNazwisko());
        parameters.put("rola", value.getRola().getValue());
        parameters.put("aktywny", value.getAktywny());
        parameters.put("_id", id);
        jdbc.update(
                "UPDATE Pracownik SET"
                        + " login=:login,haslo=COALESCE(:haslo,haslo),imie=:imie,nazwisko=:nazwisko,rola=:rola,aktywny=:aktywny"
                        + " WHERE id_pracownika=:_id",
                new MapSqlParameterSource(parameters));
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update(
                "DELETE FROM Pracownik WHERE id_pracownika=:id", new MapSqlParameterSource("id", id));
    }
}
