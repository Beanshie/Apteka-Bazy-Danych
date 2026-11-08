package pk.bd.apteka.controller;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.ReceptaApi;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

@RestController
public class ReceptaController implements ReceptaApi {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReceptaController.class);

  private final ReceptaService service;

  public ReceptaController(ReceptaService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<Recepta> createRecepta(ReceptaCreate receptaCreate) {
    try {
      return ResponseEntity.status(201).body(service.create(receptaCreate));

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
  public ResponseEntity<Void> deleteRecepta(Integer idRecepty) {
    try {
      service.delete(idRecepty);
      return ResponseEntity.noContent().build();

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
  public ResponseEntity<Recepta> getRecepta(Integer idRecepty) {
    try {
      return ResponseEntity.ok(service.get(idRecepty));

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
  public ResponseEntity<List<Recepta>> listRecepta(
      Integer idKlienta, String kodRecepty, String statusRecepty) {
    try {
      return ResponseEntity.ok(service.list(idKlienta, kodRecepty, statusRecepty));

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
  public ResponseEntity<Recepta> updateRecepta(Integer idRecepty, ReceptaUpdate receptaUpdate) {
    try {
      return ResponseEntity.ok(service.update(idRecepty, receptaUpdate));

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
