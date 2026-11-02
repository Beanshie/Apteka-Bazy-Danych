package pk.bd.apteka.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import pk.bd.apteka.dto.DaneLek;
import pk.bd.apteka.dto.Lek;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class LekiService {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    public Lek addLek(DaneLek daneLek) {
        if (daneLek == null || daneLek.getNazwa() == null || daneLek.getNazwa().isBlank()) {
            throw new IllegalArgumentException("Nazwa leku nie może być pusta.");
        }
        if (daneLek.getCena() == null || daneLek.getCena() <= 0) {
            throw new IllegalArgumentException("Cena leku musi być większa od zera.");
        }
        Lek lek = new Lek(daneLek.getNazwa(), daneLek.getCena(), daneLek.getOpis(), null);
        String sql = "INSERT INTO Lek (nazwa, cena, opis, wymagana_Recepta, dostepne) " +
                "VALUES (:nazwa, :cena, :opis, :wymaganaRecepta, :dostepne)";

        SqlParameterSource params = new BeanPropertySqlParameterSource(lek);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            int rowsAffected = jdbc.update(sql, params, keyHolder, new String[]{"lek_ID"});

            if (rowsAffected > 0 && keyHolder.getKey() != null) {
                lek.setLekId(keyHolder.getKey().intValue());
                return lek;
            }
            throw new RuntimeException("Nie udało się utworzyć leku w bazie danych.");
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Lek o nazwie '" + daneLek.getNazwa() + "' już istnieje w bazie danych.", e);
        }
    }

    public Lek getLekById(Integer lekID) {
        String sql = "SELECT * FROM Lek WHERE lek_ID = :lekID";
        SqlParameterSource params = new MapSqlParameterSource("lekID", lekID);
        try {
            return jdbc.queryForObject(sql, params, new BeanPropertyRowMapper<>(Lek.class));
        } catch (EmptyResultDataAccessException e) {
            throw new NoSuchElementException("Nie znaleziono leku w bazie danych");
        }
    }

    public List<Lek> getLeki() {
        String sql = "SELECT * FROM Lek";
        List<Lek> list = jdbc.query(sql, new BeanPropertyRowMapper<>(Lek.class));
        return list;
    }

    public Lek updateLek(Integer lekID, DaneLek daneLek) {
        if (lekID == null || lekID <= 0) {
            throw new IllegalArgumentException("Niepoprawne ID leku.");
        }
        if (daneLek == null || daneLek.getNazwa() == null || daneLek.getNazwa().isBlank()) {
            throw new IllegalArgumentException("Nazwa leku nie może być pusta.");
        }
        if (daneLek.getCena() == null || daneLek.getCena() <= 0) {
            throw new IllegalArgumentException("Cena leku musi być większa od zera.");
        }

        Lek lek = new Lek(daneLek.getNazwa(), daneLek.getCena(), daneLek.getOpis(), null);

        lek.setLekId(lekID);

        String sql = "UPDATE Lek SET " +
                "nazwa = :nazwa, " +
                "cena = :cena, " +
                "opis = :opis, " +
                "wymagana_Recepta = :wymaganaRecepta, " +
                "dostepne = :dostepne " +
                "WHERE lek_ID = :lekID";

        SqlParameterSource params = new BeanPropertySqlParameterSource(lek);

        try {
            int rowsAffected = jdbc.update(sql, params);

            if (rowsAffected == 0) {
                throw new NoSuchElementException("Lek o ID " + lekID + " nie istnieje w bazie danych.");
            }

            return lek;
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Lek o nazwie '" + daneLek.getNazwa() + "' już istnieje.", e);
        }
    }
}
