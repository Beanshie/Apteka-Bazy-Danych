-- ============================================================================
-- SKRYPT ZASILAJĄCY DANYMI TESTOWYMI: DBTestData.sql
-- Baza danych: Apteka Internetowa (schemat DBInit.sql – 12 tabel)
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM Ulubione;
DELETE FROM Opinia;
DELETE FROM Ruch_Magazynowy;
DELETE FROM Partia_Leku;
DELETE FROM Refresh_Token;
DELETE FROM Koszyk;
DELETE FROM Zamowienie_Lek;
DELETE FROM Zamowienie;
DELETE FROM Recepta;
DELETE FROM Lek;
DELETE FROM Uzytkownik;
DELETE FROM Metoda_Dostawy;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. METODY DOSTAWY (Metoda_Dostawy)
INSERT INTO Metoda_Dostawy (metoda_dostawy_ID, nazwa, cena, aktywna) VALUES
(1, 'Kurier DPD', 14.99, 1),
(2, 'Paczkomaty InPost 24/7', 11.99, 1),
(3, 'Kurier DHL Express', 19.99, 1),
(4, 'Odbiór osobisty w aptece', 0.00, 1),
(5, 'Pocztex Kurier 48', 12.50, 0);

-- 2. ZUNIFIKOWANI UŻYTKOWNICY (Uzytkownik - Klienci, Farmaceuci, Personel, Admin)
-- Hasło dla wszystkich kont testowych: "$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6"
INSERT INTO Uzytkownik (uzytkownik_ID, login, haslo, email, telefon, imie, nazwisko, ulica, numer_domu, kod_pocztowy, miasto, kraj, rola, aktywny, utworzony) VALUES
(1, 'admin_farmacja', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'admin@apteka-online.pl', '500100200', 'Tomasz', 'Zarządca', 'Karmelicka', '15', '31-131', 'Kraków', 'Polska', 'ADMIN', 1, '2026-09-01 08:00:00'),
(2, 'mgr_kowalska', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'e.kowalska@apteka-online.pl', '501202303', 'Ewa', 'Kowalska', 'Długa', '42/8', '31-147', 'Kraków', 'Polska', 'FARMACEUTA', 1, '2026-09-05 09:30:00'),
(3, 'praktykant_jan', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'j.nowicki@apteka-online.pl', '502303404', 'Jan', 'Nowicki', 'Czarnowiejska', '12', '30-054', 'Kraków', 'Polska', 'PRAKTYKANT', 1, '2026-09-10 10:00:00'),
(4, 'jkowalski', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'j.kowalski@example.com', '601111222', 'Jan', 'Kowalski', 'Floriańska', '12/4', '31-021', 'Kraków', 'Polska', 'KLIENT', 1, '2026-09-12 14:15:00'),
(5, 'anowak', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'anna.nowak@example.com', '602333444', 'Anna', 'Nowak', 'Marszałkowska', '105a', '00-110', 'Warszawa', 'Polska', 'KLIENT', 1, '2026-09-15 11:20:00'),
(6, 'pwisniewski', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'p.wisniewski@example.com', '603555666', 'Piotr', 'Wiśniewski', 'Piotrkowska', '88', '90-004', 'Łódź', 'Polska', 'KLIENT', 1, '2026-09-20 16:45:00'),
(7, 'mwojcik', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'm.wojcik@example.com', '604777888', 'Maria', 'Wójcik', 'Słoneczna', '5', '80-001', 'Gdańsk', 'Polska', 'KLIENT', 1, '2026-09-25 18:30:00'),
(8, 'kzielinski', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'k.zielinski@example.com', '605999000', 'Krzysztof', 'Zieliński', 'Wrocławska', '24/3', '50-101', 'Wrocław', 'Polska', 'KLIENT', 0, '2026-09-28 12:00:00');

