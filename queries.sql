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