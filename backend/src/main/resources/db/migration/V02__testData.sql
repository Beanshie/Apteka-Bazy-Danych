-- Fresh-database seed data sourced from NewDb.sql.

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
