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