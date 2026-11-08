package pk.bd.apteka.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.*;

class KategoriaControllerTest {
  KategoriaService service;
  MockMvc mvc;

  @BeforeEach
  void setup() {
    service = mock(KategoriaService.class);
    mvc = MockMvcBuilders.standaloneSetup(new KategoriaController(service)).build();
  }

  @Test
  void createReturns201() throws Exception {
    when(service.create(any())).thenReturn(new Kategoria().idKategorii(1).nazwaKategorii("Test"));
    mvc.perform(
            post("/kategorie")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nazwa_kategorii\":\"Test\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id_kategorii").value(1));
  }

  @Test
  void putReplacesFullObject() throws Exception {
    when(service.update(eq(1), any()))
        .thenReturn(new Kategoria().idKategorii(1).nazwaKategorii("Changed"));
    mvc.perform(
            put("/kategorie/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nazwa_kategorii\":\"Changed\"}"))
        .andExpect(status().isOk());
    verify(service)
        .update(
            eq(1),
            argThat(
                value -> value.getNazwaKategorii().equals("Changed") && value.getOpis() == null));
  }

  @Test
  void incompleteReplacementReturns400() throws Exception {
    mvc.perform(
            put("/kategorie/1").contentType(MediaType.APPLICATION_JSON).content("{\"opis\":null}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void missingResourceReturns404() throws Exception {
    when(service.get(1)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
    mvc.perform(get("/kategorie/1")).andExpect(status().isNotFound());
  }

  @Test
  void integrityConflictReturns409() throws Exception {
    doThrow(new DataIntegrityViolationException("private database details"))
        .when(service)
        .delete(1);
    mvc.perform(delete("/kategorie/1"))
        .andExpect(status().isConflict())
        .andExpect(content().string(""));
  }

  @Test
  void invalidBodyReturns400() throws Exception {
    mvc.perform(post("/kategorie").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void deleteReturns204() throws Exception {
    mvc.perform(delete("/kategorie/1")).andExpect(status().isNoContent());
  }

  @Test
  void malformedUpdateReturns400() throws Exception {
    mvc.perform(put("/kategorie/1").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
}
