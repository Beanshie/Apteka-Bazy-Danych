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

-- kategoria (kategorie produktów)
INSERT INTO Kategoria (nazwa_kategorii, opis) VALUES
('Leki przeciwbólowe', 'Preparaty przeciwbólowe, przeciwgorączkowe i przeciwzapalne'),
('Antybiotyki', 'Leki przeciwbakteryjne wydawane wyłącznie na podstawie ważnej e-recepty'),
('Witaminy i Suplementy', 'Preparaty witaminowe, minerały oraz suplementy diety wspierające odporność'),
('Układ pokarmowy', 'Leki osłonowe, probiotyki oraz środki wspomagające trawienie'),
('Okulistyka', 'Krople nawilżające, maści do oczu oraz preparaty łagodzące podrażnienia spojówek'),
('Dermatologia', 'Maści regeneracyjne, kremy przeciwgrzybicze i preparaty pielęgnacyjne');

-- dostawca (hurtownie farmaceutyczne i dystrybutorzy)
INSERT INTO Dostawca (nazwa_firmy, nip, telefon, email) VALUES
('Farmacol S.A.', '6340024921', '322080000', 'zamowienia@farmacol.com.pl'),
('Neuca S.A.', '8790017162', '566629000', 'kontakt@neuca.pl'),
('Pelion S.A.', '7280007802', '422007000', 'dystrybucja@pelion.eu'),
('Salus International Sp. z o.o.', '6340125421', '327885500', 'bok@salusint.com.pl');

-- pracownik (personel apteki: farmaceuci, kierownicy, administratorzy)
-- hasło dla wszystkich kont testowych: "$2y$10$e8t13hrkp1ql.7qk7ws6uo8d1y.ww1v4wz.8d9a0b1c2d3e4f5g6" (hasz bcrypt)
INSERT INTO Pracownik (login, haslo, imie, nazwisko, rola, aktywny) VALUES
('admin_apteka', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'Tomasz', 'Zarządca', 'ADMIN', 1),
('mgr_kowalska', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'Ewa', 'Kowalska', 'FARMACEUTA', 1),
('kierownik_jan', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'Jan', 'Nowakowski', 'KIEROWNIK', 1),
('mgr_adamski', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'Piotr', 'Adamski', 'FARMACEUTA', 1);

