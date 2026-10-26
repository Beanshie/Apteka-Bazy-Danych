package pk.bd.apteka.leki;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import pk.bd.apteka.api.LekiApi;
import pk.bd.apteka.dto.DaneLek;
import pk.bd.apteka.dto.Lek;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "*")
@RestController
public class LekiController implements LekiApi {
    @Override
    public ResponseEntity<Lek> addLek(DaneLek daneLek) {
        return null;
    }

    @Override
    public ResponseEntity<Lek> getLekById(Integer lekID) {
        return null;
    }

    @Override
    public ResponseEntity<List<Lek>> getLeki() {
        List<Lek> lista=new ArrayList<>();
        lista.add(new Lek("Apap",14.23F,"Lek123",13));
        lista.add(new Lek("Ibuprom",10.23F,"Lek321",18));
        return ResponseEntity.ok(lista);
    }

    @Override
    public ResponseEntity<Lek> updateLek(Integer lekID, DaneLek daneLek) {
        return null;
    }
}
