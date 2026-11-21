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