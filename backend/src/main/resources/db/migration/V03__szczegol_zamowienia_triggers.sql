-- chk_produkt_stan (V01) enforces stan_magazynowy >= 0.
-- Business error codes consumed by OrderItemConstraintErrors:
-- 30001: insufficient stock; 30002: order total exceeds DECIMAL(10,2).
-- 30003: prescription customer mismatch; 30004: invalid prescription status;
-- 30005: required prescription missing; 30006: prescription record missing.
-- 30007: product record missing during an update.
-- 30008: address customer mismatch; 30009: address record missing.
-- Existing items have already reserved stock; do not replay their reservations.
-- Deploy with the service changes while application writes are stopped:
-- older service code would apply the stock changes a second time.

DELIMITER //

CREATE TRIGGER zamowienie_before_insert
BEFORE INSERT ON Zamowienie
FOR EACH ROW
BEGIN
    DECLARE existing_customer INT;
    DECLARE address_customer INT;
    DECLARE missing_reference BOOLEAN DEFAULT FALSE;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET missing_reference = TRUE;

    -- Shared locks stabilize ownership without serializing orders sharing an address.
    SELECT id_klienta INTO existing_customer FROM Klient
    WHERE id_klienta = NEW.id_klienta LOCK IN SHARE MODE;
    -- Let FK/NOT NULL constraints reject missing customers and NULL addresses.
    IF NOT missing_reference AND NEW.id_adresu IS NOT NULL THEN
        SELECT id_klienta INTO address_customer FROM Adres_Klienta
        WHERE id_adresu = NEW.id_adresu LOCK IN SHARE MODE;
        IF missing_reference THEN
            SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30009, MESSAGE_TEXT = 'Nie znaleziono rekordu';
        END IF;
        IF address_customer <> NEW.id_klienta THEN
            SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30008, MESSAGE_TEXT = 'Adres należy do innego klienta';
        END IF;
    END IF;
END//

CREATE TRIGGER zamowienie_before_update
BEFORE UPDATE ON Zamowienie
FOR EACH ROW
BEGIN
    DECLARE existing_customer INT;
    DECLARE address_customer INT;
    DECLARE missing_reference BOOLEAN DEFAULT FALSE;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET missing_reference = TRUE;

    -- Item triggers update totals; only changed ownership/address needs validation.
    IF NOT (NEW.id_klienta <=> OLD.id_klienta) OR NOT (NEW.id_adresu <=> OLD.id_adresu) THEN
        SELECT id_klienta INTO existing_customer FROM Klient
        WHERE id_klienta = NEW.id_klienta LOCK IN SHARE MODE;
        IF NOT missing_reference AND NEW.id_adresu IS NOT NULL THEN
            SELECT id_klienta INTO address_customer FROM Adres_Klienta
            WHERE id_adresu = NEW.id_adresu LOCK IN SHARE MODE;
            IF missing_reference THEN
                SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30009, MESSAGE_TEXT = 'Nie znaleziono rekordu';
            END IF;
            IF address_customer <> NEW.id_klienta THEN
                SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30008, MESSAGE_TEXT = 'Adres należy do innego klienta';
            END IF;
        END IF;
    END IF;
END//

CREATE TRIGGER szczegol_zamowienia_before_insert
BEFORE INSERT ON Szczegol_Zamowienia
FOR EACH ROW
BEGIN
    DECLARE order_customer INT;
    DECLARE product_price DECIMAL(10,2);
    DECLARE requires_prescription BOOLEAN;
    DECLARE prescription_customer INT;
    DECLARE prescription_status VARCHAR(20);
    DECLARE missing_reference BOOLEAN DEFAULT FALSE;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET missing_reference = TRUE;

    -- Keep the validation inputs stable until the item transaction finishes.
    -- Only lock the referenced rows, in order/product/prescription order.
    SELECT id_klienta INTO order_customer
    FROM Zamowienie WHERE id_zamowienia = NEW.id_zamowienia FOR UPDATE;
    SELECT cena, wymaga_recepty INTO product_price, requires_prescription
    FROM Produkt WHERE id_produktu = NEW.id_produktu FOR UPDATE;

    IF NEW.cena_jednostkowa IS NULL THEN
        SET NEW.cena_jednostkowa = product_price;
    END IF;

    -- Missing orders/products are still rejected by the existing FK/NOT NULL constraints.
    IF NOT missing_reference THEN
        IF NEW.id_recepty IS NOT NULL THEN
            SELECT id_klienta, status_recepty INTO prescription_customer, prescription_status
            FROM Recepta WHERE id_recepty = NEW.id_recepty FOR UPDATE;
            IF missing_reference THEN
                SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30006, MESSAGE_TEXT = 'Nie znaleziono rekordu';
            END IF;
            IF prescription_customer <> order_customer THEN
                SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30003, MESSAGE_TEXT = 'Recepta należy do innego klienta';
            END IF;
            IF prescription_status NOT IN ('NOWA', 'ZWERYFIKOWANA') THEN
                SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30004, MESSAGE_TEXT = 'Recepta nie jest ważna';
            END IF;
        ELSEIF requires_prescription THEN
            SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30005, MESSAGE_TEXT = 'Wymagana recepta';
        END IF;
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

