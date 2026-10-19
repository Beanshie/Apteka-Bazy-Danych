-- 1. Klient
INSERT INTO Klient (login, haslo, imie, nazwisko, ulica, numer_domu, kod_pocztowy, miasto, kraj) VALUES
('jkowalski', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'Jan', 'Kowalski', 'Floriańska', '12/4', '31-021', 'Kraków', 'Polska'),
('anowak', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'Anna', 'Nowak', 'Marszałkowska', '105a', '00-110', 'Warszawa', 'Polska'),
('pwisniewski', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'Piotr', 'Wiśniewski', 'Piotrkowska', '88', '90-004', 'Łódź', 'Polska'),
('mwojcik', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6', 'Maria', 'Wójcik', 'Słoneczna', '5', '80-001', 'Gdańsk', 'Polska');

-- 2. Pracownik
INSERT INTO Pracownik (login, haslo) VALUES
('admin_farmacja', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6'),
('mgr_kowalska', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6'),
('praktykant_jan', '$2y$10$e8T13hRkP1qL.7qK7wS6uO8d1y.wW1v4wZ.8d9a0b1c2d3e4f5g6');

-- 3. Lek
INSERT INTO Lek (nazwa, cena, opis, wymagana_Recepta, dostepne) VALUES
('Paracetamol 500mg', 12.50, 'Lek przeciwbólowy i przeciwgorączkowy.', 0, 150),
('Ibuprom Max 400mg', 18.99, 'Niesteroidowy lek przeciwzapalny.', 0, 80),
('Amotaks 500mg', 34.20, 'Antybiotyk bakteriobójczy (amoksycylina).', 1, 30),
('Rutinoscorbin 150 tab.', 22.00, 'Preparat witaminowy wspierający odporność.', 0, 200),
('Xanax 0.5mg', 45.00, 'Lek uspokajający i przeciwlękowy.', 1, 15),
('Krople Starazolin', 15.30, 'Krople łagodzące podrażnienia oczu.', 0, 45);

-- 4. Recepta
INSERT INTO Recepta (recepta_Numer, data_Wystawienia) VALUES
('REC-2026-0001234', '2026-09-15'),
('REC-2026-0005678', '2026-10-01'),
('REC-2026-0009999', '2026-10-10');

-- 5. Zamowienie
INSERT INTO Zamowienie (klient_ID, utworzony, zrealizowane, platnosc) VALUES
(1, '2026-10-01 10:15:00', '2026-10-02 14:00:00', 'BLIK'),
(2, '2026-10-05 12:30:00', '2026-10-06 09:20:00', 'Karta płatnicza online'),
(3, '2026-10-12 18:45:00', NULL, 'Przelew tradycyjny'),
(1, '2026-10-15 08:00:00', '2026-10-16 11:30:00', 'BLIK'),
(4, '2026-10-18 16:10:00', NULL, 'Płatność przy odbiorze');

-- 6. Zamowienie_Lek
INSERT INTO Zamowienie_Lek (zamowienie_ID, lek_ID, recepta_ID, krotnosc) VALUES
-- Zamówienie 1 (Jan Kowalski: bez recepty)
(1, 1, NULL, 2), -- 2x Paracetamol
(1, 4, NULL, 1), -- 1x Rutinoscorbin

-- Zamówienie 2 (Anna Nowak: wymaga recepty dla Amotaks)
(2, 3, 1, 1),    -- 1x Amotaks (Recepta #1)
(2, 2, NULL, 1), -- 1x Ibuprom Max

-- Zamówienie 3 (Piotr Wiśniewski: w trakcie realizacji)
(3, 5, 2, 2),    -- 2x Xanax (Recepta #2)
(3, 6, NULL, 1), -- 1x Starazolin

-- Zamówienie 4 (Jan Kowalski: ponowne zamówienie)
(4, 2, NULL, 3), -- 3x Ibuprom Max

-- Zamówienie 5 (Maria Wójcik: oczekujące)
(5, 3, 3, 1),    -- 1x Amotaks (Recepta #3)
(5, 1, NULL, 1); -- 1x Paracetamol