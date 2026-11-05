package pk.bd.apteka.service;


import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import pk.bd.apteka.dto.AktualizacjaKlienta;
import pk.bd.apteka.dto.Klient;

@Service
public class KlienciService {

    public void deleteKlient(Long id){
        //TODO: Usuniecie z bazy klienta
        return;
    }

    public Klient getKlient(Long id){
        //TODO:Przeszukuje baze klientow po id
        Klient klient = new Klient();
        return klient;
    }

    public Klient updateKliet(Long id, @Valid AktualizacjaKlienta aktualizacjaKlienta){
        //TODO: wyciaga i aktualizuje klienta po id
        Klient klient = new Klient();
        return klient;
    }
}
