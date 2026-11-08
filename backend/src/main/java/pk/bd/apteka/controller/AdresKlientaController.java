package pk.bd.apteka.controller;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.AdresKlientaApi;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

@RestController
public class AdresKlientaController implements AdresKlientaApi {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdresKlientaController.class);

  private final AdresKlientaService service;

  public AdresKlientaController(AdresKlientaService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<AdresKlienta> createAdresKlienta(AdresKlientaCreate adresKlientaCreate) {
    try {
      return ResponseEntity.status(201).body(service.create(adresKlientaCreate));

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
  public ResponseEntity<Void> deleteAdresKlienta(Integer idAdresu) {
    try {
      service.delete(idAdresu);
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
  public ResponseEntity<AdresKlienta> getAdresKlienta(Integer idAdresu) {
    try {
      return ResponseEntity.ok(service.get(idAdresu));

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
  public ResponseEntity<List<AdresKlienta>> listAdresKlienta(Integer idKlienta) {
    try {
      return ResponseEntity.ok(service.list(idKlienta));

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
  public ResponseEntity<AdresKlienta> updateAdresKlienta(
      Integer idAdresu, AdresKlientaUpdate adresKlientaUpdate) {
    try {
      return ResponseEntity.ok(service.update(idAdresu, adresKlientaUpdate));

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
