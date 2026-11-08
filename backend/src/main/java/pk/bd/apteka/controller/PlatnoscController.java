package pk.bd.apteka.controller;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.PlatnoscApi;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

@RestController
public class PlatnoscController implements PlatnoscApi {
  private static final Logger LOGGER = LoggerFactory.getLogger(PlatnoscController.class);

  private final PlatnoscService service;

  public PlatnoscController(PlatnoscService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<Platnosc> createPlatnosc(PlatnoscCreate platnoscCreate) {
    try {
      return ResponseEntity.status(201).body(service.create(platnoscCreate));

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
  public ResponseEntity<Void> deletePlatnosc(Integer idPlatnosci) {
    try {
      service.delete(idPlatnosci);
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
  public ResponseEntity<Platnosc> getPlatnosc(Integer idPlatnosci) {
    try {
      return ResponseEntity.ok(service.get(idPlatnosci));

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
  public ResponseEntity<List<Platnosc>> listPlatnosc(Integer idZamowienia) {
    try {
      return ResponseEntity.ok(service.list(idZamowienia));

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
  public ResponseEntity<Platnosc> updatePlatnosc(
      Integer idPlatnosci, PlatnoscUpdate platnoscUpdate) {
    try {
      return ResponseEntity.ok(service.update(idPlatnosci, platnoscUpdate));

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
