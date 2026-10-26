package pk.bd.apteka.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.KlientApi;
import pk.bd.apteka.dto.AktualizacjaKlienta;
import pk.bd.apteka.dto.Klient;

@RestController
public class KlienciController implements KlientApi {

    @Override
    public ResponseEntity<Void> deleteKlient(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteKlient'");
    }

    @Override
    public ResponseEntity<Klient> getKlient(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getKlient'");
    }

    @Override
    public ResponseEntity<Klient> updateKlient(Long id, @Valid AktualizacjaKlienta aktualizacjaKlienta) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateKlient'");
    }
}