CREATE TRIGGER szczegol_zamowienia_before_update
BEFORE UPDATE ON Szczegol_Zamowienia
FOR EACH ROW
BEGIN
    DECLARE order_customer INT;
    DECLARE locked_id INT;
    DECLARE product_price DECIMAL(10,2);
    DECLARE requires_prescription BOOLEAN;
    DECLARE prescription_customer INT;
    DECLARE prescription_status VARCHAR(20);
    DECLARE strict_validation BOOLEAN;
    DECLARE missing_reference BOOLEAN DEFAULT FALSE;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET missing_reference = TRUE;

    -- Lock old/new parents in ID order; read the new parent's customer.
    IF OLD.id_zamowienia < NEW.id_zamowienia THEN
        SELECT id_zamowienia INTO locked_id FROM Zamowienie
        WHERE id_zamowienia = OLD.id_zamowienia FOR UPDATE;
    END IF;
    SELECT id_klienta INTO order_customer FROM Zamowienie
    WHERE id_zamowienia = NEW.id_zamowienia FOR UPDATE;
    IF OLD.id_zamowienia > NEW.id_zamowienia THEN
        SELECT id_zamowienia INTO locked_id FROM Zamowienie
        WHERE id_zamowienia = OLD.id_zamowienia FOR UPDATE;
    END IF;

    -- Lock old/new products in ID order before the stock trigger touches them.
    IF OLD.id_produktu < NEW.id_produktu THEN
        SELECT id_produktu INTO locked_id FROM Produkt
        WHERE id_produktu = OLD.id_produktu FOR UPDATE;
    END IF;
    SELECT cena, wymaga_recepty INTO product_price, requires_prescription
    FROM Produkt WHERE id_produktu = NEW.id_produktu FOR UPDATE;
    IF OLD.id_produktu > NEW.id_produktu THEN
        SELECT id_produktu INTO locked_id FROM Produkt
        WHERE id_produktu = OLD.id_produktu FOR UPDATE;
    END IF;
    IF NEW.id_produktu IS NOT NULL AND product_price IS NULL THEN
        SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30007, MESSAGE_TEXT = 'Nie znaleziono rekordu';
    END IF;

    -- NULL requests the same snapshot policy used by the Java update endpoint.
    -- Explicit non-NULL prices remain available to direct SQL callers.
    IF NEW.cena_jednostkowa IS NULL THEN
        IF NEW.id_produktu <=> OLD.id_produktu THEN
            SET NEW.cena_jednostkowa = OLD.cena_jednostkowa;
        ELSE
            SET NEW.cena_jednostkowa = product_price;
        END IF;
    END IF;

    SET strict_validation = NOT (NEW.id_produktu <=> OLD.id_produktu)
        OR NOT (NEW.id_recepty <=> OLD.id_recepty)
        OR NOT (NEW.id_zamowienia <=> OLD.id_zamowienia)
        OR NEW.ilosc > OLD.ilosc;

    -- Existing constraints handle missing parents and NULL products/quantities.
    IF order_customer IS NOT NULL AND product_price IS NOT NULL THEN
        IF NEW.id_recepty IS NOT NULL THEN
            SET missing_reference = FALSE;
            SELECT id_klienta, status_recepty INTO prescription_customer, prescription_status
            FROM Recepta WHERE id_recepty = NEW.id_recepty FOR UPDATE;
            IF missing_reference THEN
                SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30006, MESSAGE_TEXT = 'Nie znaleziono rekordu';
            END IF;
            IF prescription_customer <> order_customer THEN
                SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30003, MESSAGE_TEXT = 'Recepta należy do innego klienta';
            END IF;
            IF strict_validation AND prescription_status NOT IN ('NOWA', 'ZWERYFIKOWANA') THEN
                SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30004, MESSAGE_TEXT = 'Recepta nie jest ważna';
            END IF;
        ELSEIF strict_validation AND requires_prescription THEN
            SIGNAL SQLSTATE '23000' SET MYSQL_ERRNO = 30005, MESSAGE_TEXT = 'Wymagana recepta';
        END IF;
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
