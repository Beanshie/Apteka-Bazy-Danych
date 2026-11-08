package pk.bd.apteka.controller;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.ProduktApi;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

@RestController
public class ProduktController implements ProduktApi {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProduktController.class);

  private final ProduktService service;

  public ProduktController(ProduktService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<Produkt> createProdukt(ProduktCreate produktCreate) {
    try {
      return ResponseEntity.status(201).body(service.create(produktCreate));

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
  public ResponseEntity<Void> deleteProdukt(Integer idProduktu) {
    try {
      service.delete(idProduktu);
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
  public ResponseEntity<Produkt> getProdukt(Integer idProduktu) {
    try {
      return ResponseEntity.ok(service.get(idProduktu));

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
  public ResponseEntity<List<Produkt>> listProdukt(Integer idKategorii, Integer idDostawcy) {
    try {
      return ResponseEntity.ok(service.list(idKategorii, idDostawcy));

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
  public ResponseEntity<Produkt> updateProdukt(Integer idProduktu, ProduktUpdate produktUpdate) {
    try {
      return ResponseEntity.ok(service.update(idProduktu, produktUpdate));

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
