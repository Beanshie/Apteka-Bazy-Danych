# RAPORT 2: IMPLEMENTACJA FIZYCZNA BAZY DANYCH (DDL) ORAZ ZASILENIE DANYMI TESTOWYMI (DML)

**Skład zespołu projektowego:**
* Karol Walczyszyn (Kierownik projektu / Architekt bazy danych)
* Kacper Uchyła (Programista SQL)
* Łukasz Ziembiński (Backend Developer)
* Aleksandra Sandomierska (Frontend Developer)

**Temat projektu:** System Obsługi Apteki (nr. 11)  
**Etap prac:** Etap II — Wdrożenie fizyczne schematu relacyjnego (DDL) oraz inicjalizacja danych testowych (DML)  
**Środowisko bazodanowe:** MariaDB / MySQL (silnik `InnoDB`, zestaw znaków `utf8mb4`, kolacja `utf8mb4_unicode_ci`)

---

## I. Podsumowanie wykonanych prac w ramach II etapu

W drugim etapie realizacji projektu przeprowadzono translację 10-tabelowego modelu relacyjnego (opracowanego w **Raporcie 1**) na fizyczną strukturę bazy danych w środowisku MariaDB / MySQL oraz przygotowano kompletny zestaw danych testowych weryfikujących poprawność więzów integralności i reguł biznesowych.

Kluczowe rezultaty prac obejmują:

1. **Implementację skryptu strukturalnego DDL (`DBInit.sql`):** Utworzono wszystkie 10 tabel (`Kategoria`, `Dostawca`, `Pracownik`, `Klient`, `Adres_Klienta`, `Produkt`, `Recepta`, `Zamowienie`, `Szczegol_Zamowienia`, `Platnosc`) w oparciu o silnik transakcyjny `InnoDB` oraz pełne wsparcie dla polskich znaków (`utf8mb4_unicode_ci`).
2. **Wdrożenie mechanizmów integralności i bezpieczeństwa danych:** Zdefiniowano klucze główne (`AUTO_INCREMENT`), klucze obce z dobranymi regułami usuwania (`RESTRICT`, `CASCADE`, `SET NULL`), ograniczenia unikalności (`UNIQUE`), ograniczenia dziedzinowe (`CHECK`) oraz dedykowane indeksy (`CREATE INDEX`) optymalizujące zapytania wyszukujące.
3. **Opracowanie skryptu zasilającego DML (`dane_testowe_apteka.sql`):** Przygotowano idempotentny skrypt ładujący **64 spójne rekordy testowe** we wszystkich 10 encjach, pokrywające pełne scenariusze pracy apteki (sprzedaż leków OTC oraz Rx na e-receptę, wieloadresowe profile klientów, różne statusy realizacji zamówień i płatności oraz role personelu).

---

## II. Charakterystyka skryptu strukturalnego DDL (`CREATE TABLE`)

Skrypt inicjalizujący strukturę bazy danych został zaprojektowany z myślą o powtarzalnym, bezbłędnym wdrażaniu schematu w środowiskach developerskich i produkcyjnych:

* **Bezpieczna reinicjalizacja schematu:** Na początku skryptu tymczasowo wyłączana jest weryfikacja kluczy obcych (`SET FOREIGN_KEY_CHECKS = 0;`), a istniejące tabele są usuwane (`DROP TABLE IF EXISTS`) w kolejności odwrotnej do hierarchii zależności (od tabel podrzędnych takich jak `Platnosc` i `Szczegol_Zamowienia` do tabel słownikowych `Dostawca` i `Kategoria`).
* **Precyzja typów danych:** Wszystkie wartości pieniężne (`cena`, `suma_calkowita`, `cena_jednostkowa`) zapisywane są w stałoprzecinkowym typie `DECIMAL(10, 2)`, eliminującym błędy zaokrągleń zmiennoprzecinkowych. Flagi binarne (`aktywny`, `wymaga_recepty`) wykorzystują typ `TINYINT(1)`, a pola na skróty haseł (`haslo`) mają długość `VARCHAR(255)` dostosowaną do algorytmu `bcrypt`.
* **Ochrona logiki biznesowej na poziomie silnika SQL:**
  * Ograniczenia `CHECK` blokują ujemne ceny i stany magazynowe (`>= 0`), zerową lub ujemną liczbę zamawianych sztuk (`ilosc > 0`) oraz wartości spoza zdefiniowanych słowników statusów i ról (`chk_pracownik_rola`, `chk_recepta_status`, `chk_zamowienie_status`, `chk_platnosc_status`).
  * Ograniczenie złożone `uq_zamowienie_produkt UNIQUE (id_zamowienia, id_produktu)` w tabeli `Szczegol_Zamowienia` uniemożliwia zdublowanie tego samego produktu w ramach jednego koszyka zamówienia.
