-- ---------------- 1. Zapytanie - Ranking najlepszych klientów ----------------
SELECT
    k.id_klienta,
    CONCAT(k.imie, ' ', k.nazwisko) AS klient,
    k.email,
    COUNT(z.id_zamowienia) AS liczba_zamowien,
    SUM(z.suma_calkowita) AS laczna_wartosc_zakupow,
    ROUND(AVG(z.suma_calkowita), 2) AS srednia_wartosc_zamowienia
FROM Klient k
INNER JOIN Zamowienie z ON k.id_klienta = z.id_klienta
WHERE z.status_zamowienia <> 'ANULOWANE'
GROUP BY k.id_klienta, k.imie, k.nazwisko, k.email
ORDER BY laczna_wartosc_zakupow DESC;

-- ---------------- 2. Zapytanie - Analiza sprzedaży według kategorii ----------------
SELECT
    kat.nazwa_kategorii,
    COUNT(DISTINCT sz.id_zamowienia) AS liczba_zamowien,
    SUM(sz.ilosc) AS sprzedane_sztuki,
    SUM(sz.ilosc * sz.cena_jednostkowa) AS laczny_przychod
FROM Kategoria kat
INNER JOIN Produkt p ON kat.id_kategorii = p.id_kategorii
INNER JOIN Szczegol_Zamowienia sz ON p.id_produktu = sz.id_produktu
INNER JOIN Zamowienie z ON sz.id_zamowienia = z.id_zamowienia
WHERE z.status_zamowienia <> 'ANULOWANE'
GROUP BY kat.id_kategorii, kat.nazwa_kategorii
HAVING SUM(sz.ilosc * sz.cena_jednostkowa) > 0
ORDER BY laczny_przychod DESC;

-- ---------------- 3. Zapytanie - Produkty droższe niż średnia w swojej kategorii ----------------
SELECT
    p.id_produktu,
    p.nazwa_produktu,
    kat.nazwa_kategorii,
    d.nazwa_firmy AS dostawca,
    p.cena,
    (
        SELECT ROUND(AVG(p2.cena), 2)
        FROM Produkt p2
        WHERE p2.id_kategorii = p.id_kategorii
    ) AS srednia_cena_w_kategorii
FROM Produkt p
INNER JOIN Kategoria kat ON p.id_kategorii = kat.id_kategorii
INNER JOIN Dostawca d ON p.id_dostawcy = d.id_dostawcy
WHERE p.cena > (
    SELECT AVG(p2.cena)
    FROM Produkt p2
    WHERE p2.id_kategorii = p.id_kategorii
)
ORDER BY kat.nazwa_kategorii, p.cena DESC;

-- ---------------- 4. Zapytanie - Raport zamówień z lekami na receptę (Rx) ----------------
SELECT
    z.id_zamowienia,
    z.data_zamowienia,
    CONCAT(k.imie, ' ', k.nazwisko) AS pacjent,
    p.nazwa_produktu AS lek_rx,
    sz.ilosc,
    r.kod_recepty,
    r.status_recepty,
    z.status_zamowienia,
    pl.metoda_platnosci,
    pl.status_platnosci
FROM Zamowienie z
INNER JOIN Klient k ON z.id_klienta = k.id_klienta
INNER JOIN Szczegol_Zamowienia sz ON z.id_zamowienia = sz.id_zamowienia
INNER JOIN Produkt p ON sz.id_produktu = p.id_produktu
LEFT JOIN Recepta r ON sz.id_recepty = r.id_recepty
LEFT JOIN Platnosc pl ON z.id_zamowienia = pl.id_zamowienia
WHERE p.wymaga_recepty = 1
ORDER BY z.data_zamowienia DESC;

-- ---------------- 5. Zapytanie - Produkty, które nie były jeszcze zamówione ----------------
SELECT
    p.id_produktu,
    p.nazwa_produktu,
    kat.nazwa_kategorii,
    p.stan_magazynowy,
    p.cena,
    (p.stan_magazynowy * p.cena) AS wartosc_magazynowa
FROM Produkt p
INNER JOIN Kategoria kat ON p.id_kategorii = kat.id_kategorii
WHERE NOT EXISTS (
    SELECT 1
    FROM Szczegol_Zamowienia sz
    WHERE sz.id_produktu = p.id_produktu
)
ORDER BY wartosc_magazynowa DESC;

