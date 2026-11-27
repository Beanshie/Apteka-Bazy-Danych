# Dokumentacja warstwy logiki bazodanowej – Etap 3

Niniejszy dokument zawiera specyfikację techniczną oraz opis zastosowań biznesowych dla obiektów zaimplementowanych w pliku `queries.sql` (środowisko MariaDB/MySQL). Przygotowane rozwiązania obejmują warstwę raportowo-analityczną, abstrakcję danych poprzez widoki, enkapsulację logiki biznesowej (funkcje, procedury, wyzwalacze) oraz mechanizmy kontroli spójności transakcyjnej.

---

## 1. Zapytania analityczne i raportowe (`SELECT`)

### 1.1. Ranking klientów o najwyższej wartości zakupów
* **Mechanizmy SQL:** `INNER JOIN`, `GROUP BY`, funkcje agregujące (`COUNT`, `SUM`, `AVG`), `CONCAT`, `ROUND`, filtrowanie statusów (`<> 'ANULOWANE'`).
* **Zastosowanie biznesowe i systemowe:**
  * Wyłanianie kluczowych odbiorców na podstawie wolumenu zamówień, łącznego przychodu oraz średniej wartości koszyka.
  * Źródło danych dla modułu CRM, segmentacji klientów oraz programów lojalnościowych (np. automatyczne przyznawanie rabatów).

### 1.2. Analiza struktury sprzedaży według kategorii produktów
* **Mechanizmy SQL:** wielokrotne złączenia (`INNER JOIN` na czterech tabelach), `GROUP BY`, `HAVING`, `COUNT(DISTINCT ...)`.
* **Zastosowanie biznesowe i systemowe:**
  * Zestawienie przychodu oraz liczby sprzedanych jednostek w podziale na kategorie asortymentowe z wyłączeniem transakcji anulowanych.
  * Podstawa dla panelu zarządczego (dashboardów KPI) oraz analizy rentowności poszczególnych segmentów oferty apteki.

### 1.3. Identyfikacja produktów o cenie wyższej niż średnia w kategorii
* **Mechanizmy SQL:** podzapytania skorelowane w klauzulach `SELECT` i `WHERE`, `INNER JOIN`.
* **Zastosowanie biznesowe i systemowe:**
  * Dynamiczne porównanie ceny jednostkowej każdego produktu ze średnią ceną wyliczoną wyłącznie dla jego kategorii, wzbogacone o dane dostawcy.
  * Wsparcie dla działu zakupów przy weryfikacji polityki cenowej, kontroli marż oraz identyfikacji preparatów z segmentu premium.

### 1.4. Raport weryfikacyjny zamówień z lekami na receptę (Rx)
* **Mechanizmy SQL:** złączenia sześciu tabel (`INNER JOIN` dla danych obligatoryjnych oraz `LEFT JOIN` dla powiązań opcjonalnych z receptami i płatnościami).
* **Zastosowanie biznesowe i systemowe:**
  * Skonsolidowane zestawienie operacyjne dla farmaceutów, agregujące dane pacjenta, zamawiany lek Rx, kod i status recepty oraz status płatności.
  * Usprawnienie procesu weryfikacji przed wydaniem leku oraz zapewnienie gotowego materiału pod audyt zgodności z przepisami farmaceutycznymi.

### 1.5. Analiza towarów nierotujących (zalegających w magazynie)
* **Mechanizmy SQL:** podzapytanie z predykatem `NOT EXISTS`, wyliczenia wartościowe (`stan_magazynowy * cena`).
* **Zastosowanie biznesowe i systemowe:**
  * Wykrywanie pozycji magazynowych bez historii sprzedaży oraz oszacowanie kapitału zamrożonego w tych produktach.
  * Kluczowe narzędzie w zarządzaniu zapasami aptecznymi (szczególnie w kontekście terminów ważności leków), wspierające decyzje o wyprzedażach lub optymalizacji kolejnych dostaw.

---

## 2. Widoki bazodanowe (`VIEW`)

### 2.1. `v_katalog_produktow`
* **Specyfikacja:** Łączy tabele `Produkt`, `Kategoria` i `Dostawca`, mapując flagę binarną `wymaga_recepty` na czytelne etykiety tekstowe (*Na receptę (Rx)* / *Bez recepty (OTC)*) za pomocą wyrażenia `CASE`.
* **Rola w architekturze systemu:** Stanowi warstwę abstrakcji dla aplikacji backendowej i frontendowej. Eliminuje konieczność powielania złożonych złączeń w kodzie aplikacji, udostępniając gotowy model oferty produktowej.

### 2.2. `v_podsumowanie_zamowien`
* **Specyfikacja:** Agreguje informacje o zamówieniach, klientach, płatnościach oraz formatuje kompletny adres dostawy w pojedynczy ciąg znaków z bezpieczną obsługą opcjonalnego numeru mieszkania (`IFNULL`).
* **Rola w architekturze systemu:** Dostarcza ustandaryzowany zestaw danych na potrzeby obsługi logistycznej (generowanie etykiet kurierskich) oraz prezentacji historii zamówień w interfejsie użytkownika.

---

## 3. Funkcja składowana (`FUNCTION`)