* **Indeksowanie:** Utworzono 8 jawnych indeksów (`idx_adres_klient`, `idx_produkt_kategoria`, `idx_produkt_dostawca`, `idx_recepta_klient`, `idx_zamowienie_klient`, `idx_zamowienie_status`, `idx_szczegol_produkt`, `idx_szczegol_recepta`) przyspieszających operacje `JOIN` oraz filtrowanie zamówień po statusie.

### Kod źródłowy skryptu DDL (`CREATE TABLE`)

```sql
-- ============================================================================
-- PROJEKT RELACYJNEJ BAZY DANYCH: APTEKA INTERNETOWA (10 TABEL)
-- Silnik: MariaDB / MySQL (InnoDB, UTF-8 utf8mb4)
-- Wersja: Nazewnictwo tabel w liczbie pojedynczej (zgodnie ze standardem akademickim)
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS Platnosc;
DROP TABLE IF EXISTS Szczegol_Zamowienia;
DROP TABLE IF EXISTS Zamowienie;
DROP TABLE IF EXISTS Recepta;
DROP TABLE IF EXISTS Produkt;
DROP TABLE IF EXISTS Adres_Klienta;
DROP TABLE IF EXISTS Klient;
DROP TABLE IF EXISTS Pracownik;
DROP TABLE IF EXISTS Dostawca;
DROP TABLE IF EXISTS Kategoria;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 1. KATEGORIA PRODUKTÓW
-- ============================================================================
CREATE TABLE Kategoria (
    id_kategorii     INT AUTO_INCREMENT PRIMARY KEY,
    nazwa_kategorii  VARCHAR(50) NOT NULL UNIQUE,
    opis             TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 2. DOSTAWCA (HURTOWNIE I DYSTRYBUTORZY)
-- ============================================================================
CREATE TABLE Dostawca (
    id_dostawcy INT AUTO_INCREMENT PRIMARY KEY,
    nazwa_firmy VARCHAR(100) NOT NULL,
    nip         VARCHAR(15) NULL UNIQUE,
    telefon     VARCHAR(20) NULL,
    email       VARCHAR(100) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 3. PRACOWNIK APTEKI (FARMACEUTA, KIEROWNIK, ADMIN)
-- ============================================================================
CREATE TABLE Pracownik (
    id_pracownika INT AUTO_INCREMENT PRIMARY KEY,
    login         VARCHAR(50) NOT NULL UNIQUE,
    haslo         VARCHAR(255) NOT NULL,
    imie          VARCHAR(50) NOT NULL,
    nazwisko      VARCHAR(50) NOT NULL,
    rola          VARCHAR(30) NOT NULL DEFAULT 'FARMACEUTA',
    aktywny       TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT chk_pracownik_rola CHECK (rola IN ('FARMACEUTA', 'KIEROWNIK', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 4. KLIENT APTEKI
-- ============================================================================
CREATE TABLE Klient (
    id_klienta       INT AUTO_INCREMENT PRIMARY KEY,
    imie             VARCHAR(50) NOT NULL,
    nazwisko         VARCHAR(50) NOT NULL,
    email            VARCHAR(100) NOT NULL UNIQUE,
    haslo            VARCHAR(255) NOT NULL,
    telefon          VARCHAR(20) NULL,
    data_rejestracji DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 5. ADRES KLIENTA (ZNORMALIZOWANA KSIĄŻKA ADRESOWA)
-- ============================================================================
CREATE TABLE Adres_Klienta (
    id_adresu        INT AUTO_INCREMENT PRIMARY KEY,
    id_klienta       INT NOT NULL,
    ulica            VARCHAR(100) NOT NULL,
    numer_domu       VARCHAR(10) NOT NULL,
    numer_mieszkania VARCHAR(10) NULL,
    kod_pocztowy     VARCHAR(10) NOT NULL,
    miasto           VARCHAR(50) NOT NULL,
    CONSTRAINT fk_adres_klient FOREIGN KEY (id_klienta) 
        REFERENCES Klient (id_klienta) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_adres_klient ON Adres_Klienta (id_klienta);

-- ============================================================================
-- 6. PRODUKT (KATALOG LEKÓW I SUPLEMENTÓW)
-- ============================================================================
CREATE TABLE Produkt (
    id_produktu      INT AUTO_INCREMENT PRIMARY KEY,
    id_kategorii     INT NOT NULL,
    id_dostawcy      INT NOT NULL,
    nazwa_produktu   VARCHAR(100) NOT NULL,
    cena             DECIMAL(10, 2) NOT NULL,
    stan_magazynowy  INT NOT NULL DEFAULT 0,
    wymaga_recepty   TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_produkt_kategoria FOREIGN KEY (id_kategorii) 
        REFERENCES Kategoria (id_kategorii) ON DELETE RESTRICT,
    CONSTRAINT fk_produkt_dostawca FOREIGN KEY (id_dostawcy) 
        REFERENCES Dostawca (id_dostawcy) ON DELETE RESTRICT,
    CONSTRAINT chk_produkt_cena CHECK (cena >= 0),
    CONSTRAINT chk_produkt_stan CHECK (stan_magazynowy >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_produkt_kategoria ON Produkt (id_kategorii);
CREATE INDEX idx_produkt_dostawca ON Produkt (id_dostawcy);

-- ============================================================================
-- 7. RECEPTA LEKARSKA
-- ============================================================================
CREATE TABLE Recepta (
    id_recepty       INT AUTO_INCREMENT PRIMARY KEY,
    id_klienta       INT NOT NULL,
    kod_recepty      VARCHAR(10) NOT NULL,
    pesel_pacjenta   VARCHAR(11) NOT NULL,
    status_recepty   VARCHAR(20) NOT NULL DEFAULT 'NOWA',
    CONSTRAINT fk_recepta_klient FOREIGN KEY (id_klienta) 
        REFERENCES Klient (id_klienta) ON DELETE CASCADE,
    CONSTRAINT chk_recepta_status CHECK (status_recepty IN ('NOWA', 'ZWERYFIKOWANA', 'ZREALIZOWANA', 'ODRZUCONA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_recepta_klient ON Recepta (id_klienta);

-- ============================================================================
-- 8. ZAMÓWIENIE (NAGŁÓWEK TRANSAKCJI)
-- ============================================================================
CREATE TABLE Zamowienie (
    id_zamowienia     INT AUTO_INCREMENT PRIMARY KEY,
    id_klienta        INT NOT NULL,
    id_adresu         INT NOT NULL,
    data_zamowienia   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_zamowienia VARCHAR(20) NOT NULL DEFAULT 'NOWE',
    suma_calkowita    DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_zamowienie_klient FOREIGN KEY (id_klienta) 
        REFERENCES Klient (id_klienta) ON DELETE RESTRICT,
    CONSTRAINT fk_zamowienie_adres FOREIGN KEY (id_adresu) 
        REFERENCES Adres_Klienta (id_adresu) ON DELETE RESTRICT,
    CONSTRAINT chk_zamowienie_status CHECK (status_zamowienia IN ('NOWE', 'OPLACONE', 'W_REALIZACJI', 'WYSLANE', 'ZREALIZOWANE', 'ANULOWANE')),
    CONSTRAINT chk_zamowienie_suma CHECK (suma_calkowita >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_zamowienie_klient ON Zamowienie (id_klienta);
CREATE INDEX idx_zamowienie_status ON Zamowienie (status_zamowienia);

-- ============================================================================
-- 9. SZCZEGÓŁ ZAMÓWIENIA (POZYCJA W KOSZYKU ZAMÓWIENIA)
-- ============================================================================
CREATE TABLE Szczegol_Zamowienia (
    id_szczegolu     INT AUTO_INCREMENT PRIMARY KEY,
    id_zamowienia    INT NOT NULL,
    id_produktu      INT NOT NULL,
    id_recepty       INT NULL,
    ilosc            INT NOT NULL DEFAULT 1,
    cena_jednostkowa DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_szczegol_zamowienie FOREIGN KEY (id_zamowienia) 
        REFERENCES Zamowienie (id_zamowienia) ON DELETE CASCADE,
    CONSTRAINT fk_szczegol_produkt FOREIGN KEY (id_produktu) 
        REFERENCES Produkt (id_produktu) ON DELETE RESTRICT,
    CONSTRAINT fk_szczegol_recepta FOREIGN KEY (id_recepty) 
        REFERENCES Recepta (id_recepty) ON DELETE SET NULL,
    CONSTRAINT chk_szczegol_ilosc CHECK (ilosc > 0),
    CONSTRAINT chk_szczegol_cena CHECK (cena_jednostkowa >= 0),
    CONSTRAINT uq_zamowienie_produkt UNIQUE (id_zamowienia, id_produktu)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_szczegol_produkt ON Szczegol_Zamowienia (id_produktu);
CREATE INDEX idx_szczegol_recepta ON Szczegol_Zamowienia (id_recepty);

-- ============================================================================
-- 10. PŁATNOŚĆ TRANSAKCYJNA
-- ============================================================================
CREATE TABLE Platnosc (
    id_platnosci     INT AUTO_INCREMENT PRIMARY KEY,
    id_zamowienia    INT NOT NULL UNIQUE,
    metoda_platnosci VARCHAR(30) NOT NULL,
    status_platnosci VARCHAR(30) NOT NULL DEFAULT 'OCZEKUJACA',
    data_platnosci   DATETIME NULL,
    CONSTRAINT fk_platnosc_zamowienie FOREIGN KEY (id_zamowienia) 
        REFERENCES Zamowienie (id_zamowienia) ON DELETE CASCADE,
    CONSTRAINT chk_platnosc_status CHECK (status_platnosci IN ('OCZEKUJACA', 'OPLACONA', 'NIEUDANA', 'ZWROCONA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## III. Charakterystyka skryptu zasilającego DML (`INSERT INTO`)

Skrypt `dane_testowe_apteka.sql` zapewnia deterministyczne odtworzenie zestawu testowego (czyszczenie tabel z uwzględnieniem kluczy obcych oraz reset liczników `AUTO_INCREMENT = 1`), a następnie wprowadza dane z zachowaniem pełnej spójności referencyjnej i rachunkowej.

### Zestawienie wolumenu i zakresu danych testowych

| Lp. | Nazwa tabeli | Liczba rekordów | Zakres przetestowanych scenariuszy biznesowych |
| :--- | :--- | :---: | :--- |
| 1 | `Kategoria` | 6 | Pełny przekrój asortymentu aptecznego (leki OTC, antybiotyki Rx, suplementy, okulistyka, dermatologia). |
| 2 | `Dostawca` | 4 | Główni dystrybutorzy hurtowi z unikalnymi numerami NIP i danymi kontaktowymi. |
| 3 | `Pracownik` | 4 | Konta personelu dla wszystkich ról (`ADMIN`, `FARMACEUTA`, `KIEROWNIK`) z zahaszowanymi hasłami `bcrypt`. |
| 4 | `Klient` | 6 | Zarejestrowani pacjenci o zróżnicowanej historii zakupowej i receptowej. |
| 5 | `Adres_Klienta` | 7 | Książka adresowa weryfikująca relację $1:N$ (klient `id_klienta = 1` posiada dwa odrębne adresy w Krakowie; testowanie wartości `NULL` w polu `numer_mieszkania`). |
| 6 | `Produkt` | 10 | Preparaty bez recepty (`wymaga_recepty = 0`, 7 pozycji) oraz leki wydawane z przepisu lekarza (`wymaga_recepty = 1`, 3 pozycje). |
| 7 | `Recepta` | 5 | E-recepty pacjentów w różnych stanach weryfikacji (`NOWA`, `ZWERYFIKOWANA`, `ZREALIZOWANA`). |
| 8 | `Zamowienie` | 6 | Zamówienia reprezentujące kolejne etapy cyklu realizacji (`NOWE`, `OPLACONE`, `W_REALIZACJI`, `WYSLANE`, `ZREALIZOWANE`). |
| 9 | `Szczegol_Zamowienia` | 10 | Pozycje koszyków zamówień (wielopozycyjne i jednopozycyjne), z pełną zgodnością rachunkową $\sum (\text{ilosc} \times \text{cena\_jednostkowa}) = \text{suma\_calkowita}$ oraz przypisaniem `id_recepty` wyłącznie dla leków Rx. |
| 10 | `Platnosc` | 6 | Rozliczenia w relacji $1:1$ dla każdego zamówienia, obejmujące różne kanały płatności (`BLIK`, `Karta płatnicza`, `Przelew tradycyjny`, `Płatność przy odbiorze`) oraz status `OCZEKUJACA` z `data_platnosci = NULL`. |

### Kod źródłowy skryptu DML (`INSERT INTO`)

```sql
-- ============================================================================
-- SKRYPT ZASILAJĄCY DANYMI TESTOWYMI: dane_testowe_apteka.sql
-- Baza danych: Apteka Internetowa (10 tabel, nazwy w liczbie pojedynczej)
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM Platnosc;
DELETE FROM Szczegol_Zamowienia;
DELETE FROM Zamowienie;
DELETE FROM Recepta;
DELETE FROM Produkt;
DELETE FROM Adres_Klienta;
DELETE FROM Klient;
DELETE FROM Pracownik;
DELETE FROM Dostawca;
DELETE FROM Kategoria;

