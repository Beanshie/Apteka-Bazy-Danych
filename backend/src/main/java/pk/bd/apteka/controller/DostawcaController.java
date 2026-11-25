package pk.bd.apteka.controller;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.DostawcaApi;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

@RestController
public class DostawcaController implements DostawcaApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(DostawcaController.class);

    private final DostawcaService service;

    public DostawcaController(DostawcaService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<Dostawca> createDostawca(DostawcaCreate dostawcaCreate) {
        try {
            return ResponseEntity.status(201).body(service.create(dostawcaCreate));

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
    public ResponseEntity<Void> deleteDostawca(Integer idDostawcy) {
        try {
            service.delete(idDostawcy);
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
    public ResponseEntity<Dostawca> getDostawca(Integer idDostawcy) {
        try {
            return ResponseEntity.ok(service.get(idDostawcy));

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
    public ResponseEntity<List<Dostawca>> listDostawca() {
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
    public ResponseEntity<Dostawca> updateDostawca(
            Integer idDostawcy, DostawcaUpdate dostawcaUpdate) {
        try {
            return ResponseEntity.ok(service.update(idDostawcy, dostawcaUpdate));

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
