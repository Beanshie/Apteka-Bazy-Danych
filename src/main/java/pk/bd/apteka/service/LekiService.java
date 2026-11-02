package pk.bd.apteka.service;

import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class LekiService {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    public List<Lek> getLeki() {
        String sql = "SELECT * FROM Lek";
        List<Lek> list=jdbc.query(sql, new BeanPropertyRowMapper<>(Lek.class));
        return list;
    }

    public Lek addLek(DaneLek daneLek){
        Lek lek=new Lek(daneLek.getNazwa(),daneLek.getCena(),daneLek.getOpis(),null);
        String sql = "INSERT INTO Lek (nazwa, cena, opis, wymagana_Recepta, dostepne) " +
                "VALUES (:nazwa, :cena, :opis, :wymaganaRecepta, :dostepne)";
        SqlParameterSource params = new BeanPropertySqlParameterSource(lek);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(sql, params, keyHolder, new String[]{"lek_ID"});

        if (keyHolder.getKey() != null) {
            Integer wygenerowaneId = keyHolder.getKey().intValue();
            lek.setLekId(wygenerowaneId);
            return lek;
        }
        lek.setLekId(-1);
        return lek;
    }

    public Lek getLekById(Integer lekID)
    {
        String sql = "SELECT * FROM Lek WHERE lek_ID = :lekID";
        SqlParameterSource params = new MapSqlParameterSource("lekID", lekID);
        try {
            return jdbc.queryForObject(sql, params, new BeanPropertyRowMapper<>(Lek.class));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Lek updateLek(Integer lekID,DaneLek daneLek )
    {
        Lek lek=new Lek(daneLek.getNazwa(),daneLek.getCena(),daneLek.getOpis(),lekID);
        String sql = "UPDATE Lek SET " +
                "nazwa = :nazwa, " +
                "cena = :cena, " +
                "opis = :opis, " +
                "wymagana_Recepta = :wymaganaRecepta, " +
                "dostepne = :dostepne " +
                "WHERE lek_ID = :lekID";

        SqlParameterSource params = new BeanPropertySqlParameterSource(lek);
        int rowsAffected = jdbc.update(sql, params);//dodaj sprawdzenie
        return lek
    }
}
