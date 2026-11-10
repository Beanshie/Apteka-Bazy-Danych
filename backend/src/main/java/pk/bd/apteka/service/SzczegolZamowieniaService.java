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
public class SzczegolZamowieniaService {

    private final NamedParameterJdbcTemplate jdbc;

    private final ProduktService produkt;

    private final ReceptaService recepta;

    private final ZamowienieService zamowienie;

    public SzczegolZamowieniaService(
            NamedParameterJdbcTemplate jdbc,
            ProduktService produkt,
            ReceptaService recepta,
            ZamowienieService zamowienie) {
        this.jdbc = jdbc;
        this.produkt = produkt;
        this.recepta = recepta;
        this.zamowienie = zamowienie;
    }

    public SzczegolZamowienia get(Integer id, Integer idZamowienia) {
        zamowienie.get(idZamowienia);
        SzczegolZamowienia record;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record =
                    jdbc.queryForObject(
                            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                    + " Szczegol_Zamowienia WHERE id_szczegolu=:id",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
                            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (!Objects.equals(record.getIdZamowienia(), idZamowienia))
            throw new NoSuchElementException("Nie znaleziono rekordu");
        return record;
    }

    public List<SzczegolZamowienia> list(Integer idZamowienia) {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                + " Szczegol_Zamowienia WHERE 1=1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        zamowienie.get(idZamowienia);
        sql.append(" AND id_zamowienia=:id");
        parameters.put("id", idZamowienia);
        sql.append(" ORDER BY id_szczegolu");
        var result =
                jdbc.query(
                        sql.toString(), parameters, new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
        return result;
    }

    @Transactional
    public SzczegolZamowienia create(SzczegolZamowieniaCreate value, Integer idZamowienia) {
        Zamowienie record;
        if (idZamowienia == null || idZamowienia < 1)
            throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record =
                    jdbc.queryForObject(
                            "SELECT"
                                    + " id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita"
                                    + " FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
                            new MapSqlParameterSource().addValue("id", idZamowienia).getValues(),
                            (rs, index) -> {
                                var mappedRecord = new Zamowienie();
                                mappedRecord.setIdZamowienia(rs.getObject("id_zamowienia", Integer.class));
                                mappedRecord.setIdKlienta(rs.getObject("id_klienta", Integer.class));
                                mappedRecord.setIdAdresu(rs.getObject("id_adresu", Integer.class));
                                mappedRecord.setDataZamowienia(
                                        rs.getTimestamp("data_zamowienia") == null
                                                ? null
                                                : rs.getTimestamp("data_zamowienia")
                                                .toLocalDateTime()
                                                .atOffset(java.time.ZoneOffset.UTC));
                                mappedRecord.setStatusZamowienia(
                                        Zamowienie.StatusZamowieniaEnum.fromValue(rs.getString("status_zamowienia")));
                                mappedRecord.setSumaCalkowita(rs.getBigDecimal("suma_calkowita"));
                                return mappedRecord;
                            });
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        jdbc.queryForList("SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
        if (record.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE) {
            List<SzczegolZamowienia> items =
                    jdbc.query(
                            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                    + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
                            new MapSqlParameterSource().addValue("id", idZamowienia).getValues(),
                            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
            jdbc.queryForList(
                    "SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
            for (var l : items)
                jdbc.update(
                        "UPDATE Produkt SET stan_magazynowy=stan_magazynowy+:q WHERE id_produktu=:p",
                        new MapSqlParameterSource()
                                .addValue("q", l.getIlosc())
                                .addValue("p", l.getIdProduktu()));
        }
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getIdProduktu() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_produktu");
        if (value.getIdProduktu() != null) {
            BigDecimal d = new BigDecimal(value.getIdProduktu().toString());
            if (d.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdProduktu() != null) produkt.get(value.getIdProduktu());
        if (value.getIdRecepty() != null) {
            BigDecimal d2 = new BigDecimal(value.getIdRecepty().toString());
            if (d2.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdRecepty() != null) recepta.get(value.getIdRecepty());
        if (value.getIlosc() != null) {
            BigDecimal d3 = new BigDecimal(value.getIlosc().toString());
            if (d3.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id_produktu", value.getIdProduktu());
        parameters.put("id_recepty", value.getIdRecepty());
        parameters.put("ilosc", value.getIlosc() == null ? 1 : (value.getIlosc()));
        parameters.put("id_zamowienia", idZamowienia);
        parameters.put("cena_jednostkowa", produkt.get(value.getIdProduktu()).getCena());
        var key = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO Szczegol_Zamowienia"
                        + " (id_produktu,id_recepty,ilosc,id_zamowienia,cena_jednostkowa) VALUES"
                        + " (:id_produktu,:id_recepty,:ilosc,:id_zamowienia,:cena_jednostkowa)",
                new MapSqlParameterSource(parameters),
                key,
                new String[]{"id_szczegolu"});
        int id = Objects.requireNonNull(key.getKey()).intValue();
        Set<Integer> check = Set.of(id);
        var next = zamowienie.get(idZamowienia);
        AdresKlienta record3;
        if (next.getIdAdresu() == null || next.getIdAdresu() < 1)
            throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record3 =
                    jdbc.queryForObject(
                            "SELECT id_adresu,id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto"
                                    + " FROM Adres_Klienta WHERE id_adresu=:id FOR UPDATE",
                            new MapSqlParameterSource().addValue("id", next.getIdAdresu()).getValues(),
                            new BeanPropertyRowMapper<>(AdresKlienta.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (!Objects.equals(record3.getIdKlienta(), next.getIdKlienta()))
            throw new IllegalArgumentException("Adres należy do innego klienta");
        boolean active = next.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE;
        List<SzczegolZamowienia> items2 =
                jdbc.query(
                        "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
                        new MapSqlParameterSource().addValue("id", idZamowienia).getValues(),
                        new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
        for (var line : items2) {
            boolean strict =
                    check.contains(line.getIdSzczegolu())
                            || active
                            && record.getStatusZamowienia() == Zamowienie.StatusZamowieniaEnum.ANULOWANE;
            var product = produkt.get(line.getIdProduktu());
            if (line.getIdRecepty() != null) {
                Recepta record4;
                if (line.getIdRecepty() == null || line.getIdRecepty() < 1)
                    throw new IllegalArgumentException("Niepoprawne ID");
                try {
                    record4 =
                            jdbc.queryForObject(
                                    "SELECT id_recepty,id_klienta,kod_recepty,pesel_pacjenta,status_recepty FROM"
                                            + " Recepta WHERE id_recepty=:id FOR UPDATE",
                                    new MapSqlParameterSource().addValue("id", line.getIdRecepty()).getValues(),
                                    new BeanPropertyRowMapper<>(Recepta.class));
                } catch (EmptyResultDataAccessException errorItem) {
                    throw new NoSuchElementException("Nie znaleziono rekordu");
                }
                if (!Objects.equals(record4.getIdKlienta(), next.getIdKlienta()))
                    throw new IllegalArgumentException("Recepta należy do innego klienta");
                if (strict
                        && record4.getStatusRecepty() != Recepta.StatusReceptyEnum.NOWA
                        && record4.getStatusRecepty() != Recepta.StatusReceptyEnum.ZWERYFIKOWANA)
                    throw new IllegalArgumentException("Recepta nie jest ważna");
            }
            if (strict && product.getWymagaRecepty() && line.getIdRecepty() == null)
                throw new IllegalArgumentException("Wymagana recepta");
        }
        if (active) {
            for (var l2 : items2)
                if (jdbc.update(
                        "UPDATE Produkt SET stan_magazynowy=stan_magazynowy-:q WHERE id_produktu=:p AND"
                                + " stan_magazynowy>=:q",
                        new MapSqlParameterSource()
                                .addValue("q", l2.getIlosc())
                                .addValue("p", l2.getIdProduktu()))
                        == 0) throw new IllegalArgumentException("Brak produktu na stanie");
        }
        BigDecimal total =
                items2.stream()
                        .map(
                                lineItem ->
                                        lineItem.getCenaJednostkowa().multiply(BigDecimal.valueOf(lineItem.getIlosc())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new IllegalArgumentException("Zbyt duża suma");
        jdbc.update(
                "UPDATE Zamowienie SET suma_calkowita=:total WHERE id_zamowienia=:id",
                new MapSqlParameterSource().addValue("total", total).addValue("id", idZamowienia));
        return get(id, idZamowienia);
    }

    @Transactional
    public SzczegolZamowienia update(
            Integer id, SzczegolZamowieniaUpdate value, Integer idZamowienia) {
        Zamowienie record;
        if (idZamowienia == null || idZamowienia < 1)
            throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record =
                    jdbc.queryForObject(
                            "SELECT"
                                    + " id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita"
                                    + " FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
                            new MapSqlParameterSource().addValue("id", idZamowienia).getValues(),
                            (rs, index) -> {
                                var mappedRecord = new Zamowienie();
                                mappedRecord.setIdZamowienia(rs.getObject("id_zamowienia", Integer.class));
                                mappedRecord.setIdKlienta(rs.getObject("id_klienta", Integer.class));
                                mappedRecord.setIdAdresu(rs.getObject("id_adresu", Integer.class));
                                mappedRecord.setDataZamowienia(
                                        rs.getTimestamp("data_zamowienia") == null
                                                ? null
                                                : rs.getTimestamp("data_zamowienia")
                                                .toLocalDateTime()
                                                .atOffset(java.time.ZoneOffset.UTC));
                                mappedRecord.setStatusZamowienia(
                                        Zamowienie.StatusZamowieniaEnum.fromValue(rs.getString("status_zamowienia")));
                                mappedRecord.setSumaCalkowita(rs.getBigDecimal("suma_calkowita"));
                                return mappedRecord;
                            });
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        jdbc.queryForList("SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
        if (record.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE) {
            List<SzczegolZamowienia> items =
                    jdbc.query(
                            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                    + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
                            new MapSqlParameterSource().addValue("id", idZamowienia).getValues(),
                            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
            jdbc.queryForList(
                    "SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
            for (var l : items)
                jdbc.update(
                        "UPDATE Produkt SET stan_magazynowy=stan_magazynowy+:q WHERE id_produktu=:p",
                        new MapSqlParameterSource()
                                .addValue("q", l.getIlosc())
                                .addValue("p", l.getIdProduktu()));
        }
        var before = record;
        SzczegolZamowienia record4;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record4 =
                    jdbc.queryForObject(
                            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                    + " Szczegol_Zamowienia WHERE id_szczegolu=:id",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
                            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (!Objects.equals(record4.getIdZamowienia(), idZamowienia))
            throw new NoSuchElementException("Nie znaleziono rekordu");
        SzczegolZamowienia record3 = record4;
        var previous = record3;
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getIdProduktu() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_produktu");
        if (value.getIdProduktu() != null) {
            BigDecimal d = new BigDecimal(value.getIdProduktu().toString());
            if (d.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdProduktu() != null) produkt.get(value.getIdProduktu());
        if (value.getIdRecepty() != null) {
            BigDecimal d2 = new BigDecimal(value.getIdRecepty().toString());
            if (d2.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdRecepty() != null) recepta.get(value.getIdRecepty());
        if (value.getIlosc() == null)
            throw new IllegalArgumentException("Pole nie może być null: ilosc");
        if (value.getIlosc() != null) {
            BigDecimal d3 = new BigDecimal(value.getIlosc().toString());
            if (d3.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id_produktu", value.getIdProduktu());
        result.put("id_recepty", value.getIdRecepty());
        result.put("ilosc", value.getIlosc());
        Map<String, Object> parameters = result;
        var data = parameters;
        boolean changed = !Objects.equals(value.getIdProduktu(), previous.getIdProduktu());
        data.put(
                "cena_jednostkowa",
                changed ? produkt.get(value.getIdProduktu()).getCena() : previous.getCenaJednostkowa());
        Map<String, Object> parameters2 = new LinkedHashMap<>(data);
        parameters2.put("_id", id);
        jdbc.update(
                "UPDATE Szczegol_Zamowienia SET"
                        + " id_produktu=:id_produktu,id_recepty=:id_recepty,ilosc=:ilosc,cena_jednostkowa=:cena_jednostkowa"
                        + " WHERE id_szczegolu=:_id",
                new MapSqlParameterSource(parameters2));
        boolean strict =
                changed
                        || !Objects.equals(value.getIdRecepty(), previous.getIdRecepty())
                        || value.getIlosc() > previous.getIlosc();
        Set<Integer> check = strict ? Set.of(id) : Set.of();
        var next = zamowienie.get(idZamowienia);
        AdresKlienta record5;
        if (next.getIdAdresu() == null || next.getIdAdresu() < 1)
            throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record5 =
                    jdbc.queryForObject(
                            "SELECT id_adresu,id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto"
                                    + " FROM Adres_Klienta WHERE id_adresu=:id FOR UPDATE",
                            new MapSqlParameterSource().addValue("id", next.getIdAdresu()).getValues(),
                            new BeanPropertyRowMapper<>(AdresKlienta.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (!Objects.equals(record5.getIdKlienta(), next.getIdKlienta()))
            throw new IllegalArgumentException("Adres należy do innego klienta");
        boolean active = next.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE;
        List<SzczegolZamowienia> items2 =
                jdbc.query(
                        "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
                        new MapSqlParameterSource().addValue("id", idZamowienia).getValues(),
                        new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
        for (var line : items2) {
            boolean strict2 =
                    check.contains(line.getIdSzczegolu())
                            || active
                            && before.getStatusZamowienia() == Zamowienie.StatusZamowieniaEnum.ANULOWANE;
            var product = produkt.get(line.getIdProduktu());
            if (line.getIdRecepty() != null) {
                Recepta record6;
                if (line.getIdRecepty() == null || line.getIdRecepty() < 1)
                    throw new IllegalArgumentException("Niepoprawne ID");
                try {
                    record6 =
                            jdbc.queryForObject(
                                    "SELECT id_recepty,id_klienta,kod_recepty,pesel_pacjenta,status_recepty FROM"
                                            + " Recepta WHERE id_recepty=:id FOR UPDATE",
                                    new MapSqlParameterSource().addValue("id", line.getIdRecepty()).getValues(),
                                    new BeanPropertyRowMapper<>(Recepta.class));
                } catch (EmptyResultDataAccessException errorItem) {
                    throw new NoSuchElementException("Nie znaleziono rekordu");
                }
                if (!Objects.equals(record6.getIdKlienta(), next.getIdKlienta()))
                    throw new IllegalArgumentException("Recepta należy do innego klienta");
                if (strict2
                        && record6.getStatusRecepty() != Recepta.StatusReceptyEnum.NOWA
                        && record6.getStatusRecepty() != Recepta.StatusReceptyEnum.ZWERYFIKOWANA)
                    throw new IllegalArgumentException("Recepta nie jest ważna");
            }
            if (strict2 && product.getWymagaRecepty() && line.getIdRecepty() == null)
                throw new IllegalArgumentException("Wymagana recepta");
        }
        if (active) {
            for (var l2 : items2)
                if (jdbc.update(
                        "UPDATE Produkt SET stan_magazynowy=stan_magazynowy-:q WHERE id_produktu=:p AND"
                                + " stan_magazynowy>=:q",
                        new MapSqlParameterSource()
                                .addValue("q", l2.getIlosc())
                                .addValue("p", l2.getIdProduktu()))
                        == 0) throw new IllegalArgumentException("Brak produktu na stanie");
        }
        BigDecimal total =
                items2.stream()
                        .map(
                                lineItem ->
                                        lineItem.getCenaJednostkowa().multiply(BigDecimal.valueOf(lineItem.getIlosc())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new IllegalArgumentException("Zbyt duża suma");
        jdbc.update(
                "UPDATE Zamowienie SET suma_calkowita=:total WHERE id_zamowienia=:id",
                new MapSqlParameterSource().addValue("total", total).addValue("id", idZamowienia));
        return get(id, idZamowienia);
    }

    @Transactional
    public void delete(Integer id, Integer idZamowienia) {
        if (id == null || id < 1 || idZamowienia == null || idZamowienia < 1)
            throw new IllegalArgumentException("Niepoprawne ID");
        if (jdbc.queryForList(
                        "SELECT id_zamowienia FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
                        Map.of("id", idZamowienia))
                .isEmpty()) throw new NoSuchElementException("Nie znaleziono zamówienia");
        if (jdbc.queryForList(
                        "SELECT id_szczegolu FROM Szczegol_Zamowienia WHERE id_szczegolu=:id AND"
                                + " id_zamowienia=:order",
                        Map.of("id", id, "order", idZamowienia))
                .isEmpty()) return;
        Zamowienie record2;
        if (idZamowienia == null || idZamowienia < 1)
            throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record2 =
                    jdbc.queryForObject(
                            "SELECT"
                                    + " id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita"
                                    + " FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
                            new MapSqlParameterSource().addValue("id", idZamowienia).getValues(),
                            (rs, index) -> {
                                var mappedRecord = new Zamowienie();
                                mappedRecord.setIdZamowienia(rs.getObject("id_zamowienia", Integer.class));
                                mappedRecord.setIdKlienta(rs.getObject("id_klienta", Integer.class));
                                mappedRecord.setIdAdresu(rs.getObject("id_adresu", Integer.class));
                                mappedRecord.setDataZamowienia(
                                        rs.getTimestamp("data_zamowienia") == null
                                                ? null
                                                : rs.getTimestamp("data_zamowienia")
                                                .toLocalDateTime()
                                                .atOffset(java.time.ZoneOffset.UTC));
                                mappedRecord.setStatusZamowienia(
                                        Zamowienie.StatusZamowieniaEnum.fromValue(rs.getString("status_zamowienia")));
                                mappedRecord.setSumaCalkowita(rs.getBigDecimal("suma_calkowita"));
                                return mappedRecord;
                            });
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        jdbc.queryForList("SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
        if (record2.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE) {
            List<SzczegolZamowienia> items =
                    jdbc.query(
                            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                    + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
                            new MapSqlParameterSource().addValue("id", idZamowienia).getValues(),
                            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
            jdbc.queryForList(
                    "SELECT id_produktu FROM Produkt ORDER BY id_produktu FOR UPDATE", Map.of());
            for (var l : items)
                jdbc.update(
                        "UPDATE Produkt SET stan_magazynowy=stan_magazynowy+:q WHERE id_produktu=:p",
                        new MapSqlParameterSource()
                                .addValue("q", l.getIlosc())
                                .addValue("p", l.getIdProduktu()));
        }
        Zamowienie record = record2;
        var before = record;
        SzczegolZamowienia record4;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record4 =
                    jdbc.queryForObject(
                            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                    + " Szczegol_Zamowienia WHERE id_szczegolu=:id",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
                            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (!Objects.equals(record4.getIdZamowienia(), idZamowienia))
            throw new NoSuchElementException("Nie znaleziono rekordu");
        SzczegolZamowienia record3 = record4;
        jdbc.update(
                "DELETE FROM Szczegol_Zamowienia WHERE id_szczegolu=:id AND id_zamowienia=:order",
                new MapSqlParameterSource().addValue("id", id).addValue("order", idZamowienia));
        Set<Integer> check = Set.of();
        var next = zamowienie.get(idZamowienia);
        AdresKlienta record5;
        if (next.getIdAdresu() == null || next.getIdAdresu() < 1)
            throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record5 =
                    jdbc.queryForObject(
                            "SELECT id_adresu,id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto"
                                    + " FROM Adres_Klienta WHERE id_adresu=:id FOR UPDATE",
                            new MapSqlParameterSource().addValue("id", next.getIdAdresu()).getValues(),
                            new BeanPropertyRowMapper<>(AdresKlienta.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (!Objects.equals(record5.getIdKlienta(), next.getIdKlienta()))
            throw new IllegalArgumentException("Adres należy do innego klienta");
        boolean active = next.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE;
        List<SzczegolZamowienia> items2 =
                jdbc.query(
                        "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
                        new MapSqlParameterSource().addValue("id", idZamowienia).getValues(),
                        new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
        for (var line : items2) {
            boolean strict =
                    check.contains(line.getIdSzczegolu())
                            || active
                            && before.getStatusZamowienia() == Zamowienie.StatusZamowieniaEnum.ANULOWANE;
            var product = produkt.get(line.getIdProduktu());
            if (line.getIdRecepty() != null) {
                Recepta record6;
                if (line.getIdRecepty() == null || line.getIdRecepty() < 1)
                    throw new IllegalArgumentException("Niepoprawne ID");
                try {
                    record6 =
                            jdbc.queryForObject(
                                    "SELECT id_recepty,id_klienta,kod_recepty,pesel_pacjenta,status_recepty FROM"
                                            + " Recepta WHERE id_recepty=:id FOR UPDATE",
                                    new MapSqlParameterSource().addValue("id", line.getIdRecepty()).getValues(),
                                    new BeanPropertyRowMapper<>(Recepta.class));
                } catch (EmptyResultDataAccessException errorItem) {
                    throw new NoSuchElementException("Nie znaleziono rekordu");
                }
                if (!Objects.equals(record6.getIdKlienta(), next.getIdKlienta()))
                    throw new IllegalArgumentException("Recepta należy do innego klienta");
                if (strict
                        && record6.getStatusRecepty() != Recepta.StatusReceptyEnum.NOWA
                        && record6.getStatusRecepty() != Recepta.StatusReceptyEnum.ZWERYFIKOWANA)
                    throw new IllegalArgumentException("Recepta nie jest ważna");
            }
            if (strict && product.getWymagaRecepty() && line.getIdRecepty() == null)
                throw new IllegalArgumentException("Wymagana recepta");
        }
        if (active) {
            for (var l2 : items2)
                if (jdbc.update(
                        "UPDATE Produkt SET stan_magazynowy=stan_magazynowy-:q WHERE id_produktu=:p AND"
                                + " stan_magazynowy>=:q",
                        new MapSqlParameterSource()
                                .addValue("q", l2.getIlosc())
                                .addValue("p", l2.getIdProduktu()))
                        == 0) throw new IllegalArgumentException("Brak produktu na stanie");
        }
        BigDecimal total =
                items2.stream()
                        .map(
                                lineItem ->
                                        lineItem.getCenaJednostkowa().multiply(BigDecimal.valueOf(lineItem.getIlosc())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new IllegalArgumentException("Zbyt duża suma");
        jdbc.update(
                "UPDATE Zamowienie SET suma_calkowita=:total WHERE id_zamowienia=:id",
                new MapSqlParameterSource().addValue("total", total).addValue("id", idZamowienia));
    }
}