-- Resetowanie liczników auto_increment
ALTER TABLE Platnosc AUTO_INCREMENT = 1;
ALTER TABLE Szczegol_Zamowienia AUTO_INCREMENT = 1;
ALTER TABLE Zamowienie AUTO_INCREMENT = 1;
ALTER TABLE Recepta AUTO_INCREMENT = 1;
ALTER TABLE Produkt AUTO_INCREMENT = 1;
ALTER TABLE Adres_Klienta AUTO_INCREMENT = 1;
ALTER TABLE Klient AUTO_INCREMENT = 1;
ALTER TABLE Pracownik AUTO_INCREMENT = 1;
ALTER TABLE Dostawca AUTO_INCREMENT = 1;
ALTER TABLE Kategoria AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 1. KATEGORIA (Kategorie produktów)
-- ============================================================================
INSERT INTO Kategoria (id_kategorii, nazwa_kategorii, opis) VALUES
(1, 'Leki przeciwbólowe', 'Preparaty przeciwbólowe, przeciwgorączkowe i przeciwzapalne'),
(2, 'Antybiotyki', 'Leki przeciwbakteryjne wydawane wyłącznie na podstawie ważnej e-recepty'),
(3, 'Witaminy i Suplementy', 'Preparaty witaminowe, minerały oraz suplementy diety wspierające odporność'),
(4, 'Układ pokarmowy', 'Leki osłonowe, probiotyki oraz środki wspomagające trawienie'),
(5, 'Okulistyka', 'Krople nawilżające, maści do oczu oraz preparaty łagodzące podrażnienia spojówek'),
(6, 'Dermatologia', 'Maści regeneracyjne, kremy przeciwgrzybicze i preparaty pielęgnacyjne');

