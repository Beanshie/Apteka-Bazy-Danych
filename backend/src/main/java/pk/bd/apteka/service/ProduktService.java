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
        Produkt record;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record =
                    jdbc.queryForObject(
                            "SELECT"
                                    + " id_produktu,id_kategorii,id_dostawcy,nazwa_produktu,cena,stan_magazynowy,wymaga_recepty"
                                    + " FROM Produkt WHERE id_produktu=:id",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
                            new BeanPropertyRowMapper<>(Produkt.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        return record;
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
        var result = jdbc.query(sql.toString(), parameters, new BeanPropertyRowMapper<>(Produkt.class));
        return result;
    }

    @Transactional
    public Produkt create(ProduktCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getIdKategorii() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_kategorii");
        if (value.getIdKategorii() != null) {
            BigDecimal d = new BigDecimal(value.getIdKategorii().toString());
            if (d.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdKategorii() != null) kategoria.get(value.getIdKategorii());
        if (value.getIdDostawcy() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_dostawcy");
        if (value.getIdDostawcy() != null) {
            BigDecimal d2 = new BigDecimal(value.getIdDostawcy().toString());
            if (d2.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdDostawcy() != null) dostawca.get(value.getIdDostawcy());
        if (value.getNazwaProduktu() == null)
            throw new IllegalArgumentException("Pole nie może być null: nazwa_produktu");
        if (value.getNazwaProduktu() != null) {
            if (value.getNazwaProduktu().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getNazwaProduktu().length() > 100)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getCena() == null) throw new IllegalArgumentException("Pole nie może być null: cena");
        if (value.getCena() != null) {
            BigDecimal d3 = new BigDecimal(value.getCena().toString());
            if (d3.compareTo(BigDecimal.valueOf(0)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
            if ((d3.stripTrailingZeros().scale() > 2 || d3.compareTo(new BigDecimal("99999999.99")) > 0))
                throw new IllegalArgumentException("Niepoprawna cena");
        }
        if (value.getStanMagazynowy() != null) {
            BigDecimal d4 = new BigDecimal(value.getStanMagazynowy().toString());
            if (d4.compareTo(BigDecimal.valueOf(0)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_kategorii", value.getIdKategorii());
        parameters.put("id_dostawcy", value.getIdDostawcy());
        parameters.put("nazwa_produktu", value.getNazwaProduktu());
        parameters.put("cena", value.getCena());
        parameters.put(
                "stan_magazynowy", value.getStanMagazynowy() == null ? 0 : (value.getStanMagazynowy()));
        parameters.put(
                "wymaga_recepty", value.getWymagaRecepty() != null && (value.getWymagaRecepty()));
        var key = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO Produkt"
                        + " (id_kategorii,id_dostawcy,nazwa_produktu,cena,stan_magazynowy,wymaga_recepty)"
                        + " VALUES"
                        + " (:id_kategorii,:id_dostawcy,:nazwa_produktu,:cena,:stan_magazynowy,:wymaga_recepty)",
                new MapSqlParameterSource(parameters),
                key,
                new String[]{"id_produktu"});
        int id = Objects.requireNonNull(key.getKey()).intValue();
        return get(id);
    }

    @Transactional
    public Produkt update(Integer id, ProduktUpdate value) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            jdbc.queryForObject(
                    "SELECT"
                            + " id_produktu,id_kategorii,id_dostawcy,nazwa_produktu,cena,stan_magazynowy,wymaga_recepty"
                            + " FROM Produkt WHERE id_produktu=:id FOR UPDATE",
                    new MapSqlParameterSource().addValue("id", id).getValues(),
                    new BeanPropertyRowMapper<>(Produkt.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getIdKategorii() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_kategorii");
        if (value.getIdKategorii() != null) {
            BigDecimal d = new BigDecimal(value.getIdKategorii().toString());
            if (d.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdKategorii() != null) kategoria.get(value.getIdKategorii());
        if (value.getIdDostawcy() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_dostawcy");
        if (value.getIdDostawcy() != null) {
            BigDecimal d2 = new BigDecimal(value.getIdDostawcy().toString());
            if (d2.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdDostawcy() != null) dostawca.get(value.getIdDostawcy());
        if (value.getNazwaProduktu() == null)
            throw new IllegalArgumentException("Pole nie może być null: nazwa_produktu");
        if (value.getNazwaProduktu() != null) {
            if (value.getNazwaProduktu().isBlank()) throw new IllegalArgumentException("Puste pole");
            if (value.getNazwaProduktu().length() > 100)
                throw new IllegalArgumentException("Zbyt długie pole");
        }
        if (value.getCena() == null) throw new IllegalArgumentException("Pole nie może być null: cena");
        if (value.getCena() != null) {
            BigDecimal d3 = new BigDecimal(value.getCena().toString());
            if (d3.compareTo(BigDecimal.valueOf(0)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
            if ((d3.stripTrailingZeros().scale() > 2 || d3.compareTo(new BigDecimal("99999999.99")) > 0))
                throw new IllegalArgumentException("Niepoprawna cena");
        }
        if (value.getStanMagazynowy() == null)
            throw new IllegalArgumentException("Pole nie może być null: stan_magazynowy");
        if (value.getStanMagazynowy() != null) {
            BigDecimal d4 = new BigDecimal(value.getStanMagazynowy().toString());
            if (d4.compareTo(BigDecimal.valueOf(0)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getWymagaRecepty() == null)
            throw new IllegalArgumentException("Pole nie może być null: wymaga_recepty");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_kategorii", value.getIdKategorii());
        parameters.put("id_dostawcy", value.getIdDostawcy());
        parameters.put("nazwa_produktu", value.getNazwaProduktu());
        parameters.put("cena", value.getCena());
        parameters.put("stan_magazynowy", value.getStanMagazynowy());
        parameters.put("wymaga_recepty", value.getWymagaRecepty());
        parameters.put("_id", id);
        jdbc.update(
                "UPDATE Produkt SET"
                        + " id_kategorii=:id_kategorii,id_dostawcy=:id_dostawcy,nazwa_produktu=:nazwa_produktu,cena=:cena,stan_magazynowy=:stan_magazynowy,wymaga_recepty=:wymaga_recepty"
                        + " WHERE id_produktu=:_id",
                new MapSqlParameterSource(parameters));
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        jdbc.update("DELETE FROM Produkt WHERE id_produktu=:id", new MapSqlParameterSource("id", id));
    }
}
