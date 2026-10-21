# PROJEKT BAZODANOWY APTEKA
## PODZIAŁ PRACY W ZESPOLE
**Relacyjna baza danych apteki – 4 osoby:**
- **Osoba 1:** Karol Walczyszyn
- **Osoba 2:** Kacper Uchyła
- **Osoba 3:** Łukasz Ziembiński
- **Osoba 4:** Aleksandra Sandomierska

### 1. Cel i zakres projektu
Celem projektu jest stworzenie relacyjnej bazy danych apteki w MariaDB oraz prostej aplikacji internetowej współpracującej z bazą przez API. System ma obsługiwać m.in. katalog leków i stany magazynowe, klientów, pracowników, recepty, zamówienia (sprzedaż) oraz raporty analityczne.
Najważniejsze elementy projektu to poprawny model danych (3NF), zapytania SQL, procedura rejestracji zamówienia/sprzedaży, trigger, transakcje oraz działający interfejs demonstracyjny (CRUD).

### 2. Podział ról

| Osoba / rola                                                                | Główne zadania                                                                                                                                                                                                                                                                         | Rezultat                                                                      |
| :-------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :---------------------------------------------------------------------------- |
| **Osoba 1 – Architekt bazy, dokumentalista i kierownik projektu** (ok. 26%) | • Ustalenie wymagań, reguł biznesowych i organizacja pracy (harmonogram, repozytorium)<br>• Projekt tabel, relacji i diagramu ER<br>• Normalizacja do 3NF i słownik danych<br>• Skrypty `DBInit.sql` (`schema.sql`) i `DBTestData.sql` (`seed.sql`)<br>• Dokumentacja końcowa i slajdy | Struktura bazy, dane testowe, diagram ER, dokumentacja i koordynacja zespołu. |
| **Osoba 2 – Programista SQL** (ok. 24%)                                     | • Minimum 5 zapytań raportowych<br>• Widoki SQL<br>• Procedura rejestracji zamówienia/sprzedaży<br>• Trigger i demonstracja transakcji<br>• Role, uprawnienia i testy SQL<br>• Wsparcie danych dla ekranu raportów                                                                     | Plik `queries.sql`, procedura, trigger, raporty i wyniki testów.              |
| **Osoba 3 – Programista Backend (API)** (ok. 24%)                           | • Specyfikacja i implementacja endpointów API<br>• Połączenie backendu z MariaDB<br>• Obsługa błędów, autoryzacji i zapytań parametryzowanych<br>• Endpointy dla leków, klientów, pracowników, recept i zamówień<br>• Instrukcja uruchomienia                                          | Działające API łączące bazę z frontendem oraz instrukcja wdrożenia.           |
| **Osoba 4 – Programista Frontend** (ok. 26%)                                | • Układ strony i wspólne komponenty<br>• Ekran logowania (klient / pracownik)<br>• Lista i wyszukiwanie leków<br>• Formularz nowego zamówienia / sprzedaży<br>• Ekran recept, klientów i raportów<br>• Integracja z API i scenariusz demo                                              | Działający interfejs użytkownika oraz demonstracja aplikacji.                 |

### 3. Etapy realizacji
*   **Etap 1 - Ustalenia i projekt:** Wspólne ustalenie funkcji systemu. Osoba 1 przygotowuje model danych, dokumentację i harmonogram, a Osoba 3 wstępną specyfikację API. Osoba 4 rozpoczyna makiety interfejsu.
*   **Etap 2 - Baza danych:** Osoba 1 tworzy tabele (`DBInit.sql`) i dane testowe (`DBTestData.sql`). Osoba 2 zapoznaje się ze schematem, a Osoby 3 i 4 przygotowują szkielet aplikacji.
*   **Etap 3 - Logika i aplikacja:** Osoba 2 tworzy zapytania, widoki, procedurę, trigger i transakcje (`queries.sql`). Osoba 3 rozwija API, a Osoba 4 kolejne ekrany.
*   **Etap 4 - Integracja i testy:** Zespół łączy frontend, API i bazę, sprawdza składanie zamówień, weryfikację recept, stany magazynowe, raporty oraz obsługę błędów.
*   **Etap 5 - Dokumentacja i prezentacja:** Uzupełnienie dokumentacji, przygotowanie instrukcji uruchomienia, slajdów i wspólna próba prezentacji pod nadzorem Osoby 1.

### 4. Przekazywanie pracy między osobami

| Od          | Do             | Co przekazuje                                                                |
| :---------- | :------------- | :--------------------------------------------------------------------------- |
| **Osoba 1** | Osoby 2, 3 i 4 | Diagram ER, `DBInit.sql` i nazwy kolumn; następnie `DBTestData.sql`.         |
| **Osoba 2** | Osoba 3        | Nazwy procedur i widoków, parametry oraz format wyników z `queries.sql`.     |
| **Osoba 2** | Osoba 4        | Wymagania i struktura danych dla ekranu raportów.                            |
| **Osoba 3** | Osoba 4        | Specyfikacja API, przykładowe JSON-y i działające endpointy.                 |
| **Wszyscy** | Wszyscy        | Wyniki testów, błędy i poprawki w repozytorium (koordynowane przez Osobę 1). |

### 5. Testy i kryteria ukończenia
*   Baza tworzy się od zera (`DBInit.sql`), a dane testowe (`DBTestData.sql`) ładują się bez błędów.
*   Klucze obce i ograniczenia (`CHECK`, `UNIQUE`, `NOT NULL`) poprawnie blokują niedozwolone dane.
*   Zapytania i widoki zwracają oczekiwane wyniki analityczne.
*   Procedura zamówienia zmienia stan magazynowy (`dostepne`), weryfikuje recepty i odrzuca niepoprawne operacje.
*   Trigger oraz transakcje (`COMMIT` i `ROLLBACK`) można zademonstrować na żywo.
*   Frontend komunikuje się z API i wyświetla błędy w czytelny sposób.
*   Projekt można uruchomić według instrukcji, a każdy członek zespołu potrafi omówić swoją część.