-- klient (konta zarejestrowanych pacjentów)
INSERT INTO Klient (imie, nazwisko, email, haslo, telefon, data_rejestracji) VALUES
('Jan', 'Kowalski', 'j.kowalski@example.com', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', '601111222', '2026-09-10 10:15:00'),
('Anna', 'Nowak', 'a.nowak@example.com', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', '602333444', '2026-09-12 14:30:00'),
('Piotr', 'Wiśniewski', 'p.wisniewski@example.com', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', '603555666', '2026-09-15 16:45:00'),
('Maria', 'Wójcik', 'm.wojcik@example.com', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', '604777888', '2026-09-20 11:20:00'),
('Krzysztof', 'Kamiński', 'k.kaminski@example.com', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', '605888999', '2026-09-25 09:00:00'),
('Zofia', 'Lewandowska', 'z.lewandowska@example.com', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', '606123456', '2026-10-01 12:10:00');

-- adres_klienta (książka adresowa powiązana z klientami)
INSERT INTO Adres_Klienta (id_klienta, ulica, numer_domu, numer_mieszkania, kod_pocztowy, miasto) VALUES
(1, 'Floriańska', '12', '4', '31-021', 'Kraków'),
(2, 'Marszałkowska', '105a', NULL, '00-110', 'Warszawa'),
(3, 'Piotrkowska', '88', '12', '90-004', 'Łódź'),
(4, 'Słoneczna', '5', NULL, '80-001', 'Gdańsk'),
(1, 'Karmelicka', '20', '2', '31-131', 'Kraków'),
(5, 'Świdnicka', '14', '7', '50-068', 'Wrocław'),
(6, 'Półwiejska', '32', NULL, '61-888', 'Poznań');

-- produkt (Leki OTC, leki Rx i suplementy z flagami oraz stanami)
INSERT INTO Produkt (id_kategorii, id_dostawcy, nazwa_produktu, cena, stan_magazynowy, wymaga_recepty) VALUES
(1, 1, 'Paracetamol Polfa 500mg', 12.50, 150, 0),
(1, 2, 'Ibuprom Max 400mg', 18.99, 85, 0),
(2, 1, 'Amotaks 500mg (16 kaps.)', 34.20, 30, 1),
(3, 2, 'Rutinoscorbin (150 tabl.)', 22.00, 200, 0),
(2, 3, 'Augmentin 875mg + 125mg', 42.80, 25, 1),
(5, 2, 'Starazolin Krople do oczu', 15.30, 45, 0),
(4, 1, 'Controloc Control 20mg', 19.50, 60, 0),
(3, 4, 'Mag B6 Forte (60 tabl.)', 27.90, 110, 0),
(2, 3, 'Xanax 0.5mg (30 tabl.)', 45.00, 15, 1),
(6, 4, 'Alantan Plus Maść 35g', 14.80, 70, 0);

-- recepta (ewidencja e-recept pacjentów)
INSERT INTO Recepta (id_klienta, kod_recepty, pesel_pacjenta, status_recepty) VALUES
(2, '1234', '92041201234', 'ZREALIZOWANA'),
(3, '5678', '85072005678', 'ZWERYFIKOWANA'),
(4, '9999', '78110309999', 'NOWA'),
(1, '4321', '80011501234', 'ZWERYFIKOWANA'),
(5, '7788', '95092809876', 'NOWA');

-- zamowienie (nagłówki transakcji zakupu)
INSERT INTO Zamowienie (id_klienta, id_adresu, data_zamowienia, status_zamowienia, suma_calkowita) VALUES
(1, 1, '2026-10-01 10:15:00', 'ZREALIZOWANE', 47.00),
(2, 2, '2026-10-05 12:30:00', 'ZREALIZOWANE', 53.19),
(3, 3, '2026-10-12 18:45:00', 'W_REALIZACJI', 85.60),
(4, 4, '2026-10-18 16:10:00', 'NOWE', 34.20),
(5, 6, '2026-10-22 09:20:00', 'OPLACONE', 53.28),
(6, 7, '2026-10-25 14:00:00', 'WYSLANE', 63.50);

-- szczegol_zamowienia (pozycje transakcji ze snapshotem ceny i id_recepty)
INSERT INTO Szczegol_Zamowienia (id_zamowienia, id_produktu, id_recepty, ilosc, cena_jednostkowa) VALUES
-- Zamówienie 1 (Jan Kowalski): 2x Paracetamol (25.00) + 1x Rutinoscorbin (22.00) = 47.00
(1, 1, NULL, 2, 12.50),
(1, 4, NULL, 1, 22.00),

-- Zamówienie 2 (Anna Nowak): 1x Amotaks z receptą #1 (34.20) + 1x Ibuprom Max (18.99) = 53.19
(2, 3, 1, 1, 34.20),
(2, 2, NULL, 1, 18.99),

-- Zamówienie 3 (Piotr Wiśniewski): 2x Augmentin z receptą #2 (85.60) = 85.60
(3, 5, 2, 2, 42.80),

-- Zamówienie 4 (Maria Wójcik): 1x Amotaks z receptą #3 (34.20) = 34.20
(4, 3, 3, 1, 34.20),

-- Zamówienie 5 (Krzysztof Kamiński): 2x Ibuprom Max (37.98) + 1x Starazolin (15.30) = 53.28
(5, 2, NULL, 2, 18.99),
(5, 6, NULL, 1, 15.30),

-- Zamówienie 6 (Zofia Lewandowska): 1x Controloc (19.50) + 2x Rutinoscorbin (44.00) = 63.50
(6, 7, NULL, 1, 19.50),
(6, 4, NULL, 2, 22.00);

-- platnosc (statusy rozliczeń transakcji)
INSERT INTO Platnosc (id_zamowienia, metoda_platnosci, status_platnosci, data_platnosci) VALUES
(1, 'BLIK', 'OPLACONA', '2026-10-01 10:18:00'),
(2, 'Karta płatnicza', 'OPLACONA', '2026-10-05 12:32:00'),
(3, 'Przelew tradycyjny', 'OPLACONA', '2026-10-12 19:00:00'),
(4, 'Płatność przy odbiorze', 'OCZEKUJACA', NULL),
(5, 'BLIK', 'OPLACONA', '2026-10-22 09:22:00'),
(6, 'Karta płatnicza', 'OPLACONA', '2026-10-25 14:05:00');