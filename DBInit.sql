-- 1. TABELE SŁOWNIKOWE

CREATE TABLE Kategoria (
kategoria_ID INT AUTO_INCREMENT PRIMARY KEY,
nazwa        VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
opis         VARCHAR(255)
);

CREATE TABLE Metoda_Dostawy (
metoda_dostawy_ID INT AUTO_INCREMENT PRIMARY KEY,
nazwa             VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
cena              DECIMAL(10, 2) NOT NULL DEFAULT 0,
aktywna           TINYINT NOT NULL DEFAULT 1,
CONSTRAINT chk_metoda_dostawy_cena CHECK (cena >= 0)
);

-- 2. UŻYTKOWNICY I PRACOWNICY

CREATE TABLE Klient (
klient_ID    INT AUTO_INCREMENT PRIMARY KEY,
login        VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
haslo        VARCHAR(255) NOT NULL,
email        VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL UNIQUE,
telefon      VARCHAR(20) NULL,
imie         VARCHAR(32) NULL,
nazwisko     VARCHAR(64) NULL,
ulica        VARCHAR(255) NULL,
numer_domu   VARCHAR(30) NULL,
kod_pocztowy VARCHAR(20) NULL,
miasto       VARCHAR(64) NULL,
kraj         VARCHAR(64) NULL,
rola         VARCHAR(16) NOT NULL DEFAULT 'KLIENT',
aktywny      TINYINT NOT NULL DEFAULT 1,
utworzony    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT chk_klient_rola CHECK (rola IN ('KLIENT', 'ADMIN'))
);

CREATE TABLE Pracownik (
pracownik_ID INT AUTO_INCREMENT PRIMARY KEY,
login        VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
haslo        VARCHAR(255) NOT NULL,
imie         VARCHAR(32) NULL,
nazwisko     VARCHAR(64) NULL,
rola         VARCHAR(16) NOT NULL DEFAULT 'FARMACEUTA',
aktywny      TINYINT NOT NULL DEFAULT 1,
CONSTRAINT chk_pracownik_rola CHECK (rola IN ('ADMIN', 'FARMACEUTA', 'PRAKTYKANT'))
);

-- 3. KATALOG LEKÓW I RECEPTY

CREATE TABLE Lek (
lek_ID            INT AUTO_INCREMENT PRIMARY KEY,
nazwa             VARCHAR(128) NOT NULL,
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
kategoria_ID      INT NULL,
CONSTRAINT fk_lek_kategoria FOREIGN KEY (kategoria_ID) REFERENCES Kategoria (kategoria_ID) ON DELETE SET NULL,
CONSTRAINT chk_lek_cena CHECK (cena >= 0),
CONSTRAINT chk_lek_dostepne CHECK (dostepne >= 0)
);

CREATE TABLE Recepta (
recepta_ID       INT AUTO_INCREMENT PRIMARY KEY,
recepta_Numer    VARCHAR(64) NOT NULL UNIQUE,
data_Wystawienia DATE NOT NULL,
klient_ID        INT NULL,
data_waznosci    DATE NOT NULL,
wykorzystana     TINYINT NOT NULL DEFAULT 0,
lekarz_nazwisko  VARCHAR(128) NULL,
lekarz_numer_pwz VARCHAR(16) NULL,
CONSTRAINT fk_recepta_klient FOREIGN KEY (klient_ID) REFERENCES Klient (klient_ID) ON DELETE SET NULL
);

-- 4. ZAMÓWIENIA I REALIZACJA

CREATE TABLE Zamowienie (
zamowienie_ID        INT AUTO_INCREMENT PRIMARY KEY,
klient_ID            INT NOT NULL,
utworzony            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
aktualizacja         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
zrealizowane         DATETIME NULL,
platnosc             VARCHAR(256) NULL, -- deprecated: zamienione na tabele Platnosc
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
CONSTRAINT fk_zamowienie_klient FOREIGN KEY (klient_ID) REFERENCES Klient (klient_ID),
CONSTRAINT fk_zamowienie_dostawa FOREIGN KEY (metoda_dostawy_ID) REFERENCES Metoda_Dostawy (metoda_dostawy_ID) ON DELETE SET NULL,
CONSTRAINT chk_zamowienie_status CHECK (status IN ('NOWE', 'OPLACONE', 'W_REALIZACJI', 'WYSLANE', 'ZREALIZOWANE', 'ANULOWANE')),
CONSTRAINT chk_zamowienie_suma CHECK (suma >= 0)
);

CREATE INDEX idx_zamowienie_klient ON Zamowienie (klient_ID);
CREATE INDEX idx_zamowienie_status ON Zamowienie (status);

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

-- 5. ADRESY I KOSZYK

CREATE TABLE Adres (
adres_ID     INT AUTO_INCREMENT PRIMARY KEY,
klient_ID    INT NOT NULL,
etykieta     VARCHAR(32) NULL,
odbiorca     VARCHAR(128) NULL,
ulica        VARCHAR(255) NOT NULL,
numer_domu   VARCHAR(30) NOT NULL,
kod_pocztowy VARCHAR(20) NOT NULL,
miasto       VARCHAR(64) NOT NULL,
kraj         VARCHAR(64) NOT NULL DEFAULT 'Polska',
domyslny     TINYINT NOT NULL DEFAULT 0,
CONSTRAINT fk_adres_klient FOREIGN KEY (klient_ID) REFERENCES Klient (klient_ID) ON DELETE CASCADE
);

CREATE INDEX idx_adres_klient ON Adres (klient_ID);

