package pk.bd.apteka.controller;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pk.bd.apteka.api.ZamowienieApi;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

@RestController
public class ZamowienieController implements ZamowienieApi {
  private static final Logger LOGGER = LoggerFactory.getLogger(ZamowienieController.class);

  private final ZamowienieService service;

  public ZamowienieController(ZamowienieService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<Zamowienie> createZamowienie(ZamowienieCreate zamowienieCreate) {
    try {
      return ResponseEntity.status(201).body(service.create(zamowienieCreate));

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
  public ResponseEntity<Void> deleteZamowienie(Integer idZamowienia) {
    try {
      service.delete(idZamowienia);
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
  public ResponseEntity<Zamowienie> getZamowienie(Integer idZamowienia) {
    try {
      return ResponseEntity.ok(service.get(idZamowienia));

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
  public ResponseEntity<List<Zamowienie>> listZamowienie(
      Integer idKlienta, String statusZamowienia) {
    try {
      return ResponseEntity.ok(service.list(idKlienta, statusZamowienia));

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
  public ResponseEntity<Zamowienie> updateZamowienie(
      Integer idZamowienia, ZamowienieUpdate zamowienieUpdate) {
    try {
      return ResponseEntity.ok(service.update(idZamowienia, zamowienieUpdate));

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
