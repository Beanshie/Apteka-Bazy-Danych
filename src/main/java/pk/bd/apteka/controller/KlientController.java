package pk.bd.apteka.controller;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.KlienciApi;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.KlientService;
import pk.bd.apteka.service.LekiService;

import java.util.ArrayList;
import java.util.List;


@RestController
public class KlientController implements KlienciApi {

    @Autowired
    private KlientService klientService;

    @Override
    public ResponseEntity<Klient> createKlient(UtworzenieKlienta utworzenieKlienta) {
    Klient klient = klientService.createKlient(utworzenieKlienta);
    return ResponseEntity.ok(klient);
    }

    @Override
    public ResponseEntity<List<Klient>> listKlienci() {
        List<Klient> lista= klientService.listKlienci();
        return ResponseEntity.ok(lista);
    }
}
