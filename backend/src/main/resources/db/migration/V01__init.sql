-- Fresh-database schema sourced from newdb2.sql.

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
