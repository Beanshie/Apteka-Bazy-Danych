# PROJEKT BAZODANOWY APTEKA
## PODZIAŁ PRACY W ZESPOLE
**Relacyjna baza danych apteki – 4 osoby:**
- **Osoba 1:** Karol Walczyszyn
- **Osoba 2:** Kacper Uchyła
- **Osoba 3:** Łukasz Ziembiński
- **Osoba 4:** Aleksandra Sandomierska

### 1. Cel i zakres projektu
Celem projektu jest stworzenie relacyjnej bazy danych apteki w MariaDB oraz prostej aplikacji internetowej współpracującej z bazą przez API. System ma obsługiwać m.in. leki, partie i stany magazynowe, dostawy, klientów, recepty, sprzedaż oraz raporty.
Najważniejsze elementy projektu to poprawny model danych, zapytania SQL, procedura rejestracji sprzedaży, trigger, transakcje oraz działający interfejs demonstracyjny.

### 2. Podział ról

| Osoba / rola                                            | Główne zadania                                                                                                                                                                                        | Rezultat                                                         |
| :------------------------------------------------------ | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------- |
| **Osoba 1 – Architekt bazy i dokumentalista** (ok. 26%) | • Ustalenie wymagań i reguł biznesowych<br>• Projekt tabel, relacji i diagramu ER<br>• Normalizacja do 3NF i słownik danych<br>• Skrypty `schema.sql` i `seed.sql`<br>• Dokumentacja końcowa i slajdy | Struktura bazy, dane testowe, diagram ER i dokumentacja.         |
| **Osoba 2 – Programista SQL** (ok. 24%)                 | • Minimum 5 zapytań raportowych<br>• Widoki SQL<br>• Procedura rejestracji sprzedaży<br>• Trigger i demonstracja transakcji<br>• Role, uprawnienia i testy SQL<br>• Ekran raportów                    | Plik `queries.sql`, procedura, trigger, raporty i wyniki testów. |
| **Osoba 3 – Backend i kierownik projektu** (ok. 24%)    | • Specyfikacja i implementacja API<br>• Połączenie backendu z MariaDB<br>• Obsługa błędów i zapytań parametryzowanych<br>• Repozytorium i harmonogram<br>• Ekrany dostaw oraz klientów i dostawców    | Działające API, instrukcja uruchomienia i organizacja pracy.     |
| **Osoba 4 – Frontend** (ok. 26%)                        | • Układ strony i wspólne komponenty<br>• Ekran logowania<br>• Lista i wyszukiwanie leków<br>• Formularz nowej sprzedaży<br>• Ekran recept<br>• Integracja z API i scenariusz demo                     | Działający interfejs oraz demonstracja aplikacji.                |

### 3. Etapy realizacji
*   **Etap 1 - Ustalenia i projekt:** Wspólne ustalenie funkcji systemu. Osoba 1 przygotowuje model danych, a Osoba 3 wstępną specyfikację API. Osoba 4 może rozpocząć makiety interfejsu.
*   **Etap 2 - Baza danych:** Osoba 1 tworzy tabele i dane testowe. Osoba 2 zapoznaje się ze schematem, a Osoby 3 i 4 przygotowują szkielet aplikacji.
*   **Etap 3 - Logika i aplikacja:** Osoba 2 tworzy zapytania, widoki, procedurę, trigger i transakcje. Osoba 3 rozwija API, a Osoba 4 kolejne ekrany.
*   **Etap 4 - Integracja i testy:** Zespół łączy frontend, API i bazę, sprawdza sprzedaż, stany magazynowe, raporty oraz obsługę błędów.
*   **Etap 5 - Dokumentacja i prezentacja:** Uzupełnienie dokumentacji, przygotowanie instrukcji uruchomienia, slajdów i wspólna próba prezentacji.

### 4. Przekazywanie pracy między osobami

| Od          | Do             | Co przekazuje                                                |
| :---------- | :------------- | :----------------------------------------------------------- |
| **Osoba 1** | Osoby 2, 3 i 4 | Diagram ER, `schema.sql` i nazwy kolumn; później `seed.sql`. |
| **Osoba 2** | Osoba 3        | Nazwy procedur i widoków, parametry oraz format wyników.     |
| **Osoba 2** | Osoba 4        | Wymagania i dane dla ekranu raportów.                        |
| **Osoba 3** | Osoba 4        | Specyfikacja API, przykładowe JSON-y i działające endpointy. |
| **Wszyscy** | Wszyscy        | Wyniki testów, błędy i poprawki.                             |

### 5. Testy i kryteria ukończenia
*   Baza tworzy się od zera, a dane testowe ładują się bez błędów.
*   Klucze obce i ograniczenia poprawnie blokują niedozwolone dane.
*   Zapytania i widoki zwracają oczekiwane wyniki.
*   Procedura sprzedaży zmienia stan magazynowy i odrzuca niepoprawne operacje.
*   Trigger oraz `COMMIT` i `ROLLBACK` można zademonstrować.
*   Frontend komunikuje się z API i wyświetla błędy w czytelny sposób.
*   Projekt można uruchomić według instrukcji, a każdy członek zespołu potrafi omówić swoją część.