CREATE TABLE Koszyk (
koszyk_ID    INT AUTO_INCREMENT PRIMARY KEY,
klient_ID    INT NOT NULL UNIQUE,
utworzony    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
aktualizacja DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
CONSTRAINT fk_koszyk_klient FOREIGN KEY (klient_ID) REFERENCES Klient (klient_ID) ON DELETE CASCADE
);

CREATE TABLE Koszyk_Lek (
koszyk_ID INT NOT NULL,
lek_ID    INT NOT NULL,
ilosc     INT NOT NULL DEFAULT 1,
PRIMARY KEY (koszyk_ID, lek_ID),
CONSTRAINT fk_koszyk_lek_koszyk FOREIGN KEY (koszyk_ID) REFERENCES Koszyk (koszyk_ID) ON DELETE CASCADE,
CONSTRAINT fk_koszyk_lek_lek FOREIGN KEY (lek_ID) REFERENCES Lek (lek_ID) ON DELETE CASCADE,
CONSTRAINT chk_koszyk_lek_ilosc CHECK (ilosc > 0)
);

-- 6. PŁATNOŚCI I TOKENY DLA AUTH (JWT)

CREATE TABLE Platnosc (
platnosc_ID              INT AUTO_INCREMENT PRIMARY KEY,
zamowienie_ID            INT NOT NULL,
kwota                    DECIMAL(10, 2) NOT NULL,
metoda                   VARCHAR(64) NOT NULL,
status                   VARCHAR(20) NOT NULL DEFAULT 'OCZEKUJACA',
identyfikator_zewnetrzny VARCHAR(128) NULL,
utworzona                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
oplacona                 DATETIME NULL,
CONSTRAINT fk_platnosc_zamowienie FOREIGN KEY (zamowienie_ID) REFERENCES Zamowienie (zamowienie_ID) ON DELETE CASCADE,
CONSTRAINT chk_platnosc_kwota CHECK (kwota >= 0),
CONSTRAINT chk_platnosc_status CHECK (status IN ('OCZEKUJACA', 'OPLACONA', 'NIEUDANA', 'ZWROCONA'))
);

CREATE INDEX idx_platnosc_zamowienie ON Platnosc (zamowienie_ID);

CREATE TABLE Refresh_Token (
token_ID     INT AUTO_INCREMENT PRIMARY KEY,
klient_ID    INT NULL,
pracownik_ID INT NULL,
token_hash   CHAR(64) NOT NULL UNIQUE,
wygasa       DATETIME NOT NULL,
uniewazniony TINYINT NOT NULL DEFAULT 0,
utworzony    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT fk_refresh_klient FOREIGN KEY (klient_ID) REFERENCES Klient (klient_ID) ON DELETE CASCADE,
CONSTRAINT fk_refresh_pracownik FOREIGN KEY (pracownik_ID) REFERENCES Pracownik (pracownik_ID) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_wygasa ON Refresh_Token (wygasa);

-- 7. PARTIE I RUCHY MAGAZYNOWE

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

CREATE TABLE Ruch_Magazynowy (
ruch_ID       INT AUTO_INCREMENT PRIMARY KEY,
lek_ID        INT NOT NULL,
partia_ID     INT NULL,
pracownik_ID  INT NULL,
zamowienie_ID INT NULL,
typ           VARCHAR(20) NOT NULL,
zmiana        INT NOT NULL,
data          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
uwagi         VARCHAR(255) NULL,
CONSTRAINT fk_ruch_lek FOREIGN KEY (lek_ID) REFERENCES Lek (lek_ID) ON DELETE CASCADE,
CONSTRAINT fk_ruch_partia FOREIGN KEY (partia_ID) REFERENCES Partia_Leku (partia_ID) ON DELETE SET NULL,
CONSTRAINT fk_ruch_pracownik FOREIGN KEY (pracownik_ID) REFERENCES Pracownik (pracownik_ID) ON DELETE SET NULL,
CONSTRAINT fk_ruch_zamowienie FOREIGN KEY (zamowienie_ID) REFERENCES Zamowienie (zamowienie_ID) ON DELETE SET NULL,
CONSTRAINT chk_ruch_typ CHECK (typ IN ('DOSTAWA', 'SPRZEDAZ', 'ZWROT', 'KOREKTA', 'UTYLIZACJA')),
CONSTRAINT chk_ruch_zmiana CHECK (zmiana <> 0)
);

CREATE INDEX idx_ruch_lek_data ON Ruch_Magazynowy (lek_ID, data);

-- 8. OPINIE I ULUBIONE

CREATE TABLE Opinia (
opinia_ID INT AUTO_INCREMENT PRIMARY KEY,
lek_ID    INT NOT NULL,
klient_ID INT NOT NULL,
ocena     TINYINT NOT NULL,
tresc     TEXT NULL,
utworzona DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT fk_opinia_lek FOREIGN KEY (lek_ID) REFERENCES Lek (lek_ID) ON DELETE CASCADE,
CONSTRAINT fk_opinia_klient FOREIGN KEY (klient_ID) REFERENCES Klient (klient_ID) ON DELETE CASCADE,
CONSTRAINT uq_opinia_klient_lek UNIQUE (klient_ID, lek_ID),
CONSTRAINT chk_opinia_ocena CHECK (ocena BETWEEN 1 AND 5)
);

CREATE TABLE Ulubione (
klient_ID INT NOT NULL,
lek_ID    INT NOT NULL,
dodano    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (klient_ID, lek_ID),
CONSTRAINT fk_ulubione_klient FOREIGN KEY (klient_ID) REFERENCES Klient (klient_ID) ON DELETE CASCADE,
CONSTRAINT fk_ulubione_lek FOREIGN KEY (lek_ID) REFERENCES Lek (lek_ID) ON DELETE CASCADE
);
