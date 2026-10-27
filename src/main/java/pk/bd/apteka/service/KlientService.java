package pk.bd.apteka.service;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import pk.bd.apteka.dto.Klient;
import pk.bd.apteka.dto.UtworzenieKlienta;

import java.util.ArrayList;
import java.util.List;

@Service
public class KlientService {

    public List<Klient> listKlienci() {
        Klient klient1=new Klient(1L,"Jan");
        Klient klient2=new Klient(2L,"Pawel");
        List<Klient> lista = new ArrayList<>();
        lista.add(klient1);
        lista.add(klient2);
        return lista;
    }

    public Klient createKlient(@NonNull UtworzenieKlienta utworzenieKlienta){
        //TODO: Zapis do bazy
        Klient nowyKlient = new Klient(0L,utworzenieKlienta.getLogin());
        return nowyKlient;
    }
}