-- ============================================================================
-- 2. DOSTAWCA (Hurtownie farmaceutyczne i dystrybutorzy)
-- ============================================================================
INSERT INTO Dostawca (id_dostawcy, nazwa_firmy, nip, telefon, email) VALUES
(1, 'Farmacol S.A.', '6340024921', '322080000', 'zamowienia@farmacol.com.pl'),
(2, 'Neuca S.A.', '8790017162', '566629000', 'kontakt@neuca.pl'),
(3, 'Pelion S.A.', '7280007802', '422007000', 'dystrybucja@pelion.eu'),
(4, 'Salus International Sp. z o.o.', '6340125421', '327885500', 'bok@salusint.com.pl');

-- ============================================================================
-- 3. PRACOWNIK (Personel apteki: farmaceuci, kierownicy, administratorzy)
-- Hasło dla wszystkich kont testowych: "$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6" (hasz bcrypt)
-- ============================================================================
INSERT INTO Pracownik (id_pracownika, login, haslo, imie, nazwisko, rola, aktywny) VALUES
(1, 'admin_apteka', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'Tomasz', 'Zarządca', 'ADMIN', 1),
(2, 'mgr_kowalska', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'Ewa', 'Kowalska', 'FARMACEUTA', 1),
(3, 'kierownik_jan', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'Jan', 'Nowakowski', 'KIEROWNIK', 1),
(4, 'mgr_adamski', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'Piotr', 'Adamski', 'FARMACEUTA', 1);