-- 3. KATALOG LEKÓW (Lek)
INSERT INTO Lek (lek_ID, nazwa, kategoria, cena, opis, wymagana_Recepta, dostepne, producent, substancja_czynna, postac, dawka, ean, obraz_url) VALUES
(1, 'Paracetamol Polfa 500mg', 'Przeciwbólowe', 12.50, 'Lek o działaniu przeciwbólowym i przeciwgorączkowym.', 0, 150, 'Polfa Łódź', 'Paracetamolum', 'Tabletki', '500 mg', '5909990010011', '/img/paracetamol.jpg'),
(2, 'Ibuprom Max 400mg', 'Przeciwzapalne', 18.99, 'Niesteroidowy lek przeciwzapalny i przeciwbólowy.', 0, 80, 'US Pharmacia', 'Ibuprofenum', 'Tabletki powlekane', '400 mg', '5909990010028', '/img/ibuprom.jpg'),
(3, 'Amotaks 500mg', 'Antybiotyki', 34.20, 'Antybiotyk bakteriobójczy o szerokim spektrum działania.', 1, 30, 'Polfa Tarchomin', 'Amoxicillinum', 'Kapsułki twarde', '500 mg', '5909990010035', '/img/amotaks.jpg'),
(4, 'Rutinoscorbin 150 tab.', 'Witaminy i odporność', 22.00, 'Preparat witaminowy z rutozydem i witaminą C.', 0, 200, 'GlaxoSmithKline', 'Rutosidum, Acidum ascorbicum', 'Tabletki', '25mg + 100mg', '5909990010042', '/img/rutinoscorbin.jpg'),
(5, 'Xanax 0.5mg', 'Uspokajające', 45.00, 'Lek psychotropowy z grupy benzodiazepin, przeciwlękowy.', 1, 15, 'Pfizer Europe', 'Alprazolamum', 'Tabletki', '0.5 mg', '5909990010059', '/img/xanax.jpg'),
(6, 'Starazolin Krople do oczu', 'Okulistyka', 15.30, 'Krople łagodzące przekrwienie i obrzęk spojówek.', 0, 45, 'Polpharma', 'Tetryzolini hydrochloridum', 'Krople do oczu', '0.5 mg/ml', '5909990010066', '/img/starazolin.jpg'),
(7, 'Cirrus Duo 14 tab.', 'Alergie i katar', 28.50, 'Lek zmniejszający przekrwienie błony śluzowej nosa.', 0, 60, 'UCB Pharma', 'Cetirizini dihydrochloridum, Pseudoephedrini hydrochloridum', 'Tabletki o przedłużonym uwalnianiu', '5mg + 120mg', '5909990010073', '/img/cirrus.jpg'),
(8, 'Augmentin 875mg + 125mg', 'Antybiotyki', 42.80, 'Antybiotyk złożony z amoksycyliny i kwasu klawulanowego.', 1, 25, 'GlaxoSmithKline', 'Amoxicillinum, Acidum clavulanicum', 'Tabletki powlekane', '875mg + 125mg', '5909990010080', '/img/augmentin.jpg');

-- 4. RECEPTY (Recepta)
INSERT INTO Recepta (recepta_ID, recepta_Numer, data_Wystawienia, uzytkownik_ID, data_waznosci, wykorzystana, lekarz_nazwisko, lekarz_numer_pwz) VALUES
(1, 'REC-2026-0001234', '2026-09-15', 5, '2026-10-15', 1, 'dr n. med. Marek Górski', 'PWZ-7821901'),
(2, 'REC-2026-0005678', '2026-10-01', 6, '2026-10-31', 1, 'lek. Katarzyna Lis', 'PWZ-3490123'),
(3, 'REC-2026-0009999', '2026-10-10', 7, '2026-11-10', 0, 'dr Andrzej Adamski', 'PWZ-5623890'),
(4, 'REC-2026-0004321', '2026-10-15', 4, '2026-11-15', 0, 'lek. Magdalena Bąk', 'PWZ-1192834');

