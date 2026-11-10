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
public class ZamowienieService {

    private final NamedParameterJdbcTemplate jdbc;

    private final AdresKlientaService adresklienta;

    private final KlientService klient;

    private final ProduktService produkt;

    private final ReceptaService recepta;

    public ZamowienieService(
            NamedParameterJdbcTemplate jdbc,
            AdresKlientaService adresklienta,
            KlientService klient,
            ProduktService produkt,
            ReceptaService recepta) {
        this.jdbc = jdbc;
        this.adresklienta = adresklienta;
        this.klient = klient;
        this.produkt = produkt;
        this.recepta = recepta;
    }

    public Zamowienie get(Integer id) {
        Zamowienie record;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record =
                    jdbc.queryForObject(
                            "SELECT"
                                    + " id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita"
                                    + " FROM Zamowienie WHERE id_zamowienia=:id",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
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
        var result = record;
        List<SzczegolZamowienia> items =
                jdbc.query(
                        "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
                        new MapSqlParameterSource().addValue("id", id).getValues(),
                        new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
        result.setPozycje(items);
        return result;
    }

    public List<Zamowienie> list(Integer idKlienta, String statusZamowienia) {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT"
                                + " id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita"
                                + " FROM Zamowienie WHERE 1=1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (idKlienta != null) {
            sql.append(" AND id_klienta=:id_klienta");
            parameters.put("id_klienta", idKlienta);
        }
        if (statusZamowienia != null) {
            sql.append(" AND status_zamowienia=:status_zamowienia");
            parameters.put("status_zamowienia", statusZamowienia);
        }
        sql.append(" ORDER BY id_zamowienia");
        var result =
                jdbc.query(
                        sql.toString(),
                        parameters,
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
        for (var value : result) {
            List<SzczegolZamowienia> items =
                    jdbc.query(
                            "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                    + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
                            new MapSqlParameterSource().addValue("id", value.getIdZamowienia()).getValues(),
                            new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
            value.setPozycje(items);
        }
        return result;
    }

    @Transactional
    public Zamowienie create(ZamowienieCreate value) {
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getIdKlienta() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_klienta");
        if (value.getIdKlienta() != null) {
            BigDecimal d = new BigDecimal(value.getIdKlienta().toString());
            if (d.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdKlienta() != null) klient.get(value.getIdKlienta());
        if (value.getIdAdresu() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_adresu");
        if (value.getIdAdresu() != null) {
            BigDecimal d2 = new BigDecimal(value.getIdAdresu().toString());
            if (d2.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdAdresu() != null) adresklienta.get(value.getIdAdresu());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id_klienta", value.getIdKlienta());
        result.put("id_adresu", value.getIdAdresu());
        result.put(
                "status_zamowienia",
                value.getStatusZamowienia() == null ? "NOWE" : (value.getStatusZamowienia().getValue()));
        Map<String, Object> parameters = result;
        var key = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO Zamowienie (id_klienta,id_adresu,status_zamowienia) VALUES"
                        + " (:id_klienta,:id_adresu,:status_zamowienia)",
                new MapSqlParameterSource(parameters),
                key,
                new String[]{"id_zamowienia"});
        int generatedId = Objects.requireNonNull(key.getKey()).intValue();
        Integer id = generatedId;
        Zamowienie record2;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record2 =
                    jdbc.queryForObject(
                            "SELECT"
                                    + " id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita"
                                    + " FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
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
                            new MapSqlParameterSource().addValue("id", id).getValues(),
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
        if (value.getPozycje() == null || value.getPozycje().isEmpty())
            throw new IllegalArgumentException("Wymagane pozycje");
        jdbc.update(
                "DELETE FROM Szczegol_Zamowienia WHERE id_zamowienia=:id",
                new MapSqlParameterSource().addValue("id", id));
        Set<Integer> check2 = new HashSet<>();
        for (var item : value.getPozycje()) {
            if (item == null) throw new IllegalArgumentException("Pole nie może być null: pozycja");
            if (item.getIdProduktu() == null)
                throw new IllegalArgumentException("Pole nie może być null: id_produktu");
            if (item.getIdProduktu() == null || item.getIdProduktu() < 1)
                throw new IllegalArgumentException("Niepoprawne ID");
            int quantity = item.getIlosc() == null ? 1 : item.getIlosc();
            BigDecimal d3 = new BigDecimal(String.valueOf(quantity));
            if (d3.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
            if (item.getIdRecepty() != null) recepta.get(item.getIdRecepty());
            var key2 = new GeneratedKeyHolder();
            jdbc.update(
                    "INSERT INTO Szczegol_Zamowienia"
                            + " (id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa) VALUES"
                            + " (:order,:product,:recipe,:quantity,:price)",
                    new MapSqlParameterSource(
                            new MapSqlParameterSource()
                                    .addValue("order", id)
                                    .addValue("product", item.getIdProduktu())
                                    .addValue("recipe", item.getIdRecepty())
                                    .addValue("quantity", quantity)
                                    .addValue("price", produkt.get(item.getIdProduktu()).getCena())
                                    .getValues()),
                    key2,
                    new String[]{"id_szczegolu"});
            check2.add(Objects.requireNonNull(key2.getKey()).intValue());
        }
        Set<Integer> record3 = check2;
        var check = record3;
        var next = get(id);
        AdresKlienta record4;
        if (next.getIdAdresu() == null || next.getIdAdresu() < 1)
            throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record4 =
                    jdbc.queryForObject(
                            "SELECT id_adresu,id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto"
                                    + " FROM Adres_Klienta WHERE id_adresu=:id FOR UPDATE",
                            new MapSqlParameterSource().addValue("id", next.getIdAdresu()).getValues(),
                            new BeanPropertyRowMapper<>(AdresKlienta.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (!Objects.equals(record4.getIdKlienta(), next.getIdKlienta()))
            throw new IllegalArgumentException("Adres należy do innego klienta");
        boolean active = next.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE;
        List<SzczegolZamowienia> items2 =
                jdbc.query(
                        "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
                        new MapSqlParameterSource().addValue("id", id).getValues(),
                        new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
        for (var line : items2) {
            boolean strict =
                    check.contains(line.getIdSzczegolu())
                            || active
                            && before.getStatusZamowienia() == Zamowienie.StatusZamowieniaEnum.ANULOWANE;
            var product = produkt.get(line.getIdProduktu());
            if (line.getIdRecepty() != null) {
                Recepta record5;
                if (line.getIdRecepty() == null || line.getIdRecepty() < 1)
                    throw new IllegalArgumentException("Niepoprawne ID");
                try {
                    record5 =
                            jdbc.queryForObject(
                                    "SELECT id_recepty,id_klienta,kod_recepty,pesel_pacjenta,status_recepty FROM"
                                            + " Recepta WHERE id_recepty=:id FOR UPDATE",
                                    new MapSqlParameterSource().addValue("id", line.getIdRecepty()).getValues(),
                                    new BeanPropertyRowMapper<>(Recepta.class));
                } catch (EmptyResultDataAccessException errorItem) {
                    throw new NoSuchElementException("Nie znaleziono rekordu");
                }
                if (!Objects.equals(record5.getIdKlienta(), next.getIdKlienta()))
                    throw new IllegalArgumentException("Recepta należy do innego klienta");
                if (strict
                        && record5.getStatusRecepty() != Recepta.StatusReceptyEnum.NOWA
                        && record5.getStatusRecepty() != Recepta.StatusReceptyEnum.ZWERYFIKOWANA)
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
                new MapSqlParameterSource().addValue("total", total).addValue("id", id));
        return get(id);
    }

    @Transactional
    public Zamowienie update(Integer id, ZamowienieUpdate value) {
        Zamowienie record2;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record2 =
                    jdbc.queryForObject(
                            "SELECT"
                                    + " id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita"
                                    + " FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
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
                            new MapSqlParameterSource().addValue("id", id).getValues(),
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
        if (value == null) throw new IllegalArgumentException("Pole nie może być null: body");
        if (value.getIdKlienta() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_klienta");
        if (value.getIdKlienta() != null) {
            BigDecimal d = new BigDecimal(value.getIdKlienta().toString());
            if (d.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdKlienta() != null) klient.get(value.getIdKlienta());
        if (value.getIdAdresu() == null)
            throw new IllegalArgumentException("Pole nie może być null: id_adresu");
        if (value.getIdAdresu() != null) {
            BigDecimal d2 = new BigDecimal(value.getIdAdresu().toString());
            if (d2.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
        }
        if (value.getIdAdresu() != null) adresklienta.get(value.getIdAdresu());
        if (value.getStatusZamowienia() == null)
            throw new IllegalArgumentException("Pole nie może być null: status_zamowienia");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id_klienta", value.getIdKlienta());
        result.put("id_adresu", value.getIdAdresu());
        result.put(
                "status_zamowienia",
                value.getStatusZamowienia() == null ? null : value.getStatusZamowienia().getValue());
        Map<String, Object> parameters = result;
        Map<String, Object> parameters2 = new LinkedHashMap<>(parameters);
        parameters2.put("_id", id);
        jdbc.update(
                "UPDATE Zamowienie SET"
                        + " id_klienta=:id_klienta,id_adresu=:id_adresu,status_zamowienia=:status_zamowienia"
                        + " WHERE id_zamowienia=:_id",
                new MapSqlParameterSource(parameters2));
        if (value.getPozycje() == null || value.getPozycje().isEmpty())
            throw new IllegalArgumentException("Wymagane pozycje");
        jdbc.update(
                "DELETE FROM Szczegol_Zamowienia WHERE id_zamowienia=:id",
                new MapSqlParameterSource().addValue("id", id));
        Set<Integer> check2 = new HashSet<>();
        for (var item : value.getPozycje()) {
            if (item == null) throw new IllegalArgumentException("Pole nie może być null: pozycja");
            if (item.getIdProduktu() == null)
                throw new IllegalArgumentException("Pole nie może być null: id_produktu");
            if (item.getIdProduktu() == null || item.getIdProduktu() < 1)
                throw new IllegalArgumentException("Niepoprawne ID");
            int quantity = item.getIlosc() == null ? 1 : item.getIlosc();
            BigDecimal d3 = new BigDecimal(String.valueOf(quantity));
            if (d3.compareTo(BigDecimal.valueOf(1)) < 0)
                throw new IllegalArgumentException("Niepoprawna liczba");
            if (item.getIdRecepty() != null) recepta.get(item.getIdRecepty());
            var key = new GeneratedKeyHolder();
            jdbc.update(
                    "INSERT INTO Szczegol_Zamowienia"
                            + " (id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa) VALUES"
                            + " (:order,:product,:recipe,:quantity,:price)",
                    new MapSqlParameterSource(
                            new MapSqlParameterSource()
                                    .addValue("order", id)
                                    .addValue("product", item.getIdProduktu())
                                    .addValue("recipe", item.getIdRecepty())
                                    .addValue("quantity", quantity)
                                    .addValue("price", produkt.get(item.getIdProduktu()).getCena())
                                    .getValues()),
                    key,
                    new String[]{"id_szczegolu"});
            check2.add(Objects.requireNonNull(key.getKey()).intValue());
        }
        Set<Integer> record3 = check2;
        var check = record3;
        var next = get(id);
        AdresKlienta record4;
        if (next.getIdAdresu() == null || next.getIdAdresu() < 1)
            throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record4 =
                    jdbc.queryForObject(
                            "SELECT id_adresu,id_klienta,ulica,numer_domu,numer_mieszkania,kod_pocztowy,miasto"
                                    + " FROM Adres_Klienta WHERE id_adresu=:id FOR UPDATE",
                            new MapSqlParameterSource().addValue("id", next.getIdAdresu()).getValues(),
                            new BeanPropertyRowMapper<>(AdresKlienta.class));
        } catch (EmptyResultDataAccessException errorItem) {
            throw new NoSuchElementException("Nie znaleziono rekordu");
        }
        if (!Objects.equals(record4.getIdKlienta(), next.getIdKlienta()))
            throw new IllegalArgumentException("Adres należy do innego klienta");
        boolean active = next.getStatusZamowienia() != Zamowienie.StatusZamowieniaEnum.ANULOWANE;
        List<SzczegolZamowienia> items2 =
                jdbc.query(
                        "SELECT id_szczegolu,id_zamowienia,id_produktu,id_recepty,ilosc,cena_jednostkowa FROM"
                                + " Szczegol_Zamowienia WHERE id_zamowienia=:id ORDER BY id_szczegolu",
                        new MapSqlParameterSource().addValue("id", id).getValues(),
                        new BeanPropertyRowMapper<>(SzczegolZamowienia.class));
        for (var line : items2) {
            boolean strict =
                    check.contains(line.getIdSzczegolu())
                            || active
                            && before.getStatusZamowienia() == Zamowienie.StatusZamowieniaEnum.ANULOWANE;
            var product = produkt.get(line.getIdProduktu());
            if (line.getIdRecepty() != null) {
                Recepta record5;
                if (line.getIdRecepty() == null || line.getIdRecepty() < 1)
                    throw new IllegalArgumentException("Niepoprawne ID");
                try {
                    record5 =
                            jdbc.queryForObject(
                                    "SELECT id_recepty,id_klienta,kod_recepty,pesel_pacjenta,status_recepty FROM"
                                            + " Recepta WHERE id_recepty=:id FOR UPDATE",
                                    new MapSqlParameterSource().addValue("id", line.getIdRecepty()).getValues(),
                                    new BeanPropertyRowMapper<>(Recepta.class));
                } catch (EmptyResultDataAccessException errorItem) {
                    throw new NoSuchElementException("Nie znaleziono rekordu");
                }
                if (!Objects.equals(record5.getIdKlienta(), next.getIdKlienta()))
                    throw new IllegalArgumentException("Recepta należy do innego klienta");
                if (strict
                        && record5.getStatusRecepty() != Recepta.StatusReceptyEnum.NOWA
                        && record5.getStatusRecepty() != Recepta.StatusReceptyEnum.ZWERYFIKOWANA)
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
                new MapSqlParameterSource().addValue("total", total).addValue("id", id));
        return get(id);
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        if (jdbc.queryForList(
                        "SELECT id_zamowienia FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
                        Map.of("id", id))
                .isEmpty()) return;
        Zamowienie record2;
        if (id == null || id < 1) throw new IllegalArgumentException("Niepoprawne ID");
        try {
            record2 =
                    jdbc.queryForObject(
                            "SELECT"
                                    + " id_zamowienia,id_klienta,id_adresu,data_zamowienia,status_zamowienia,suma_calkowita"
                                    + " FROM Zamowienie WHERE id_zamowienia=:id FOR UPDATE",
                            new MapSqlParameterSource().addValue("id", id).getValues(),
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
                            new MapSqlParameterSource().addValue("id", id).getValues(),
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
        jdbc.update(
                "DELETE FROM Zamowienie WHERE id_zamowienia=:id",
                new MapSqlParameterSource().addValue("id", id));
    }
}
