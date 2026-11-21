-- chk_produkt_stan (V01) enforces stan_magazynowy >= 0.
-- Business error codes consumed by OrderItemConstraintErrors:
-- 30001: insufficient stock; 30002: order total exceeds DECIMAL(10,2).
-- Existing items have already reserved stock; do not replay their reservations.
-- Deploy with the service changes while application writes are stopped:
-- older service code would apply the stock changes a second time.

DELIMITER //

CREATE TRIGGER szczegol_zamowienia_before_insert
BEFORE INSERT ON Szczegol_Zamowienia
FOR EACH ROW
BEGIN
    IF NEW.cena_jednostkowa IS NULL THEN
        SET NEW.cena_jednostkowa = (
            SELECT cena FROM Produkt WHERE id_produktu = NEW.id_produktu
        );
    END IF;
END//

CREATE TRIGGER szczegol_zamowienia_after_insert
AFTER INSERT ON Szczegol_Zamowienia
FOR EACH ROW
BEGIN
    DECLARE active_order BOOLEAN;

    BEGIN
        DECLARE EXIT HANDLER FOR 1264
            SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30002, MESSAGE_TEXT = 'Zbyt duża suma';
        UPDATE Zamowienie
        SET suma_calkowita = suma_calkowita + NEW.ilosc * NEW.cena_jednostkowa
        WHERE id_zamowienia = NEW.id_zamowienia;
    END;

    SELECT status_zamowienia <> 'ANULOWANE' INTO active_order
    FROM Zamowienie WHERE id_zamowienia = NEW.id_zamowienia FOR UPDATE;

    IF active_order THEN
        BEGIN
            DECLARE EXIT HANDLER FOR 4025
                SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30001, MESSAGE_TEXT = 'Brak produktu na stanie';
            UPDATE Produkt
            SET stan_magazynowy = stan_magazynowy - NEW.ilosc
            WHERE id_produktu = NEW.id_produktu;
        END;
    END IF;
END//

CREATE TRIGGER szczegol_zamowienia_after_update
AFTER UPDATE ON Szczegol_Zamowienia
FOR EACH ROW
BEGIN
    DECLARE old_active BOOLEAN;
    DECLARE new_active BOOLEAN;

    -- Lock both parents in ID order when a direct SQL update moves an item.
    IF OLD.id_zamowienia <= NEW.id_zamowienia THEN
        SELECT status_zamowienia <> 'ANULOWANE' INTO old_active
        FROM Zamowienie WHERE id_zamowienia = OLD.id_zamowienia FOR UPDATE;
        IF OLD.id_zamowienia = NEW.id_zamowienia THEN
            SET new_active = old_active;
        ELSE
            SELECT status_zamowienia <> 'ANULOWANE' INTO new_active
            FROM Zamowienie WHERE id_zamowienia = NEW.id_zamowienia FOR UPDATE;
        END IF;
    ELSE
        SELECT status_zamowienia <> 'ANULOWANE' INTO new_active
        FROM Zamowienie WHERE id_zamowienia = NEW.id_zamowienia FOR UPDATE;
        SELECT status_zamowienia <> 'ANULOWANE' INTO old_active
        FROM Zamowienie WHERE id_zamowienia = OLD.id_zamowienia FOR UPDATE;
    END IF;

    BEGIN
        DECLARE EXIT HANDLER FOR 1264
            SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30002, MESSAGE_TEXT = 'Zbyt duża suma';
        IF OLD.id_zamowienia = NEW.id_zamowienia THEN
            UPDATE Zamowienie
            SET suma_calkowita = suma_calkowita
                + (NEW.ilosc * NEW.cena_jednostkowa - OLD.ilosc * OLD.cena_jednostkowa)
            WHERE id_zamowienia = NEW.id_zamowienia;
        ELSE
            UPDATE Zamowienie
            SET suma_calkowita = suma_calkowita - OLD.ilosc * OLD.cena_jednostkowa
            WHERE id_zamowienia = OLD.id_zamowienia;
            UPDATE Zamowienie
            SET suma_calkowita = suma_calkowita + NEW.ilosc * NEW.cena_jednostkowa
            WHERE id_zamowienia = NEW.id_zamowienia;
        END IF;
    END;

    BEGIN
        DECLARE EXIT HANDLER FOR 4025
            SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30001, MESSAGE_TEXT = 'Brak produktu na stanie';
        -- One net adjustment per product, including when the product stays the same.
        UPDATE Produkt
        SET stan_magazynowy = stan_magazynowy
            + IF(id_produktu = OLD.id_produktu AND old_active, OLD.ilosc, 0)
            - IF(id_produktu = NEW.id_produktu AND new_active, NEW.ilosc, 0)
        WHERE id_produktu IN (OLD.id_produktu, NEW.id_produktu)
        ORDER BY id_produktu;
    END;
END//

CREATE TRIGGER szczegol_zamowienia_after_delete
AFTER DELETE ON Szczegol_Zamowienia
FOR EACH ROW
BEGIN
    DECLARE active_order BOOLEAN;

    UPDATE Zamowienie
    SET suma_calkowita = suma_calkowita - OLD.ilosc * OLD.cena_jednostkowa
    WHERE id_zamowienia = OLD.id_zamowienia;

    SELECT status_zamowienia <> 'ANULOWANE' INTO active_order
    FROM Zamowienie WHERE id_zamowienia = OLD.id_zamowienia FOR UPDATE;

    IF active_order THEN
        UPDATE Produkt
        SET stan_magazynowy = stan_magazynowy + OLD.ilosc
        WHERE id_produktu = OLD.id_produktu;
    END IF;
END//

DELIMITER ;
