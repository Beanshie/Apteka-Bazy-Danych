package pk.bd.apteka.service;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.*;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.bd.apteka.dto.*;

@Service
public class KlientService {
    private final NamedParameterJdbcTemplate jdbc;

    public KlientService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Klient create(KlientCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        String sql =
                "INSERT INTO Klient (imie,nazwisko,email,haslo,telefon) VALUES"
                        + " (:imie,:nazwisko,:email,PASSWORD(:haslo),:telefon)";
        SqlParameterSource parameters = new BeanPropertySqlParameterSource(value);
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, parameters, keyHolder, new String[]{"id_klienta"});
        int id = Objects.requireNonNull(keyHolder.getKey()).intValue();
        return get(id);
    }

    public Klient get(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        SqlParameterSource params = new MapSqlParameterSource("idKlienta", id);
        try {
            return jdbc.queryForObject("SELECT * FROM Lek WHERE lek_ID = :lekID", params, new BeanPropertyRowMapper<>(Klient.class));
        } catch (EmptyResultDataAccessException e) {
            throw new NoSuchElementException("Nie znaleziono klienta w bazie danych");
        }
    }

    public List<Klient> list() {

        return jdbc.query("SELECT * FROM Klient ORDER BY id_klienta", new BeanPropertyRowMapper<>(Klient.class));
    }

    @Transactional
    public Klient update(Integer id, KlientUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        String sql =
                "UPDATE Klient SET"
                        + " imie=:imie,nazwisko=:nazwisko,email=:email,haslo=COALESCE(:haslo,haslo),telefon=:telefon"
                        + " WHERE id_klienta=:id";
        SqlParameterSource params = new BeanPropertySqlParameterSource(value);
        jdbc.update(sql, params);

            int rowsAffected = jdbc.update(sql, params);

            if (rowsAffected == 0) {
                throw new NoSuchElementException("Lek o ID " + id + " nie istnieje w bazie danych.");
            }

            return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        String sql = "DELETE FROM Klient WHERE id_klienta=:id";
        jdbc.update(sql, Map.of("id", id));
    }
}