-- 5. ZAMÓWIENIA (Zamowienie)
INSERT INTO Zamowienie (zamowienie_ID, uzytkownik_ID, utworzony, aktualizacja, zrealizowane, status, suma, metoda_dostawy_ID, koszt_dostawy, dostawa_odbiorca, dostawa_ulica, dostawa_numer_domu, dostawa_kod_pocztowy, dostawa_miasto, dostawa_kraj, platnosc_metoda, platnosc_status) VALUES
(1, 4, '2026-10-01 10:15:00', '2026-10-02 14:00:00', '2026-10-02 14:00:00', 'ZREALIZOWANE', 47.00, 2, 11.99, 'Jan Kowalski', 'Floriańska', '12/4', '31-021', 'Kraków', 'Polska', 'BLIK', 'OPLACONA'),
(2, 5, '2026-10-05 12:30:00', '2026-10-06 09:20:00', '2026-10-06 09:20:00', 'ZREALIZOWANE', 53.19, 1, 14.99, 'Anna Nowak', 'Marszałkowska', '105a', '00-110', 'Warszawa', 'Polska', 'Karta płatnicza online', 'OPLACONA'),
(3, 6, '2026-10-12 18:45:00', '2026-10-12 18:50:00', NULL, 'W_REALIZACJI', 105.30, 3, 19.99, 'Piotr Wiśniewski', 'Piotrkowska', '88', '90-004', 'Łódź', 'Polska', 'Przelew tradycyjny', 'OPLACONA'),
(4, 4, '2026-10-15 08:00:00', '2026-10-16 11:30:00', '2026-10-16 11:30:00', 'WYSLANE', 56.97, 2, 11.99, 'Jan Kowalski', 'Floriańska', '12/4', '31-021', 'Kraków', 'Polska', 'BLIK', 'OPLACONA'),
(5, 7, '2026-10-18 16:10:00', '2026-10-18 16:10:00', NULL, 'NOWE', 46.70, 4, 0.00, 'Maria Wójcik', 'Słoneczna', '5', '80-001', 'Gdańsk', 'Polska', 'Płatność przy odbiorze', 'OCZEKUJACA');

-- 6. SZCZEGÓŁY ZAMÓWIENIA (Zamowienie_Lek)
INSERT INTO Zamowienie_Lek (zamowienie_ID, lek_ID, recepta_ID, krotnosc, cena_jednostkowa) VALUES
-- Zamówienie 1 (Jan Kowalski: 2x Paracetamol, 1x Rutinoscorbin)
(1, 1, NULL, 2, 12.50),
(1, 4, NULL, 1, 22.00),