-- ---------------- 6. Widoki - Katalog produktów i podsumowanie zamówień ----------------
CREATE OR REPLACE VIEW v_katalog_produktow AS
SELECT
    p.id_produktu,
    p.nazwa_produktu,
    kat.nazwa_kategorii,
    d.nazwa_firmy AS dostawca,
    p.cena,
    p.stan_magazynowy,
    CASE
        WHEN p.wymaga_recepty = 1 THEN 'Na receptę (Rx)'
        ELSE 'Bez recepty (OTC)'
    END AS typ_leku
FROM Produkt p
INNER JOIN Kategoria kat ON p.id_kategorii = kat.id_kategorii
INNER JOIN Dostawca d ON p.id_dostawcy = d.id_dostawcy;

CREATE OR REPLACE VIEW v_podsumowanie_zamowien AS
SELECT
    z.id_zamowienia,
    z.data_zamowienia,
    CONCAT(k.imie, ' ', k.nazwisko) AS klient,
    CONCAT(a.ulica, ' ', a.numer_domu, IFNULL(CONCAT('/', a.numer_mieszkania), ''), ', ', a.kod_pocztowy, ' ', a.miasto) AS adres_dostawy,
    z.status_zamowienia,
    z.suma_calkowita,
    pl.metoda_platnosci,
    pl.status_platnosci
FROM Zamowienie z
INNER JOIN Klient k ON z.id_klienta = k.id_klienta
INNER JOIN Adres_Klienta a ON z.id_adresu = a.id_adresu
LEFT JOIN Platnosc pl ON z.id_zamowienia = pl.id_zamowienia;

-- ---------------- 7. Funkcja składowana - Obliczanie wartości zamówienia ----------------
DROP FUNCTION IF EXISTS fn_oblicz_wartosc_zamowienia;

