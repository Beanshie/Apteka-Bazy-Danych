package pk.bd.apteka.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.ZamowienieService;

class ZamowienieControllerTest {
  private ZamowienieService service;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    service = mock(ZamowienieService.class);
    mvc = MockMvcBuilders.standaloneSetup(new ZamowienieController(service)).build();
  }

  private Zamowienie header() {
    return new Zamowienie().idZamowienia(1).idKlienta(1).idAdresu(1)
        .dataZamowienia(OffsetDateTime.parse("2026-11-21T12:00:00Z"))
        .statusZamowienia(Zamowienie.StatusZamowieniaEnum.NOWE).sumaCalkowita(BigDecimal.ZERO);
  }

  @Test
  void createAcceptsHeaderWithoutItemsAndReturnsNoEmbeddedItems() throws Exception {
    when(service.create(any())).thenReturn(header());
    mvc.perform(post("/zamowienia").contentType(MediaType.APPLICATION_JSON)
            .content("{\"id_klienta\":1,\"id_adresu\":1}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id_zamowienia").value(1))
        .andExpect(jsonPath("$.suma_calkowita").value(0))
        .andExpect(jsonPath("$.pozycje").doesNotExist());
    verify(service).create(argThat(value -> value.getIdKlienta() == 1 && value.getIdAdresu() == 1));
  }

  @Test
  void updateAcceptsHeaderWithoutItems() throws Exception {
    when(service.update(eq(1), any())).thenReturn(header());
    mvc.perform(put("/zamowienia/1").contentType(MediaType.APPLICATION_JSON)
            .content("{\"id_klienta\":1,\"id_adresu\":1,\"status_zamowienia\":\"NOWE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pozycje").doesNotExist());
    verify(service).update(eq(1), argThat(value -> value.getStatusZamowienia() == ZamowienieUpdate.StatusZamowieniaEnum.NOWE));
  }

  @Test
  void getAndListReturnHeadersWithoutEmbeddedItems() throws Exception {
    when(service.get(1)).thenReturn(header());
    when(service.list(null, null)).thenReturn(List.of(header()));
    mvc.perform(get("/zamowienia/1")).andExpect(status().isOk())
        .andExpect(jsonPath("$.pozycje").doesNotExist());
    mvc.perform(get("/zamowienia")).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id_zamowienia").value(1))
        .andExpect(jsonPath("$[0].pozycje").doesNotExist());
  }
}
