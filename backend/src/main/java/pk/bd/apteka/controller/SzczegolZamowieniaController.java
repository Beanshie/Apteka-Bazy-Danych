package pk.bd.apteka.controller;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.SzczegolZamowieniaApi;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

@RestController
public class SzczegolZamowieniaController implements SzczegolZamowieniaApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(SzczegolZamowieniaController.class);

    private final SzczegolZamowieniaService service;

    public SzczegolZamowieniaController(SzczegolZamowieniaService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<SzczegolZamowienia> createSzczegolZamowienia(
            Integer idZamowienia, SzczegolZamowieniaCreate szczegolZamowieniaCreate) {
        try {
            return ResponseEntity.status(201)
                    .body(service.create(szczegolZamowieniaCreate, idZamowienia));

        } catch (NoSuchElementException error) {
            return ResponseEntity.notFound().build();
        } catch (DataIntegrityViolationException error) {
            return ResponseEntity.status(409).build();
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().build();
        } catch (DataAccessException error) {
            LOGGER.error("Database operation failed", error);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<Void> deleteSzczegolZamowienia(Integer idZamowienia, Integer idSzczegolu) {
        try {
            service.delete(idSzczegolu, idZamowienia);
            return ResponseEntity.noContent().build();

        } catch (NoSuchElementException error) {
            return ResponseEntity.notFound().build();
        } catch (DataIntegrityViolationException error) {
            return ResponseEntity.status(409).build();
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().build();
        } catch (DataAccessException error) {
            LOGGER.error("Database operation failed", error);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<SzczegolZamowienia> getSzczegolZamowienia(
            Integer idZamowienia, Integer idSzczegolu) {
        try {
            return ResponseEntity.ok(service.get(idSzczegolu, idZamowienia));

        } catch (NoSuchElementException error) {
            return ResponseEntity.notFound().build();
        } catch (DataIntegrityViolationException error) {
            return ResponseEntity.status(409).build();
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().build();
        } catch (DataAccessException error) {
            LOGGER.error("Database operation failed", error);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<SzczegolZamowienia>> listSzczegolZamowienia(Integer idZamowienia) {
        try {
            return ResponseEntity.ok(service.list(idZamowienia));

        } catch (NoSuchElementException error) {
            return ResponseEntity.notFound().build();
        } catch (DataIntegrityViolationException error) {
            return ResponseEntity.status(409).build();
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().build();
        } catch (DataAccessException error) {
            LOGGER.error("Database operation failed", error);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<SzczegolZamowienia> updateSzczegolZamowienia(
            Integer idZamowienia,
            Integer idSzczegolu,
            SzczegolZamowieniaUpdate szczegolZamowieniaUpdate) {
        try {
            return ResponseEntity.ok(service.update(idSzczegolu, szczegolZamowieniaUpdate, idZamowienia));

        } catch (NoSuchElementException error) {
            return ResponseEntity.notFound().build();
        } catch (DataIntegrityViolationException error) {
            return ResponseEntity.status(409).build();
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().build();
        } catch (DataAccessException error) {
            LOGGER.error("Database operation failed", error);
            return ResponseEntity.internalServerError().build();
        }
    }
}
