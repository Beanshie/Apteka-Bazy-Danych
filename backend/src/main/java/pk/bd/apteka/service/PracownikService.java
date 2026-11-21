package pk.bd.apteka.service;

import java.util.*;

import org.springframework.dao.DataIntegrityViolationException;
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
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            return jdbc.queryForObject(
                    "SELECT id_pracownika,login,imie,nazwisko,rola,aktywny FROM Pracownik WHERE"
                            + " id_pracownika=:id",
                    new MapSqlParameterSource().addValue("id", id).getValues(),
                    new BeanPropertyRowMapper<>(Pracownik.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
    }

    public List<Pracownik> list() {
        return jdbc.query("SELECT id_pracownika,login,imie,nazwisko,rola,aktywny FROM Pracownik"
                + " ORDER BY id_pracownika", new BeanPropertyRowMapper<>(Pracownik.class));
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
        parameters.put("rola", value.getRola() == null ? "FARMACEUTA" : value.getRola().getValue());
        parameters.put("aktywny", value.getAktywny() == null || value.getAktywny());
        var key = new GeneratedKeyHolder();
        try {
            jdbc.update(
                    "INSERT INTO Pracownik (login,haslo,imie,nazwisko,rola,aktywny) VALUES"
                            + " (:login,:haslo,:imie,:nazwisko,:rola,:aktywny)",
                    new MapSqlParameterSource(parameters),
                    key,
                    new String[]{"id_pracownika"});
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Błąd integralności danych", e);
        }
        int id = Objects.requireNonNull(key.getKey()).intValue();
        return get(id);
    }

    @Transactional
    public Pracownik update(Integer id, PracownikUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("login", value.getLogin());
        String passwordHash = value.getHaslo() == null
                ? null
                : new BCryptPasswordEncoder().encode(value.getHaslo());
        parameters.put("haslo", passwordHash);
        parameters.put("imie", value.getImie());
        parameters.put("nazwisko", value.getNazwisko());
        parameters.put("rola", value.getRola() == null ? null : value.getRola().getValue());
        parameters.put("aktywny", value.getAktywny());
        parameters.put("_id", id);
        int rows = jdbc.update(
                "UPDATE Pracownik SET"
                        + " login=:login,haslo=COALESCE(:haslo,haslo),imie=:imie,nazwisko=:nazwisko,rola=:rola,aktywny=:aktywny"
                        + " WHERE id_pracownika=:_id",
                new MapSqlParameterSource(parameters));
        if (rows == 0) throw new NoSuchElementException("Nie znaleziono rekordu");
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update(
                "DELETE FROM Pracownik WHERE id_pracownika=:id", new MapSqlParameterSource("id", id));
    }
}