DELIMITER //
CREATE FUNCTION fn_oblicz_wartosc_zamowienia(p_id_zamowienia INT)
RETURNS DECIMAL(10, 2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_suma DECIMAL(10, 2);

    SELECT IFNULL(SUM(ilosc * cena_jednostkowa), 0.00)
    INTO v_suma
    FROM Szczegol_Zamowienia
    WHERE id_zamowienia = p_id_zamowienia;

    RETURN v_suma;
END //
DELIMITER ;


-- ---------------- 8. Wyzwalacze (Triggers) - Walidacja recepty i aktualizacja sumy ----------------
DROP TRIGGER IF EXISTS trg_sprawdz_recepte_przed_dodaniem;

DELIMITER //
CREATE TRIGGER trg_sprawdz_recepte_przed_dodaniem
BEFORE INSERT ON Szczegol_Zamowienia
FOR EACH ROW
BEGIN
    DECLARE v_wymaga_recepty TINYINT(1);

    SELECT wymaga_recepty
    INTO v_wymaga_recepty
    FROM Produkt
    WHERE id_produktu = NEW.id_produktu;

    IF v_wymaga_recepty = 1 AND NEW.id_recepty IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Błąd: Ten produkt wymaga ważnej recepty (id_recepty nie może być NULL).';
    END IF;
END //
DELIMITER ;

DROP TRIGGER IF EXISTS trg_aktualizuj_sume_zamowienia;

DELIMITER //
CREATE TRIGGER trg_aktualizuj_sume_zamowienia
AFTER INSERT ON Szczegol_Zamowienia
FOR EACH ROW
BEGIN
    UPDATE Zamowienie
    SET suma_calkowita = fn_oblicz_wartosc_zamowienia(NEW.id_zamowienia)
    WHERE id_zamowienia = NEW.id_zamowienia;
END //
DELIMITER ;

-- ---------------- 9. Procedura składowana - Składanie zamówienia z transakcją ----------------
DROP PROCEDURE IF EXISTS sp_zloz_zamowienie;

DELIMITER //
CREATE PROCEDURE sp_zloz_zamowienie(
    IN p_id_klienta INT,
    IN p_id_adresu INT,
    IN p_id_produktu INT,
    IN p_ilosc INT,
    IN p_id_recepty INT,
    IN p_metoda_platnosci VARCHAR(30)
)
BEGIN
    DECLARE v_cena DECIMAL(10, 2);
    DECLARE v_stan INT;
    DECLARE v_nowe_zamowienie_id INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT cena, stan_magazynowy
    INTO v_cena, v_stan
    FROM Produkt
    WHERE id_produktu = p_id_produktu
    FOR UPDATE;

    IF v_stan < p_ilosc THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Błąd: Niewystarczający stan magazynowy produktu.';
    END IF;

    INSERT INTO Zamowienie (id_klienta, id_adresu, status_zamowienia, suma_calkowita)
    VALUES (p_id_klienta, p_id_adresu, 'NOWE', 0.00);

    SET v_nowe_zamowienie_id = LAST_INSERT_ID();

    INSERT INTO Szczegol_Zamowienia (id_zamowienia, id_produktu, id_recepty, ilosc, cena_jednostkowa)
    VALUES (v_nowe_zamowienie_id, p_id_produktu, p_id_recepty, p_ilosc, v_cena);

    UPDATE Produkt
    SET stan_magazynowy = stan_magazynowy - p_ilosc
    WHERE id_produktu = p_id_produktu;

    INSERT INTO Platnosc (id_zamowienia, metoda_platnosci, status_platnosci, data_platnosci)
    VALUES (v_nowe_zamowienie_id, p_metoda_platnosci, 'OCZEKUJACA', NULL);

    COMMIT;
END //
DELIMITER ;


-- ---------------- 10. Transakcje - Demonstracja COMMIT i ROLLBACK ----------------
START TRANSACTION;

UPDATE Produkt
SET stan_magazynowy = stan_magazynowy + 50,
    cena = 13.00
WHERE id_produktu = 1;

COMMIT;

START TRANSACTION;

UPDATE Zamowienie
SET status_zamowienia = 'ANULOWANE'
WHERE id_zamowienia = 4;

ROLLBACK;


-- ---------------- 11. Procedura składowana - Archiwizacja starszych zamówień ----------------
DROP PROCEDURE IF EXISTS sp_archiwizuj_zamowienia;

DELIMITER //
CREATE PROCEDURE sp_archiwizuj_zamowienia(
    IN p_data_graniczna DATE
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    CREATE TABLE IF NOT EXISTS Archiwum_Zamowien LIKE Zamowienie;
    CREATE TABLE IF NOT EXISTS Archiwum_Szczegol_Zamowienia LIKE Szczegol_Zamowienia;

    START TRANSACTION;

    INSERT INTO Archiwum_Zamowien
    SELECT *
    FROM Zamowienie
    WHERE data_zamowienia < p_data_graniczna;

    INSERT INTO Archiwum_Szczegol_Zamowienia
    SELECT sz.*
    FROM Szczegol_Zamowienia sz
    INNER JOIN Zamowienie z ON sz.id_zamowienia = z.id_zamowienia
    WHERE z.data_zamowienia < p_data_graniczna;

    DELETE pl
    FROM Platnosc pl
    INNER JOIN Zamowienie z ON pl.id_zamowienia = z.id_zamowienia
    WHERE z.data_zamowienia < p_data_graniczna;

    DELETE sz
    FROM Szczegol_Zamowienia sz
    INNER JOIN Zamowienie z ON sz.id_zamowienia = z.id_zamowienia
    WHERE z.data_zamowienia < p_data_graniczna;

    DELETE FROM Zamowienie
    WHERE data_zamowienia < p_data_graniczna;

    COMMIT;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_wyczysc_baze;

DELIMITER //
CREATE PROCEDURE sp_wyczysc_baze()
BEGIN
    SET FOREIGN_KEY_CHECKS = 0;

    TRUNCATE TABLE Platnosc;
    TRUNCATE TABLE Szczegol_Zamowienia;
    TRUNCATE TABLE Zamowienie;
    TRUNCATE TABLE Recepta;
    TRUNCATE TABLE Produkt;
    TRUNCATE TABLE Dostawca;
    TRUNCATE TABLE Kategoria;
    TRUNCATE TABLE Adres_Klienta;
    TRUNCATE TABLE Klient;

    SET FOREIGN_KEY_CHECKS = 1;
END //
DELIMITER ;
