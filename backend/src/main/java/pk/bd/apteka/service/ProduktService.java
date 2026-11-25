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
public class ProduktService {

    private final NamedParameterJdbcTemplate jdbc;

    private final DostawcaService dostawca;

    private final KategoriaService kategoria;

    public ProduktService(
            NamedParameterJdbcTemplate jdbc, DostawcaService dostawca, KategoriaService kategoria) {
        this.jdbc = jdbc;
        this.dostawca = dostawca;
        this.kategoria = kategoria;
    }

    public Produkt get(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            return jdbc.queryForObject(
                    "SELECT"
                            + " id_produktu,id_kategorii,id_dostawcy,nazwa_produktu,cena,stan_magazynowy,wymaga_recepty"
                            + " FROM Produkt WHERE id_produktu=:id",
                    new MapSqlParameterSource().addValue("id", id).getValues(),
                    new BeanPropertyRowMapper<>(Produkt.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
    }

    public List<Produkt> list(Integer idKategorii, Integer idDostawcy) {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT"
                                + " id_produktu,id_kategorii,id_dostawcy,nazwa_produktu,cena,stan_magazynowy,wymaga_recepty"
                                + " FROM Produkt WHERE 1=1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (idKategorii != null) {
            sql.append(" AND id_kategorii=:id_kategorii");
            parameters.put("id_kategorii", idKategorii);
        }
        if (idDostawcy != null) {
            sql.append(" AND id_dostawcy=:id_dostawcy");
            parameters.put("id_dostawcy", idDostawcy);
        }
        sql.append(" ORDER BY id_produktu");
        return jdbc.query(sql.toString(), parameters, new BeanPropertyRowMapper<>(Produkt.class));
    }

    @Transactional
    public Produkt create(ProduktCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_kategorii", value.getIdKategorii());
        parameters.put("id_dostawcy", value.getIdDostawcy());
        parameters.put("nazwa_produktu", value.getNazwaProduktu());
        parameters.put("cena", value.getCena());
        parameters.put(
                "stan_magazynowy", value.getStanMagazynowy() == null ? 0 : value.getStanMagazynowy());
        parameters.put(
                "wymaga_recepty", value.getWymagaRecepty() != null && value.getWymagaRecepty());
        var key = new GeneratedKeyHolder();
        try {
            jdbc.update(
                    "INSERT INTO Produkt"
                            + " (id_kategorii,id_dostawcy,nazwa_produktu,cena,stan_magazynowy,wymaga_recepty)"
                            + " VALUES"
                            + " (:id_kategorii,:id_dostawcy,:nazwa_produktu,:cena,:stan_magazynowy,:wymaga_recepty)",
                    new MapSqlParameterSource(parameters),
                    key,
                    new String[]{"id_produktu"});
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Błąd integralności danych", e);
        }
        int id = Objects.requireNonNull(key.getKey()).intValue();
        return get(id);
    }

    @Transactional
    public Produkt update(Integer id, ProduktUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        kategoria.get(value.getIdKategorii());
        dostawca.get(value.getIdDostawcy());
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_kategorii", value.getIdKategorii());
        parameters.put("id_dostawcy", value.getIdDostawcy());
        parameters.put("nazwa_produktu", value.getNazwaProduktu());
        parameters.put("cena", value.getCena());
        parameters.put("stan_magazynowy", value.getStanMagazynowy());
        parameters.put("wymaga_recepty", value.getWymagaRecepty());
        parameters.put("_id", id);
        int rows = jdbc.update(
                "UPDATE Produkt SET"
                        + " id_kategorii=:id_kategorii,id_dostawcy=:id_dostawcy,nazwa_produktu=:nazwa_produktu,"
                        + "cena=:cena,stan_magazynowy=:stan_magazynowy,wymaga_recepty=:wymaga_recepty"
                        + " WHERE id_produktu=:_id",
                new MapSqlParameterSource(parameters));
        if (rows == 0) throw new NoSuchElementException("Nie znaleziono rekordu");
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update("DELETE FROM Produkt WHERE id_produktu=:id", new MapSqlParameterSource("id", id));
    }
}
