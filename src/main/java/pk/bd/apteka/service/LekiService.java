package pk.bd.apteka.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import pk.bd.apteka.dto.Lek;

import java.util.ArrayList;
import java.util.List;

@Service
public class LekiService {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    public List<Lek> getLeki() {
        dbConnection.execut("select * from leki")
        List<Lek> lista=new ArrayList<>();
        lista.add(new Lek("Apap",14.23F,"Lek123",13));
        lista.add(new Lek("Ibuprom",10.23F,"Lek321",18));
        return lista;
    }
}
