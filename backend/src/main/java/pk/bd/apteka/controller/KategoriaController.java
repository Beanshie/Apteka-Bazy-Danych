package pk.bd.apteka.controller;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.KategoriaApi;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

@RestController
public class KategoriaController implements KategoriaApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(KategoriaController.class);

    private final KategoriaService service;

    public KategoriaController(KategoriaService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<Kategoria> createKategoria(KategoriaCreate kategoriaCreate) {
        try {
            return ResponseEntity.status(201).body(service.create(kategoriaCreate));

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
    public ResponseEntity<Void> deleteKategoria(Integer idKategorii) {
        try {
            service.delete(idKategorii);
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
    public ResponseEntity<Kategoria> getKategoria(Integer idKategorii) {
        try {
            return ResponseEntity.ok(service.get(idKategorii));

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
    public ResponseEntity<List<Kategoria>> listKategoria() {
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
    public ResponseEntity<Kategoria> updateKategoria(
            Integer idKategorii, KategoriaUpdate kategoriaUpdate) {
        try {
            return ResponseEntity.ok(service.update(idKategorii, kategoriaUpdate));

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
