package pk.bd.apteka.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pk.bd.apteka.api.LekiApi;
import pk.bd.apteka.dto.DaneLek;
import pk.bd.apteka.dto.Lek;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.web.bind.annotation.CrossOrigin;
import pk.bd.apteka.service.LekiService;

@CrossOrigin(origins = "*")
@RestController
public class LekiController implements LekiApi {

    @Autowired
    private LekiService lekiService;

    @Override
    public ResponseEntity<Lek> addLek(DaneLek daneLek) {
        try {
            Lek lek = lekiService.addLek(daneLek);
            return ResponseEntity.status(HttpStatus.CREATED).body(lek);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Lek> getLekById(Integer lekID) {
        if (lekID == null || lekID <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawne ID leku.");
        }
        try {
            Lek lek = lekiService.getLekById(lekID);
            return ResponseEntity.ok(lek);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<List<Lek>> getLeki() {
        List<Lek> lista = lekiService.getLeki();
        return ResponseEntity.ok(lista);
    }

    @Override
    public ResponseEntity<Lek> updateLek(Integer lekID, DaneLek daneLek) {
        try {
            Lek lek = lekiService.updateLek(lekID, daneLek);
            return ResponseEntity.ok(lek);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