### 3.1. `fn_oblicz_wartosc_zamowienia(p_id_zamowienia)`
* **Specyfikacja:** Deterministyczna funkcja odczytująca dane (`READS SQL DATA`), która dla zadanego identyfikatora wylicza łączną wartość pozycji w tabeli `Szczegol_Zamowienia`, zwracając `0.00` w przypadku braku rekordów.
* **Rola w architekturze systemu:** Centralizuje logikę obliczania wartości zamówienia w jednym miejscu w bazie danych. Zapewnia spójność wyliczeń pomiędzy raportami, procedurami oraz wyzwalaczami.

---

## 4. Wyzwalacze (`TRIGGER`)

### 4.1. `trg_sprawdz_recepte_przed_dodaniem` (`BEFORE INSERT`)
* **Specyfikacja:** Przed zapisaniem rekordu w tabeli `Szczegol_Zamowienia` weryfikuje flagę `wymaga_recepty` dla wybranego produktu. W przypadku braku powiązania z receptą (`id_recepty IS NULL`) przerywa operację i zgłasza wyjątek (`SIGNAL SQLSTATE '45000'`).
* **Rola w architekturze systemu:** Wymusza integralność biznesową i prawną bezpośrednio na poziomie silnika bazy danych, zabezpieczając system przed błędami walidacji po stronie aplikacji klienckiej.

### 4.2. `trg_aktualizuj_sume_zamowienia` (`AFTER INSERT`)
* **Specyfikacja:** Po dodaniu każdej nowej pozycji do `Szczegol_Zamowienia` automatycznie aktualizuje kolumnę `suma_calkowita` w tabeli `Zamowienie`, wykorzystując funkcję `fn_oblicz_wartosc_zamowienia`.
* **Rola w architekturze systemu:** Gwarantuje utrzymanie spójności danych zagregowanych bez konieczności ręcznego przeliczania kwot przez warstwę backendową.

---

## 5. Procedury składowana (`PROCEDURE`)

### 5.1. `sp_zloz_zamowienie`
* **Specyfikacja:** Hermetyzuje pełny proces rejestracji zamówienia w ramach pojedynczej transakcji z obsługą wyjątków (`DECLARE EXIT HANDLER FOR SQLEXCEPTION` wywołujący `ROLLBACK` i `RESIGNAL`). Obejmuje blokadę pesymistyczną wiersza (`FOR UPDATE`), weryfikację stanu magazynowego, utworzenie nagłówka i pozycji zamówienia, dekrementację stanu magazynowego oraz inicjalizację płatności.
* **Rola w architekturze systemu:**
  * **Kontrola współbieżności:** Blokada `FOR UPDATE` eliminuje zjawisko wyścigu (*race condition*) przy jednoczesnym zakupie ostatnich sztuk tego samego leku.
  * **Atomowość:** Zapewnia zasadę „wszystko albo nic” – błąd na dowolnym etapie (np. brak towaru lub blokada wyzwalacza receptowego) całkowicie wycofuje zmiany, chroniąc bazę przed niespójnością.
  * **Uproszczenie integracji:** Redukuje złożoność kodu backendowego do pojedynczego wywołania `CALL sp_zloz_zamowienie(...)`.
  
### 5.2. `sp_archiwizuj_zamowienia(p_data_graniczna)`
* **Specyfikacja:** Przyjmuje datę graniczną (`DATE`) i w ramach pojedynczej transakcji z obsługą wyjątków (`ROLLBACK` + `RESIGNAL`) tworzy tabele archiwalne (`Archiwum_Zamowien` oraz `Archiwum_Szczegol_Zamowienia` przez `CREATE TABLE IF NOT EXISTS ... LIKE`), kopiuje do nich zamówienia starsze niż podana data wraz ze szczegółami, a następnie usuwa zarchiwizowane rekordy (oraz powiązane płatności) z głównych tabel.
* **Rola w architekturze systemu:** Umożliwia cykliczne odciążanie głównych tabel produkcyjnych bez utraty danych historycznych, co poprawia wydajność zapytań w miarę wzrostu liczby zamówień.

### 5.3. `sp_wyczysc_baze()`
* **Specyfikacja:** Tymczasowo wyłącza walidację kluczy obcych (`SET FOREIGN_KEY_CHECKS = 0`), usuwa wszystkie wiersze z tabel bazy danych za pomocą `TRUNCATE TABLE` (zachowując strukturę tabel i resetując liczniki `AUTO_INCREMENT`), a następnie ponownie włącza kontrolę kluczy obcych.
* **Rola w architekturze systemu:** Narzędzie wspierające środowisko deweloperskie i testowe – pozwala jednym wywołaniem przywrócić bazę do czystego stanu przed ponownym załadowaniem danych testowych (`DBTestData.sql`).
---

## 6. Obsługa transakcji (`COMMIT` i `ROLLBACK`)

* **Scenariusz zatwierdzenia (`COMMIT`):** Demonstruje atomowe wykonanie powiązanych operacji magazynowo-cenowych (przyjęcie dostawy +50 sztuk oraz aktualizacja ceny jednostkowej produktu) i ich trwały zapis w bazie.
* **Scenariusz wycofania (`ROLLBACK`):** Ilustruje mechanizm bezpiecznego cofnięcia omyłkowej modyfikacji statusu zamówienia (`ANULOWANE`) przed zatwierdzeniem transakcji, przywracając bazę do stanu sprzed rozpoczęcia operacji.