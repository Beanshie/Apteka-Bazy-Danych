-- 1. METODY DOSTAWY
CREATE TABLE Metoda_Dostawy (
    metoda_dostawy_ID INT AUTO_INCREMENT PRIMARY KEY,
    nazwa             VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
    cena              DECIMAL(10, 2) NOT NULL DEFAULT 0,
    aktywna           TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_metoda_dostawy_cena CHECK (cena >= 0)
);

-- 2. ZUNIFIKOWANI UŻYTKOWNICY
CREATE TABLE Uzytkownik (
    uzytkownik_ID INT AUTO_INCREMENT PRIMARY KEY,
    login         VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
    haslo         VARCHAR(255) NOT NULL,
    email         VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL UNIQUE,
    telefon       VARCHAR(20) NULL,
    imie          VARCHAR(32) NULL,
    nazwisko      VARCHAR(64) NULL,
    ulica         VARCHAR(255) NULL,
    numer_domu    VARCHAR(30) NULL,
    kod_pocztowy  VARCHAR(20) NULL,
    miasto        VARCHAR(64) NULL,
    kraj          VARCHAR(64) NULL DEFAULT 'Polska',
    rola          VARCHAR(16) NOT NULL DEFAULT 'KLIENT',
    aktywny       TINYINT NOT NULL DEFAULT 1,
    utworzony     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_uzytkownik_rola CHECK (rola IN ('KLIENT', 'ADMIN', 'FARMACEUTA', 'PRAKTYKANT'))
);

-- 3. KATALOG LEKÓW
CREATE TABLE Lek (
    lek_ID            INT AUTO_INCREMENT PRIMARY KEY,
    nazwa             VARCHAR(128) NOT NULL,
    kategoria         VARCHAR(64) NULL, 
    cena              DECIMAL(10, 2) NOT NULL,
    opis              TEXT NOT NULL,
    wymagana_Recepta  TINYINT NOT NULL DEFAULT 0,
    dostepne          INT NOT NULL DEFAULT 0,
    producent         VARCHAR(64) NULL,
    substancja_czynna VARCHAR(128) NULL,
    postac            VARCHAR(32) NULL,
    dawka             VARCHAR(32) NULL,
    ean               VARCHAR(13) NULL UNIQUE,
    obraz_url         VARCHAR(255) NULL,
    CONSTRAINT chk_lek_cena CHECK (cena >= 0),
    CONSTRAINT chk_lek_dostepne CHECK (dostepne >= 0)
);

-- 4. RECEPTY
CREATE TABLE Recepta (
    recepta_ID       INT AUTO_INCREMENT PRIMARY KEY,
    recepta_Numer    VARCHAR(64) NOT NULL UNIQUE,
    data_Wystawienia DATE NOT NULL,
    uzytkownik_ID    INT NULL,
    data_waznosci    DATE NOT NULL,
    wykorzystana     TINYINT NOT NULL DEFAULT 0,
    lekarz_nazwisko  VARCHAR(128) NULL,
    lekarz_numer_pwz VARCHAR(16) NULL,
    CONSTRAINT fk_recepta_uzytkownik FOREIGN KEY (uzytkownik_ID) REFERENCES Uzytkownik (uzytkownik_ID) ON DELETE SET NULL
);

