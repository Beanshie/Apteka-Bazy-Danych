package pk.bd.apteka.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import pk.bd.apteka.dto.*;
import pk.bd.apteka.service.KlientService;

class KlientControllerTest {
  @Test
  void successfulCrudReturnsExpectedStatusesAndBodies() {
    var service = mock(KlientService.class);
    var controller = new KlientController(service);
    var client = new Klient().idKlienta(7);
    var create = new KlientCreate();
    var update = new KlientUpdate();
    when(service.create(create)).thenReturn(client);
    when(service.get(7)).thenReturn(client);
    when(service.update(7, update)).thenReturn(client);
    when(service.list()).thenReturn(List.of(client));

    var created = controller.createKlient(create);
    assertEquals(201, created.getStatusCode().value());
    assertSame(client, created.getBody());
    var found = controller.getKlient(7);
    assertEquals(200, found.getStatusCode().value());
    assertSame(client, found.getBody());
    var updated = controller.updateKlient(7, update);
    assertEquals(200, updated.getStatusCode().value());
    assertSame(client, updated.getBody());
    var listed = controller.listKlient();
    assertEquals(200, listed.getStatusCode().value());
    assertEquals(List.of(client), listed.getBody());
    var deleted = controller.deleteKlient(7);
    assertEquals(204, deleted.getStatusCode().value());
    assertNull(deleted.getBody());
    verify(service).delete(7);
  }
}
