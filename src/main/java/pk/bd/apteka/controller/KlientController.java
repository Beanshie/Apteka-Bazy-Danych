package pk.bd.apteka.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.KlienciApi;
import pk.bd.apteka.dto.*;

import java.util.ArrayList;
import java.util.List;


@RestController
public class KlientController implements KlienciApi {

    @Override
    public ResponseEntity<Klient> createKlient(UtworzenieKlienta utworzenieKlienta) {
        return null;
    }

    @Override
    public ResponseEntity<List<Klient>> listKlienci() {
        Klient klient1=new Klient(1L,"Jan");
        Klient klient2=new Klient(2L,"Pawel");
        List<Klient> lista = new ArrayList<>();
        lista.add(klient1);
        lista.add(klient2);
        return ResponseEntity.ok(lista);
    }
}
