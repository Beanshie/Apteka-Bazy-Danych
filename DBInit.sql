CREATE TABLE Klient(
klient_ID INT AUTO_INCREMENT PRIMARY KEY,
login varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
haslo varchar(255) NOT NULL,
imie varchar(32),
nazwisko varchar(64),
ulica varchar(255), 
numer_domu varchar(30),
kod_pocztowy varchar(20), 
miasto varchar(64),
kraj varchar(64)
);
CREATE TABLE Lek(
lek_ID INT AUTO_INCREMENT PRIMARY KEY,
nazwa VARCHAR(32) NOT NULL,
cena DECIMAL(10,2) NOT NULL,
opis VARCHAR(256) NOT NULL,
wymagana_Recepta TINYINT DEFAULT 0,
dostepne INT NOT NULL DEFAULT 0
);
CREATE TABLE Zamowienie(
zamowienie_ID INT AUTO_INCREMENT PRIMARY KEY,
klient_ID INT NOT NULL REFERENCES Klient(klient_ID),
utworzony DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
aktualizacja DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
zrealizowane DATETIME,
platnosc varchar(256)
);
CREATE TABLE Recepta(
recepta_ID INT AUTO_INCREMENT PRIMARY KEY,
recepta_Numer VARCHAR(64) NOT NULL,
data_Wystawienia DATE NOT NULL
);
CREATE TABLE Zamowienie_Lek(
zamowienie_ID INT NOT NULL REFERENCES Zamowienie(zamowienie_ID),
lek_ID INT NOT NULL REFERENCES Lek(lek_ID),
recepta_ID INT REFERENCES Recepta(recepta_ID),
krotnosc INT NOT NULL DEFAULT 1,
PRIMARY KEY (zamowienie_ID,lek_ID)
);
CREATE TABLE Pracownik(
pracownik_ID int AUTO_INCREMENT PRIMARY KEY,
login varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
haslo varchar(255) NOT NULL
);