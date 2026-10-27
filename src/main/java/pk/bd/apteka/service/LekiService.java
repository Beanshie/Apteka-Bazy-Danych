package pk.bd.apteka.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import pk.bd.apteka.dto.DaneLek;
import pk.bd.apteka.dto.Lek;

import java.util.ArrayList;
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
        return lek;
    }
}
