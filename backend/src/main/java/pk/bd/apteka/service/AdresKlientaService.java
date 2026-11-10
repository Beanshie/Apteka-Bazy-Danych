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
public class AdresKlientaService {

    private final NamedParameterJdbcTemplate jdbc;

    private final KlientService klient;

    public AdresKlientaService(NamedParameterJdbcTemplate jdbc, KlientService klient) {
        this.jdbc = jdbc;
        this.klient = klient;
    }

    public AdresKlienta get(Integer id) {
        AdresKlienta record;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record =
                    jdbc.queryForObject(
                            "SELECT id_adresu,id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto"
                                    + " FROM Adres_Klienta WHERE id_adresu=:id",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
                            new BeanPropertyRowMapper<>(AdresKlienta.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        return record;
    }

    public List<AdresKlienta> list(Integer idKlienta) {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT id_adresu,id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto FROM"
                                + " Adres_Klienta WHERE 1=1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (idKlienta != null) {
            sql.append(" AND id_klienta=:id_klienta");
            parameters.put("id_klienta", idKlienta);
        }
        sql.append(" ORDER BY id_adresu");
        var result =
                jdbc.query(sql.toString(), parameters, new BeanPropertyRowMapper<>(AdresKlienta.class));
        return result;
    }

    @Transactional
    public AdresKlienta create(AdresKlientaCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getIdKlienta() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_klienta");
        if (value.getIdKlienta() != null) {
            BigDecimal d = new BigDecimal(value.getIdKlienta().toString());
            if (d.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdKlienta() != null) klient.get(value.getIdKlienta());
        if (value.getUlica() == null)
            throw new IllegalArgumentException("Pole nie może być null: ulica");
        if (value.getUlica() != null) {
            if (value.getUlica().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getUlica().length() > 100) throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getNumerDomu() == null)
            throw new IllegalArgumentException("Pole nie może być null: numer_domu");
        if (value.getNumerDomu() != null) {
            if (value.getNumerDomu().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getNumerDomu().length() > 10)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getNumerMieszkania() != null) {
            if (value.getNumerMieszkania().length() > 10)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getKodPocztowy() == null)
            throw new IllegalArgumentException("Pole nie może być null: kod_pocztowy");
        if (value.getKodPocztowy() != null) {
            if (value.getKodPocztowy().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getKodPocztowy().length() > 10)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getMiasto() == null)
            throw new IllegalArgumentException("Pole nie może być null: miasto");
        if (value.getMiasto() != null) {
            if (value.getMiasto().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getMiasto().length() > 50) throw new IllegalArgumentException("Zbyt długie pole");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id_klienta", value.getIdKlienta());
        result.put("ulica", value.getUlica());
        result.put("numer_domu", value.getNumerDomu());
        result.put("numer_mieszkania", value.getNumerMieszkania());
        result.put("kod_pocztowy", value.getKodPocztowy());
        result.put("miasto", value.getMiasto());
        Map<String, Object> parameters = result;
        var key = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO Adres_Klienta"
                        + " (id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto) VALUES"
                        + " (:id_klienta,:ulica,:numer_domu,:numer_mieszkania,:kod_pocztowy,:miasto)",
                new MapSqlParameterSource(parameters),
                key,
                new String[]{"id_adresu"});
        int generatedId = Objects.requireNonNull(key.getKey()).intValue();
        int id = generatedId;
        return get(id);
    }

    @Transactional
    public AdresKlienta update(Integer id, AdresKlientaUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            jdbc.queryForObject(
                    "SELECT id_adresu,id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto FROM"
                            + " Adres_Klienta WHERE id_adresu=:id"
                            + " FOR UPDATE",
                    new MapSqlParameterSource().addValue("id", id).getValues(),
                    new BeanPropertyRowMapper<>(AdresKlienta.class));
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
        if (value.getUlica() == null)
            throw new IllegalArgumentException("Pole nie może być null: ulica");
        if (value.getUlica() != null) {
            if (value.getUlica().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getUlica().length() > 100) throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getNumerDomu() == null)
            throw new IllegalArgumentException("Pole nie może być null: numer_domu");
        if (value.getNumerDomu() != null) {
            if (value.getNumerDomu().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getNumerDomu().length() > 10)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getNumerMieszkania() != null) {
            if (value.getNumerMieszkania().length() > 10)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getKodPocztowy() == null)
            throw new IllegalArgumentException("Pole nie może być null: kod_pocztowy");
        if (value.getKodPocztowy() != null) {
            if (value.getKodPocztowy().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getKodPocztowy().length() > 10)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getMiasto() == null)
            throw new IllegalArgumentException("Pole nie może być null: miasto");
        if (value.getMiasto() != null) {
            if (value.getMiasto().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getMiasto().length() > 50) throw new IllegalArgumentException("Zbyt długie pole");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id_klienta", value.getIdKlienta());
        result.put("ulica", value.getUlica());
        result.put("numer_domu", value.getNumerDomu());
        result.put("numer_mieszkania", value.getNumerMieszkania());
        result.put("kod_pocztowy", value.getKodPocztowy());
        result.put("miasto", value.getMiasto());
        Map<String, Object> parameters = result;
        Map<String, Object> parameters2 = new LinkedHashMap<>(parameters);
        parameters2.put("_id", id);
        jdbc.update(
                "UPDATE Adres_Klienta SET"
                        + " id_klienta=:id_klienta,ulica=:ulica,numer_domu=:numer_domu,numer_mieszkania=:numer_mieszkania,kod_pocztowy=:kod_pocztowy,miasto=:miasto"
                        + " WHERE id_adresu=:_id",
                new MapSqlParameterSource(parameters2));
        if (jdbc.queryForObject(
                "SELECT COUNT(*) FROM Zamowienie z JOIN Adres_Klienta a ON a.id_adresu=z.id_adresu"
                        + " WHERE a.id_adresu=:id AND a.id_klienta<>z.id_klienta",
                new MapSqlParameterSource().addValue("id", id).getValues(),
                Integer.class)
                > 0) throw new IllegalArgumentException("Adres należy do innego klienta");
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update(
                "DELETE FROM Adres_Klienta WHERE id_adresu=:id", new MapSqlParameterSource("id", id));
    }
}
