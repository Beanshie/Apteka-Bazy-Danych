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
public class AdresKlientaService {

    private final NamedParameterJdbcTemplate jdbc;

    public AdresKlientaService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdresKlienta get(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            return jdbc.queryForObject(
                    "SELECT id_adresu,id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto"
                            + " FROM Adres_Klienta WHERE id_adresu=:id",
                    new MapSqlParameterSource().addValue("id", id).getValues(),
                    new BeanPropertyRowMapper<>(AdresKlienta.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
    }

    public List<AdresKlienta> list(Integer idKlienta) {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT id_adresu,id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto FROM"
                                + " Adres_Klienta ");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (idKlienta != null) {
            sql.append(" WHERE id_klienta=:id_klienta");
            parameters.put("id_klienta", idKlienta);
        }
        sql.append(" ORDER BY id_adresu");
        return jdbc.query(sql.toString(), parameters, new BeanPropertyRowMapper<>(AdresKlienta.class));
    }

    @Transactional
    public AdresKlienta create(AdresKlientaCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_klienta", value.getIdKlienta());
        parameters.put("ulica", value.getUlica());
        parameters.put("numer_domu", value.getNumerDomu());
        parameters.put("numer_mieszkania", value.getNumerMieszkania());
        parameters.put("kod_pocztowy", value.getKodPocztowy());
        parameters.put("miasto", value.getMiasto());
        var key = new GeneratedKeyHolder();
        try {
            jdbc.update(
                    "INSERT INTO Adres_Klienta"
                            + " (id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto) VALUES"
                            + " (:id_klienta,:ulica,:numer_domu,:numer_mieszkania,:kod_pocztowy,:miasto)",
                    new MapSqlParameterSource(parameters),
                    key,
                    new String[]{"id_adresu"});
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Błąd integralności danych", e);
        }
        int id = Objects.requireNonNull(key.getKey()).intValue();
        return get(id);
    }

    @Transactional
    public AdresKlienta update(Integer id, AdresKlientaUpdate value) {
        if (id == null || id < 1)
            throw new IllegalArgumentException("Niepoprawne ID");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_klienta", value.getIdKlienta());
        parameters.put("ulica", value.getUlica());
        parameters.put("numer_domu", value.getNumerDomu());
        parameters.put("numer_mieszkania", value.getNumerMieszkania());
        parameters.put("kod_pocztowy", value.getKodPocztowy());
        parameters.put("miasto", value.getMiasto());
        parameters.put("_id", id);

        int rows= jdbc.update(
                "UPDATE Adres_Klienta SET"
                        + " ulica=:ulica,numer_domu=:numer_domu,"
                        + "numer_mieszkania=:numer_mieszkania,kod_pocztowy=:kod_pocztowy,"
                        + "miasto=:miasto WHERE id_adresu=:_id  AND id_klienta=:id_klienta",
                new MapSqlParameterSource(parameters));
        if(rows==0)
            throw new NoSuchElementException("Nie znaleziono rekordu");
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update(
                "DELETE FROM Adres_Klienta WHERE id_adresu=:id", new MapSqlParameterSource("id", id));
    }
}
