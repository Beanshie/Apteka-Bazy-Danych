# Plan Działania i Specyfikacja Wymagań Projektu

**Przedmiot:** Projektowanie i tworzenie relacyjnej bazy danych
**Prowadzący:** prof. Olena Stankevych | **Wybrana dziedzina:** nr 11 -- Apteka
**Skład zespołu:** Karol Walczyszyn (Architekt bazy i Kierownik projektu), Kacper Uchyła (Programista SQL), Łukasz Ziembiński (Backend), Aleksandra Sandomierska (Frontend)

---

## 1. Cel projektu i założenia biznesowe
Celem projektu jest praktyczne zaprojektowanie, zaimplementowanie oraz zintegrowanie relacyjnej bazy danych wspierającej obsługę zamówień, weryfikację recept i gospodarkę magazynową apteki. Realizacja projektu obejmuje cztery filary wymagane w specyfikacji przedmiotu:
1. **Projektowanie koncepcyjne i logiczne:** opracowanie diagramu ERD, słownika danych oraz przeprowadzenie normalizacji struktur do trzeciej postaci normalnej (3NF).
2. **Fizyczna implementacja w SBD (DBMS):** utworzenie struktur tabel w systemie MariaDB za pomocą skryptów DDL (z kluczami głównymi, obcymi i więzami integralności) oraz zasilenie bazy danymi początkowymi (Seed Data).
3. **Logika programistyczna SQL:** implementacja zaawansowanych zapytań analitycznych, procedur składowanych, wyzwalaczy (triggerów) oraz mechanizmu transakcji.
4. **Integracja z aplikacją:** podłączenie bazy danych przez API do aplikacji zewnętrznej realizującej pełny zestaw operacji CRUD (tworzenie, odczyt, aktualizacja, usuwanie).

## 2. Zestawienie wymagań projektowych i produktów cząstkowych
Poniższa tabela syntetyzuje wszystkie wymagania formalne projektu wraz z zaplanowanym sposobem ich realizacji w ramach systemu dla apteki.

| Etap projektu        | Wymagane elementy do oddania                                                                                  | Zakres w projekcie „Apteka”                                                                                                  |
| :------------------- | :------------------------------------------------------------------------------------------------------------ | :--------------------------------------------------------------------------------------------------------------------------- |
| **1. Dokumentacja**  | Diagram ERD (encje, atrybuty, PK, FK, relacje 1:1, 1:N, N:M), słownik danych, uzasadnienie 3NF.               | 6 tabel w systemie: `Klient`, `Pracownik`, `Lek`, `Recepta`, `Zamowienie` oraz tabela łącząca `Zamowienie_Lek`.              |
| **2. Struktura SBD** | Skrypty DDL (`CREATE TABLE`, klucze, ograniczenia) oraz skrypty DML (`INSERT` min. 5--10 rekordów/tabela).    | Skrypty `DBInit.sql` oraz `DBTestData.sql` z ograniczeniami `NOT NULL`, `UNIQUE`, `CHECK` (`cena >= 0`, `dostepne >= 0`).    |
| **3. Logika SQL**    | Min. 4--5 złożonych zapytań (`JOIN`, `GROUP BY`, podzapytania), min. 1 procedura, min. 1 trigger, transakcje. | Plik `queries.sql`: raporty sprzedaży, procedura składania zamówienia z kontrolą stanów i recept, trigger aktualizacji daty. |
| **4. Aplikacja**     | Integracja bazy z interfejsem użytkownika (CRUD), archiwum kodu oraz prezentacja końcowa.                     | Aplikacja webowa pozwalająca na logowanie, przegląd katalogu leków, realizację zamówień (z receptami) oraz podgląd raportów. |

## 3. Architektura bazy danych (Diagram ERD i Słownik Danych)
Model logiczny bazy danych został zaprojektowany zgodnie z zasadami trzeciej postaci normalnej (3NF):
- **1NF (Atomowość):** wszystkie atrybuty w tabelach są niepodzielne (np. dane adresowe w tabeli `Klient` zostały rozbite na osobne kolumny: `ulica`, `numer_domu`, `kod_pocztowy`, `miasto`, `kraj`).
- **2NF (Pełna zależność od klucza głównego):** w tabeli asocjacyjnej `Zamowienie_Lek` o złożonym kluczu głównym `(zamowienie_ID, lek_ID)` atrybut `krotnosc` oraz opcjonalny klucz `recepta_ID` zależą od całego klucza złożonego (konkretnej pozycji w konkretnym zamówieniu), natomiast cena i nazwa leku znajdują się w tabeli `Lek`.
- **3NF (Brak zależności przechodnich):** atrybuty niekluczowe zależą wyłącznie od klucza głównego danej tabeli – dane osobowe klienta, szczegóły recepty oraz parametry preparatu są odseparowane od nagłówka zamówienia (`Zamowienie`).

![WykresERD](WykresERD.png)

**Słownik danych (zgodny ze schematem ERD oraz skryptem `DBInit.sql`):**

