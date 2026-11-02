package pk.bd.apteka.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import pk.bd.apteka.api.LekiApi;
import pk.bd.apteka.dto.DaneLek;
import pk.bd.apteka.dto.Lek;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import pk.bd.apteka.service.LekiService;

@CrossOrigin(origins = "*")
@RestController
public class LekiController implements LekiApi {

    @Autowired
    private LekiService lekiService;

    @Override
    public ResponseEntity<Lek> addLek(DaneLek daneLek) {
        Lek lek= lekiService.addLek(daneLek);
        return ResponseEntity.ok(lek);
    }

    @Override
    public ResponseEntity<Lek> getLekById(Integer lekID) {
        Lek lek = lekiService.getLekById(lekID);
        return ResponseEntity.ok(lek);
    }

    @Override
    public ResponseEntity<List<Lek>> getLeki() {
        List<Lek> lista=lekiService.getLeki();
        return ResponseEntity.ok(lista);
    }

    @Override
    public ResponseEntity<Lek> updateLek(Integer lekID, DaneLek daneLek) {
        Lek lek = lekiService.updateLek(lekID,daneLek);
        return ResponseEntity.ok(lek);
    }
}