-- 5. ZAMÓWIENIA
CREATE TABLE Zamowienie (
    zamowienie_ID        INT AUTO_INCREMENT PRIMARY KEY,
    uzytkownik_ID        INT NOT NULL,
    utworzony            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aktualizacja         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    zrealizowane         DATETIME NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'NOWE',
    suma                 DECIMAL(10, 2) NOT NULL DEFAULT 0,
    metoda_dostawy_ID    INT NULL,
    koszt_dostawy        DECIMAL(10, 2) NOT NULL DEFAULT 0,
    dostawa_odbiorca     VARCHAR(128) NULL,
    dostawa_ulica        VARCHAR(255) NULL,
    dostawa_numer_domu   VARCHAR(30) NULL,
    dostawa_kod_pocztowy VARCHAR(20) NULL,
    dostawa_miasto       VARCHAR(64) NULL,
    dostawa_kraj         VARCHAR(64) NULL,
    platnosc_metoda      VARCHAR(64) NULL,
    platnosc_status      VARCHAR(20) NOT NULL DEFAULT 'OCZEKUJACA',
    CONSTRAINT fk_zamowienie_uzytkownik FOREIGN KEY (uzytkownik_ID) REFERENCES Uzytkownik (uzytkownik_ID),
    CONSTRAINT fk_zamowienie_dostawa FOREIGN KEY (metoda_dostawy_ID) REFERENCES Metoda_Dostawy (metoda_dostawy_ID) ON DELETE SET NULL,
    CONSTRAINT chk_zamowienie_status CHECK (status IN ('NOWE', 'OPLACONE', 'W_REALIZACJI', 'WYSLANE', 'ZREALIZOWANE', 'ANULOWANE')),
    CONSTRAINT chk_zamowienie_suma CHECK (suma >= 0),
    CONSTRAINT chk_platnosc_status CHECK (platnosc_status IN ('OCZEKUJACA', 'OPLACONA', 'NIEUDANA', 'ZWROCONA'))
);

CREATE INDEX idx_zamowienie_uzytkownik ON Zamowienie (uzytkownik_ID);
CREATE INDEX idx_zamowienie_status ON Zamowienie (status);

-- 6. SZCZEGÓŁY ZAMÓWIENIA
CREATE TABLE Zamowienie_Lek (
    zamowienie_ID    INT NOT NULL,
    lek_ID           INT NOT NULL,
    recepta_ID       INT NULL,
    krotnosc         INT NOT NULL DEFAULT 1,
    cena_jednostkowa DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (zamowienie_ID, lek_ID),
    CONSTRAINT fk_zl_zamowienie FOREIGN KEY (zamowienie_ID) REFERENCES Zamowienie (zamowienie_ID) ON DELETE CASCADE,
    CONSTRAINT fk_zl_lek FOREIGN KEY (lek_ID) REFERENCES Lek (lek_ID),
    CONSTRAINT fk_zl_recepta FOREIGN KEY (recepta_ID) REFERENCES Recepta (recepta_ID),
    CONSTRAINT chk_zl_krotnosc CHECK (krotnosc > 0),
    CONSTRAINT chk_zl_cena CHECK (cena_jednostkowa >= 0)
);

-- 7. KOSZYK
CREATE TABLE Koszyk (
    uzytkownik_ID INT NOT NULL,
    lek_ID        INT NOT NULL,
    ilosc         INT NOT NULL DEFAULT 1,
    dodano        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uzytkownik_ID, lek_ID),
    CONSTRAINT fk_koszyk_uzytkownik FOREIGN KEY (uzytkownik_ID) REFERENCES Uzytkownik (uzytkownik_ID) ON DELETE CASCADE,
    CONSTRAINT fk_koszyk_lek FOREIGN KEY (lek_ID) REFERENCES Lek (lek_ID) ON DELETE CASCADE,
    CONSTRAINT chk_koszyk_ilosc CHECK (ilosc > 0)
);