-- Zamówienie 2 (Anna Nowak: 1x Amotaks z receptą #1, 1x Ibuprom Max)
(2, 3, 1, 1, 34.20),
(2, 2, NULL, 1, 18.99),

-- Zamówienie 3 (Piotr Wiśniewski: 2x Xanax z receptą #2, 1x Starazolin)
(3, 5, 2, 2, 45.00),
(3, 6, NULL, 1, 15.30),

-- Zamówienie 4 (Jan Kowalski: 3x Ibuprom Max)
(4, 2, NULL, 3, 18.99),

-- Zamówienie 5 (Maria Wójcik: 1x Amotaks z receptą #3, 1x Paracetamol)
(5, 3, 3, 1, 34.20),
(5, 1, NULL, 1, 12.50);

-- 7. KOSZYK (Koszyk)
INSERT INTO Koszyk (uzytkownik_ID, lek_ID, ilosc, dodano) VALUES
(4, 7, 2, '2026-10-20 14:10:00'), -- Jan Kowalski: 2x Cirrus Duo
(5, 4, 1, '2026-10-21 09:15:00'), -- Anna Nowak: 1x Rutinoscorbin
(6, 1, 3, '2026-10-22 18:00:00'), -- Piotr Wiśniewski: 3x Paracetamol
(7, 6, 1, '2026-10-23 11:45:00'); -- Maria Wójcik: 1x Starazolin

-- 8. TOKENY SESYJNE (Refresh_Token)
INSERT INTO Refresh_Token (token_ID, uzytkownik_ID, token_hash, wygasa, uniewazniony, utworzony) VALUES
(1, 1, 'a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef0', '2026-11-04 08:00:00', 0, '2026-10-28 08:00:00'),
(2, 2, 'b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef01', '2026-11-05 09:30:00', 0, '2026-10-29 09:30:00'),
(3, 4, 'c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef012', '2026-11-01 14:15:00', 1, '2026-10-25 14:15:00'),
(4, 5, 'd4e5f67890123456789abcdef0123456789abcdef0123456789abcdef0123', '2026-11-07 11:20:00', 0, '2026-10-31 11:20:00');

-- 9. PARTIE LEKÓW (Partia_Leku)
INSERT INTO Partia_Leku (partia_ID, lek_ID, numer_serii, data_waznosci, ilosc, przyjeta) VALUES
(1, 1, 'LOT-PAR-2026-A1', '2027-06-30', 100, '2026-09-01 07:00:00'),
(2, 1, 'LOT-PAR-2026-A2', '2027-12-31', 50,  '2026-09-15 08:30:00'),
(3, 2, 'LOT-IBU-2026-B1', '2028-03-31', 80,  '2026-09-02 08:00:00'),
(4, 3, 'LOT-AMO-2026-C1', '2027-01-31', 30,  '2026-09-03 09:00:00'),
(5, 4, 'LOT-RUT-2026-D1', '2028-05-31', 200, '2026-09-05 10:00:00'),
(6, 5, 'LOT-XAN-2026-E1', '2026-12-31', 15,  '2026-09-10 11:00:00'),
(7, 6, 'LOT-STA-2026-F1', '2027-08-31', 45,  '2026-09-12 12:00:00');

-- 10. RUCHY MAGAZYNOWE (Ruch_Magazynowy)
INSERT INTO Ruch_Magazynowy (ruch_ID, lek_ID, partia_ID, uzytkownik_ID, zamowienie_ID, typ, zmiana, data, uwagi) VALUES
-- Przyjęcia dostaw początkowych przez farmaceutę / magazyniera
(1, 1, 1, 2, NULL, 'DOSTAWA', 100, '2026-09-01 07:30:00', 'Dostawa od hurtowni Farmacol'),
(2, 1, 2, 2, NULL, 'DOSTAWA', 50,  '2026-09-15 09:00:00', 'Dostawa uzupełniająca Neuca'),
(3, 2, 3, 2, NULL, 'DOSTAWA', 84,  '2026-09-02 08:30:00', 'Dostawa producenta US Pharmacia'),
(4, 3, 4, 2, NULL, 'DOSTAWA', 32,  '2026-09-03 09:30:00', 'Dostawa antybiotyków Polfa'),
(5, 5, 6, 2, NULL, 'DOSTAWA', 17,  '2026-09-10 11:30:00', 'Dostawa leków psychotropowych'),
-- Rozchody ze sprzedaży powiązane z zamówieniami
(6, 1, 1, 2, 1, 'SPRZEDAZ', -2, '2026-10-02 14:00:00', 'Realizacja zamówienia #1'),
(7, 3, 4, 2, 2, 'SPRZEDAZ', -1, '2026-10-06 09:20:00', 'Realizacja zamówienia #2 (recepta)'),
(8, 2, 3, 2, 4, 'SPRZEDAZ', -3, '2026-10-16 11:30:00', 'Realizacja zamówienia #4'),
(9, 5, 6, 2, 3, 'SPRZEDAZ', -2, '2026-10-12 19:00:00', 'Wydanie do zamówienia #3'),
-- Korekta inwentaryzacyjna / uszkodzenie
(10, 2, 3, 1, NULL, 'KOREKTA', -1, '2026-10-17 10:00:00', 'Uszkodzenie opakowania w magazynie');

-- 11. OPINIE KLIENTÓW (Opinia)
INSERT INTO Opinia (opinia_ID, lek_ID, uzytkownik_ID, ocena, tresc, utworzona) VALUES
(1, 1, 4, 5, 'Bardzo skuteczny paracetamol w dobrej cenie. Szybka ulga w gorączce.', '2026-10-03 16:30:00'),
(2, 4, 4, 5, 'Klasyka domowej apteczki na jesienne przeziębienia. Polecam duże opakowanie.', '2026-10-04 11:00:00'),
(3, 2, 5, 4, 'Działa szybko na ból głowy, choć cena mogłaby być nieco niższa.', '2026-10-08 19:20:00'),
(4, 6, 6, 5, 'Krople natychmiastowo usuwają zmęczenie i zaczerwienienie oczu po pracy przy komputerze.', '2026-10-14 21:00:00');

-- 12. ULUBIONE PRODUKTY (Ulubione)
INSERT INTO Ulubione (uzytkownik_ID, lek_ID, dodano) VALUES
(4, 1, '2026-10-02 15:00:00'), -- Jan Kowalski: Paracetamol
(4, 2, '2026-10-02 15:01:00'), -- Jan Kowalski: Ibuprom Max
(5, 4, '2026-10-07 10:30:00'), -- Anna Nowak: Rutinoscorbin
(6, 6, '2026-10-13 12:00:00'), -- Piotr Wiśniewski: Starazolin
(7, 3, '2026-10-19 17:00:00'); -- Maria Wójcik: Amotaks