| Tabela           | Kolumny i klucze                                                                                                | Typy danych                                                        | Więzy integralności i opis roli                                                                                                        |
| :--------------- | :-------------------------------------------------------------------------------------------------------------- | :----------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------- |
| `Klient`         | `klient_ID` (PK), `login`, `haslo`, `imie`, `nazwisko`, `ulica`, `numer_domu`, `kod_pocztowy`, `miasto`, `kraj` | `INT`, `VARCHAR(32)`, `VARCHAR(255)`, `VARCHAR(64)`, `VARCHAR(20)` | `PK AUTO_INCREMENT`, `login NOT NULL UNIQUE`, `haslo NOT NULL`. Przechowuje dane kont oraz znormalizowane adresy klientów apteki.      |
| `Pracownik`      | `pracownik_ID` (PK), `login`, `haslo`                                                                           | `INT`, `VARCHAR(32)`, `VARCHAR(255)`                               | `PK AUTO_INCREMENT`, `login NOT NULL UNIQUE`, `haslo NOT NULL`. Przechowuje dane uwierzytelniające personelu i farmaceutów.            |
| `Lek`            | `lek_ID` (PK), `nazwa`, `cena`, `opis`, `wymagana_Recepta`, `dostepne`                                          | `INT`, `VARCHAR(32)`, `DECIMAL(10,2)`, `VARCHAR(256)`, `TINYINT`   | `PK AUTO_INCREMENT`, `CHECK (cena >= 0)`, `CHECK (dostepne >= 0)`, `DEFAULT 0`. Katalog preparatów wraz z bieżącym stanem magazynowym. |
| `Recepta`        | `recepta_ID` (PK), `recepta_Numer`, `data_Wystawienia`                                                          | `INT`, `VARCHAR(64)`, `DATE`                                       | `PK AUTO_INCREMENT`, pola `NOT NULL`. Ewidencja recept przypisywanych do pozycji zamówień wymagających uprawnień lekarskich.           |
| `Zamowienie`     | `zamowienie_ID` (PK), `klient_ID` (FK), `utworzony`, `aktualizacja`, `zrealizowane`, `platnosc`                 | `INT`, `DATETIME`, `VARCHAR(256)`                                  | Relacja 1:N z tabelą `Klient`. Śledzi cykl życia zamówienia (`DEFAULT CURRENT_TIMESTAMP`) oraz wybraną metodę płatności.               |
| `Zamowienie_Lek` | `zamowienie_ID` (PK, FK), `lek_ID` (PK, FK), `recepta_ID` (FK), `krotnosc`                                      | `INT`                                                              | Tabela łącząca (realizacja relacji N:M między `Zamowienie` a `Lek`) z odwołaniem do `Recepta`. Domyślna krotność `DEFAULT 1`.          |

## 4. Planowana logika programistyczna po stronie bazy danych (Etap 3)
W ramach pliku `queries.sql` zaplanowano wdrożenie następujących mechanizmów bazodanowych rozwiązujących realne problemy biznesowe apteki:
- **Zaawansowane zapytania analityczne (min. 5 zapytań):**
  1. *Wyszukiwanie najlepszych klientów:* złączenie (`JOIN`) tabel `Klient`, `Zamowienie`, `Zamowienie_Lek` oraz `Lek` z grupowaniem (`GROUP BY`) i obliczeniem łącznej wartości zakupów (`SUM(cena * krotnosc)`).
  2. *Analiza bestsellerów aptecznych:* zestawienie najczęściej zamawianych leków (z podziałem na leki bez recepty i na receptę – `wymagana_Recepta`) wraz z ich udziałem w przychodzie (użycie funkcji agregujących `SUM`, `COUNT`, `AVG`).
  3. *Raport krytycznych stanów magazynowych:* zapytanie z podzapytaniem (`SUBQUERY`) identyfikujące leki, których bieżący stan magazynowy w tabeli `Lek` (`dostepne`) spadł poniżej średniej sprzedaży.
  4. *Zestawienie skuteczności płatności i czasu realizacji:* analiza średniego czasu od `utworzony` do `zrealizowane` w podziale na metody płatności (`platnosc`).
  5. *Identyfikacja leków nierotujących:* wyszukanie preparatów z tabeli `Lek`, które nie pojawiły się w żadnym zrealizowanym zamówieniu (`LEFT JOIN` / `NOT EXISTS`).
- **Procedura składowana i transakcje:** procedura `ZlozZamowienie(klient_ID, lek_ID, recepta_ID, ilosc, platnosc)`, która w ramach transakcji (`BEGIN TRANSACTION`) weryfikuje dostępność leku w kolumnie `Lek.dostepne` oraz sprawdza, czy dla leku z flagą `wymagana_Recepta = 1` podano `recepta_ID`. Następnie dodaje wpis w `Zamowienie` oraz `Zamowienie_Lek`, pomniejsza pole `dostepne` i zatwierdza operację (`COMMIT`) lub wycofuje ją (`ROLLBACK`) w przypadku błędu bądź braku wystarczającej liczby opakowań.
- **Wyzwalacze (Triggers):** trigger `trg_Zamowienie_Aktualizacja` typu `BEFORE UPDATE` na tabeli `Zamowienie` (lub trigger walidujący spójność recept i stanów magazynowych przy dodawaniu pozycji do `Zamowienie_Lek`), który automatycznie dba o integralność danych operacyjnych i znaczników czasu.

## 5. Harmonogram realizacji i kryteria oceny
1. **Faza I (Październik -- Listopad):** zatwierdzenie tematu nr 11 („Apteka”), prezentacja planu działania, odbiór diagramu ERD, słownika danych oraz weryfikacja trzeciej postaci normalnej (3NF).
2. **Faza II (Listopad -- Grudzień):** implementacja skryptów DDL (`DBInit.sql`) oraz wypełnienie wszystkich 6 tabel wysokiej jakości danymi testowymi (`DBTestData.sql`).
3. **Faza III (Grudzień -- Styczeń):** implementacja logiki po stronie bazy danych (`queries.sql`: zapytania analityczne, procedury, triggery, transakcje) oraz integracja z aplikacją CRUD przez API.
4. **Faza IV (Koniec stycznia / Sesja: 03.02 -- 15.02):** złożenie archiwum kodu programu oraz obrona i prezentacja projektu. Ocena końcowa wyliczana jest według wzoru $O = \frac{O_o + O_i}{2}$ (średnia z oceny ogólnej projektu $O_o$ oraz oceny indywidualnego wkładu członka zespołu $O_i$).