-- 8. TOKENY
CREATE TABLE Refresh_Token (
    token_ID      INT AUTO_INCREMENT PRIMARY KEY,
    uzytkownik_ID INT NOT NULL,
    token_hash    CHAR(64) NOT NULL UNIQUE,
    wygasa        DATETIME NOT NULL,
    uniewazniony  TINYINT NOT NULL DEFAULT 0,
    utworzony     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_uzytkownik FOREIGN KEY (uzytkownik_ID) REFERENCES Uzytkownik (uzytkownik_ID) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_wygasa ON Refresh_Token (wygasa);

-- 9. PARTIE LEKU
CREATE TABLE Partia_Leku (
    partia_ID     INT AUTO_INCREMENT PRIMARY KEY,
    lek_ID        INT NOT NULL,
    numer_serii   VARCHAR(64) NOT NULL,
    data_waznosci DATE NOT NULL,
    ilosc         INT NOT NULL DEFAULT 0,
    przyjeta      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_partia_lek FOREIGN KEY (lek_ID) REFERENCES Lek (lek_ID) ON DELETE CASCADE,
    CONSTRAINT uq_partia_lek_seria UNIQUE (lek_ID, numer_serii),
    CONSTRAINT chk_partia_ilosc CHECK (ilosc >= 0)
);

CREATE INDEX idx_partia_waznosc ON Partia_Leku (data_waznosci);

-- 10. RUCHY MAGAZYNOWE
CREATE TABLE Ruch_Magazynowy (
    ruch_ID       INT AUTO_INCREMENT PRIMARY KEY,
    lek_ID        INT NOT NULL,
    partia_ID     INT NULL,
    uzytkownik_ID INT NULL, 
    zamowienie_ID INT NULL,
    typ           VARCHAR(20) NOT NULL,
    zmiana        INT NOT NULL,
    data          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uwagi         VARCHAR(255) NULL,
    CONSTRAINT fk_ruch_lek FOREIGN KEY (lek_ID) REFERENCES Lek (lek_ID) ON DELETE CASCADE,
    CONSTRAINT fk_ruch_partia FOREIGN KEY (partia_ID) REFERENCES Partia_Leku (partia_ID) ON DELETE SET NULL,
    CONSTRAINT fk_ruch_uzytkownik FOREIGN KEY (uzytkownik_ID) REFERENCES Uzytkownik (uzytkownik_ID) ON DELETE SET NULL,
    CONSTRAINT fk_ruch_zamowienie FOREIGN KEY (zamowienie_ID) REFERENCES Zamowienie (zamowienie_ID) ON DELETE SET NULL,
    CONSTRAINT chk_ruch_typ CHECK (typ IN ('DOSTAWA', 'SPRZEDAZ', 'ZWROT', 'KOREKTA', 'UTYLIZACJA')),
    CONSTRAINT chk_ruch_zmiana CHECK (zmiana <> 0)
);

CREATE INDEX idx_ruch_lek_data ON Ruch_Magazynowy (lek_ID, data);

-- 11. OPINIE
CREATE TABLE Opinia (
    opinia_ID     INT AUTO_INCREMENT PRIMARY KEY,
    lek_ID        INT NOT NULL,
    uzytkownik_ID INT NOT NULL,
    ocena         TINYINT NOT NULL,
    tresc         TEXT NULL,
    utworzona     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_opinia_lek FOREIGN KEY (lek_ID) REFERENCES Lek (lek_ID) ON DELETE CASCADE,
    CONSTRAINT fk_opinia_uzytkownik FOREIGN KEY (uzytkownik_ID) REFERENCES Uzytkownik (uzytkownik_ID) ON DELETE CASCADE,
    CONSTRAINT uq_opinia_uzytkownik_lek UNIQUE (uzytkownik_ID, lek_ID),
    CONSTRAINT chk_opinia_ocena CHECK (ocena BETWEEN 1 AND 5)
);

-- 12. ULUBIONE
CREATE TABLE Ulubione (
    uzytkownik_ID INT NOT NULL,
    lek_ID        INT NOT NULL,
    dodano        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uzytkownik_ID, lek_ID),
    CONSTRAINT fk_ulubione_uzytkownik FOREIGN KEY (uzytkownik_ID) REFERENCES Uzytkownik (uzytkownik_ID) ON DELETE CASCADE,
    CONSTRAINT fk_ulubione_lek FOREIGN KEY (lek_ID) REFERENCES Lek (lek_ID) ON DELETE CASCADE
);