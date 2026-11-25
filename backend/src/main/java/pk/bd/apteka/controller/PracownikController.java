package pk.bd.apteka.controller;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.PracownikApi;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

@RestController
public class PracownikController implements PracownikApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(PracownikController.class);

    private final PracownikService service;

    public PracownikController(PracownikService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<Pracownik> createPracownik(PracownikCreate pracownikCreate) {
        try {
            return ResponseEntity.status(201).body(service.create(pracownikCreate));

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
    public ResponseEntity<Void> deletePracownik(Integer idPracownika) {
        try {
            service.delete(idPracownika);
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
    public ResponseEntity<Pracownik> getPracownik(Integer idPracownika) {
        try {
            return ResponseEntity.ok(service.get(idPracownika));

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
    public ResponseEntity<List<Pracownik>> listPracownik() {
        try {
            return ResponseEntity.ok(service.list());

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
    public ResponseEntity<Pracownik> updatePracownik(
            Integer idPracownika, PracownikUpdate pracownikUpdate) {
        try {
            return ResponseEntity.ok(service.update(idPracownika, pracownikUpdate));

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
