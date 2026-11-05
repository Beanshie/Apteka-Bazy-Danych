package pk.bd.apteka.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.KlientApi;
import pk.bd.apteka.dto.AktualizacjaKlienta;
import pk.bd.apteka.dto.Klient;
import pk.bd.apteka.service.KlienciService;

@RestController
public class KlienciController implements KlientApi {

    @Autowired
    private KlienciService klienciService;

    @Override
    public ResponseEntity<Void> deleteKlient(Long id) {
        klienciService.deleteKlient(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Klient> getKlient(Long id) {
        Klient klient = klienciService.getKlient(id);
        return ResponseEntity.ok(klient);
    }

    @Override
    public ResponseEntity<Klient> updateKlient(Long id, @Valid AktualizacjaKlienta aktualizacjaKlienta) {
        Klient klient = klienciService.updateKliet(id,aktualizacjaKlienta);
        return ResponseEntity.ok(klient);
    }
}