-- ============================================================================
-- 4. KLIENT (Konta zarejestrowanych pacjentów)
-- ============================================================================
INSERT INTO Klient (id_klienta, imie, nazwisko, email, haslo, telefon, data_rejestracji) VALUES
(1, 'Jan', 'Kowalski', 'j.kowalski@example.com', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', '601111222', '2026-09-10 10:15:00'),
(2, 'Anna', 'Nowak', 'a.nowak@example.com', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', '602333444', '2026-09-12 14:30:00'),
(3, 'Piotr', 'Wiśniewski', 'p.wisniewski@example.com', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', '603555666', '2026-09-15 16:45:00'),
(4, 'Maria', 'Wójcik', 'm.wojcik@example.com', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', '604777888', '2026-09-20 11:20:00'),
(5, 'Krzysztof', 'Kamiński', 'k.kaminski@example.com', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', '605888999', '2026-09-25 09:00:00'),
(6, 'Zofia', 'Lewandowska', 'z.lewandowska@example.com', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', '606123456', '2026-10-01 12:10:00');

-- ============================================================================
-- 5. ADRES_KLIENTA (Książka adresowa powiązana z klientami)
-- ============================================================================
INSERT INTO Adres_Klienta (id_adresu, id_klienta, ulica, numer_domu, numer_mieszkania, kod_pocztowy, miasto) VALUES
(1, 1, 'Floriańska', '12', '4', '31-021', 'Kraków'),
(2, 2, 'Marszałkowska', '105a', NULL, '00-110', 'Warszawa'),
(3, 3, 'Piotrkowska', '88', '12', '90-004', 'Łódź'),
(4, 4, 'Słoneczna', '5', NULL, '80-001', 'Gdańsk'),
(5, 1, 'Karmelicka', '20', '2', '31-131', 'Kraków'),
(6, 5, 'Świdnicka', '14', '7', '50-068', 'Wrocław'),
(7, 6, 'Półwiejska', '32', NULL, '61-888', 'Poznań');

-- ============================================================================
-- 6. PRODUKT (Leki OTC, leki Rx i suplementy z flagami oraz stanami)
-- ============================================================================
INSERT INTO Produkt (id_produktu, id_kategorii, id_dostawcy, nazwa_produktu, cena, stan_magazynowy, wymaga_recepty) VALUES
(1, 1, 1, 'Paracetamol Polfa 500mg', 12.50, 150, 0),
(2, 1, 2, 'Ibuprom Max 400mg', 18.99, 85, 0),
(3, 2, 1, 'Amotaks 500mg (16 kaps.)', 34.20, 30, 1),
(4, 3, 2, 'Rutinoscorbin (150 tabl.)', 22.00, 200, 0),
(5, 2, 3, 'Augmentin 875mg + 125mg', 42.80, 25, 1),
(6, 5, 2, 'Starazolin Krople do oczu', 15.30, 45, 0),
(7, 4, 1, 'Controloc Control 20mg', 19.50, 60, 0),
(8, 3, 4, 'Mag B6 Forte (60 tabl.)', 27.90, 110, 0),
(9, 2, 3, 'Xanax 0.5mg (30 tabl.)', 45.00, 15, 1),
(10, 6, 4, 'Alantan Plus Maść 35g', 14.80, 70, 0);

-- ============================================================================
-- 7. RECEPTA (Ewidencja e-recept pacjentów)
-- ============================================================================
INSERT INTO Recepta (id_recepty, id_klienta, kod_recepty, pesel_pacjenta, status_recepty) VALUES
(1, 2, '1234', '92041201234', 'ZREALIZOWANA'),
(2, 3, '5678', '85072005678', 'ZWERYFIKOWANA'),
(3, 4, '9999', '78110309999', 'NOWA'),
(4, 1, '4321', '80011501234', 'ZWERYFIKOWANA'),
(5, 5, '7788', '95092809876', 'NOWA');

-- ============================================================================
-- 8. ZAMOWIENIE (Nagłówki transakcji zakupu)
-- ============================================================================
INSERT INTO Zamowienie (id_zamowienia, id_klienta, id_adresu, data_zamowienia, status_zamowienia, suma_calkowita) VALUES
(1, 1, 1, '2026-10-01 10:15:00', 'ZREALIZOWANE', 47.00),
(2, 2, 2, '2026-10-05 12:30:00', 'ZREALIZOWANE', 53.19),
(3, 3, 3, '2026-10-12 18:45:00', 'W_REALIZACJI', 85.60),
(4, 4, 4, '2026-10-18 16:10:00', 'NOWE', 34.20),
(5, 5, 6, '2026-10-22 09:20:00', 'OPLACONE', 53.28),
(6, 6, 7, '2026-10-25 14:00:00', 'WYSLANE', 63.50);

-- ============================================================================
-- 9. SZCZEGOL_ZAMOWIENIA (Pozycje transakcji ze snapshotem ceny i id_recepty)
-- ============================================================================
INSERT INTO Szczegol_Zamowienia (id_szczegolu, id_zamowienia, id_produktu, id_recepty, ilosc, cena_jednostkowa) VALUES
-- Zamówienie 1 (Jan Kowalski): 2x Paracetamol (25.00) + 1x Rutinoscorbin (22.00) = 47.00
(1, 1, 1, NULL, 2, 12.50),
(2, 1, 4, NULL, 1, 22.00),

-- Zamówienie 2 (Anna Nowak): 1x Amotaks z receptą #1 (34.20) + 1x Ibuprom Max (18.99) = 53.19
(3, 2, 3, 1, 1, 34.20),
(4, 2, 2, NULL, 1, 18.99),

-- Zamówienie 3 (Piotr Wiśniewski): 2x Augmentin z receptą #2 (85.60) = 85.60
(5, 3, 5, 2, 2, 42.80),

-- Zamówienie 4 (Maria Wójcik): 1x Amotaks z receptą #3 (34.20) = 34.20
(6, 4, 3, 3, 1, 34.20),

-- Zamówienie 5 (Krzysztof Kamiński): 2x Ibuprom Max (37.98) + 1x Starazolin (15.30) = 53.28
(7, 5, 2, NULL, 2, 18.99),
(8, 5, 6, NULL, 1, 15.30),

-- Zamówienie 6 (Zofia Lewandowska): 1x Controloc (19.50) + 2x Rutinoscorbin (44.00) = 63.50
(9, 6, 7, NULL, 1, 19.50),
(10, 6, 4, NULL, 2, 22.00);

-- ============================================================================
-- 10. PLATNOSC (Statusy rozliczeń transakcji)
-- ============================================================================
INSERT INTO Platnosc (id_platnosci, id_zamowienia, metoda_platnosci, status_platnosci, data_platnosci) VALUES
(1, 1, 'BLIK', 'OPLACONA', '2026-10-01 10:18:00'),
(2, 2, 'Karta płatnicza', 'OPLACONA', '2026-10-05 12:32:00'),
(3, 3, 'Przelew tradycyjny', 'OPLACONA', '2026-10-12 19:00:00'),
(4, 4, 'Płatność przy odbiorze', 'OCZEKUJACA', NULL),
(5, 5, 'BLIK', 'OPLACONA', '2026-10-22 09:22:00'),
(6, 6, 'Karta płatnicza', 'OPLACONA', '2026-10-25 14:05:00');
```

---

## IV. Weryfikacja poprawności wdrożenia

Oba skrypty zostały uruchomione sekwencyjnie na serwerze MariaDB / MySQL z silnikiem `InnoDB`. Proces tworzenia schematu oraz ładowania 64 rekordów testowych zakończył się pełnym powodzeniem bez ostrzeżeń ani błędów naruszenia integralności referencyjnej (`FOREIGN KEY`) czy ograniczeń dziedzinowych (`CHECK`), potwierdzając gotowość bazy danych do integracji z warstwą backendową aplikacji.