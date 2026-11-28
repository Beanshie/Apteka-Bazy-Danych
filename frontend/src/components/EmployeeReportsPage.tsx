import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { zamowieniaService } from '../services/zamowieniaService';
import { produktyService } from '../services/produktyService';
import { klienciService } from '../services/klienciService';
import { receptyService } from '../services/receptyService';
import { kategorieService } from '../services/kategorieService';
import { platnosciService } from '../services/platnosciService';
import type { StatusZamowienia, Zamowienie } from '../types/zamowienie';
import type { Produkt } from '../types/produkt';
import type { Klient } from '../types/klient';
import type { Recepta, StatusRecepty } from '../types/recepta';
import type { Kategoria } from '../types/kategoria';
import type { Platnosc, StatusPlatnosci } from '../types/platnosc';
import {
  OrderStatusBadge,
  PlatnoscStatusBadge,
  ReceptaStatusBadge,
  StockBadge,
} from './Badges';
import { formatDatePl, zl } from '../utils/format';

const ORDER_STATUSES: StatusZamowienia[] = [
  'NOWE',
  'OPLACONE',
  'W_REALIZACJI',
  'WYSLANE',
  'ZREALIZOWANE',
  'ANULOWANE',
];

const RECEPTA_STATUSES: StatusRecepty[] = [
  'NOWA',
  'ZWERYFIKOWANA',
  'ZREALIZOWANA',
  'ODRZUCONA',
];

const PLATNOSC_STATUSES: StatusPlatnosci[] = [
  'OCZEKUJACA',
  'OPLACONA',
  'NIEUDANA',
  'ZWROCONA',
];

export const EmployeeReportsPage: React.FC = () => {
  const [zamowienia, setZamowienia] = useState<Zamowienie[]>([]);
  const [produkty, setProdukty] = useState<Produkt[]>([]);
  const [klienci, setKlienci] = useState<Klient[]>([]);
  const [recepty, setRecepty] = useState<Recepta[]>([]);
  const [kategorie, setKategorie] = useState<Kategoria[]>([]);
  const [platnosci, setPlatnosci] = useState<Platnosc[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [updatedAt, setUpdatedAt] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [
        zamowieniaData,
        produktyData,
        klienciData,
        receptyData,
        kategorieData,
        platnosciData,
      ] = await Promise.all([
        zamowieniaService.getAll(),
        produktyService.getAll(),
        klienciService.getAll(),
        receptyService.getAll(),
        kategorieService.getAll(),
        platnosciService.getAll(),
      ]);

      setZamowienia(zamowieniaData);
      setProdukty(produktyData);
      setKlienci(klienciData);
      setRecepty(receptyData);
      setKategorie(kategorieData);
      setPlatnosci(platnosciData);
      setUpdatedAt(new Date().toISOString());
    } catch (err) {
      console.error(err);
      setError('Nie udało się wygenerować raportu. Sprawdź połączenie z API.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const kategorieMap = useMemo(() => {
    const map: Record<number, Kategoria> = {};
    for (const k of kategorie) map[k.id_kategorii] = k;
    return map;
  }, [kategorie]);

  const klienciMap = useMemo(() => {
    const map: Record<number, Klient> = {};
    for (const k of klienci) map[k.id_klienta] = k;
    return map;
  }, [klienci]);

  // Sekcja 1: KPI
  const kpi = useMemo(() => {
    const activeOrders = zamowienia.filter(
      (z) => z.status_zamowienia !== 'ANULOWANE',
    );
    const totalRevenue = activeOrders.reduce(
      (sum, z) => sum + Number(z.suma_calkowita ?? 0),
      0,
    );
    const completedOrders = zamowienia.filter(
      (z) => z.status_zamowienia === 'ZREALIZOWANE',
    ).length;
    const inProgressOrders = zamowienia.filter((z) =>
      ['NOWE', 'OPLACONE', 'W_REALIZACJI', 'WYSLANE'].includes(
        z.status_zamowienia,
      ),
    ).length;

    const lowStockCount = produkty.filter(
      (p) => (p.stan_magazynowy ?? 0) < 10,
    ).length;
    const outOfStockCount = produkty.filter(
      (p) => (p.stan_magazynowy ?? 0) === 0,
    ).length;

    const pendingRecepty = recepty.filter(
      (r) => r.status_recepty === 'NOWA',
    ).length;

    const paidPaymentsCount = platnosci.filter(
      (p) => p.status_platnosci === 'OPLACONA',
    ).length;

    return {
      totalRevenue,
      totalOrders: zamowienia.length,
      completedOrders,
      inProgressOrders,
      lowStockCount,
      outOfStockCount,
      pendingRecepty,
      totalRecepty: recepty.length,
      paidPaymentsCount,
    };
  }, [zamowienia, produkty, recepty, platnosci]);

  // Sekcja 2: Przychód i liczba zamówień wg statusu
  const ordersByStatus = useMemo(() => {
    const totalCount = zamowienia.length;
    return ORDER_STATUSES.map((status) => {
      const matching = zamowienia.filter((z) => z.status_zamowienia === status);
      const count = matching.length;
      const sum = matching.reduce(
        (acc, z) => acc + Number(z.suma_calkowita ?? 0),
        0,
      );
      const share = totalCount > 0 ? (count / totalCount) * 100 : 0;
      return { status, count, sum, share };
    });
  }, [zamowienia]);

  // Sekcja 3: Alert niskiego stanu magazynowego (< 10)
  const lowStockProducts = useMemo(() => {
    return produkty
      .filter((p) => (p.stan_magazynowy ?? 0) < 10)
      .sort((a, b) => (a.stan_magazynowy ?? 0) - (b.stan_magazynowy ?? 0));
  }, [produkty]);

  // Sekcja 4: Top klienci wg wartości zamówień (bez ANULOWANE)
  const topClients = useMemo(() => {
    const totals = new Map<number, { count: number; total: number }>();
    for (const z of zamowienia) {
      if (z.status_zamowienia === 'ANULOWANE') continue;
      const entry = totals.get(z.id_klienta) ?? { count: 0, total: 0 };
      entry.count += 1;
      entry.total += Number(z.suma_calkowita ?? 0);
      totals.set(z.id_klienta, entry);
    }

    return Array.from(totals.entries())
      .map(([idKlienta, stats]) => ({
        idKlienta,
        klient: klienciMap[idKlienta] ?? null,
        count: stats.count,
        total: stats.total,
      }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 10);
  }, [zamowienia, klienciMap]);

  // Sekcja 5: Recepty i płatności wg statusu
  const receptyByStatus = useMemo(() => {
    const total = recepty.length;
    return RECEPTA_STATUSES.map((status) => {
      const count = recepty.filter((r) => r.status_recepty === status).length;
      const share = total > 0 ? (count / total) * 100 : 0;
      return { status, count, share };
    });
  }, [recepty]);

  const platnosciByStatus = useMemo(() => {
    const total = platnosci.length;
    return PLATNOSC_STATUSES.map((status) => {
      const count = platnosci.filter((p) => p.status_platnosci === status).length;
      const share = total > 0 ? (count / total) * 100 : 0;
      return { status, count, share };
    });
  }, [platnosci]);

  return (
    <div className="space-y-6">
      {/* Nagłówek */}
      <div className="bg-white rounded-2xl p-4 sm:p-6 shadow-sm border border-slate-200/80 flex flex-col sm:flex-row gap-4 justify-between items-start sm:items-center">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Raporty analityczne</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Zestawienie sprzedaży, magazynu, klientów i recept
            {updatedAt ? ` • zaktualizowano: ${formatDatePl(updatedAt, 'short')}` : ''}
          </p>
        </div>

        <button
          type="button"
          onClick={load}
          disabled={loading}
          className="inline-flex items-center gap-2 px-4 py-2 bg-teal-600 hover:bg-teal-700 disabled:opacity-60 text-white font-medium text-sm rounded-xl transition-all"
        >
          {loading ? 'Odświeżanie...' : 'Odśwież raport'}
        </button>
      </div>

      {error && (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-4 text-sm text-rose-800 flex items-center justify-between gap-4">
          <span>{error}</span>
          <button
            type="button"
            onClick={load}
            className="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold"
          >
            Spróbuj ponownie
          </button>
        </div>
      )}

      {loading ? (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 py-20 flex flex-col items-center justify-center gap-3">
          <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
          <p className="text-sm font-medium text-slate-600">
            Generowanie zestawień raportowych...
          </p>
        </div>
      ) : (
        <>
          {/* Sekcja 1: Karty KPI */}
          <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-5 space-y-1">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Łączny przychód
              </p>
              <p className="text-2xl font-bold text-teal-700">
                {zl(kpi.totalRevenue)}
              </p>
              <p className="text-xs text-slate-500">
                Bez anulowanych • opłaconych płatności: {kpi.paidPaymentsCount}
              </p>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-5 space-y-1">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Liczba zamówień
              </p>
              <p className="text-2xl font-bold text-slate-900">{kpi.totalOrders}</p>
              <p className="text-xs text-slate-500">
                W toku: {kpi.inProgressOrders} • Zrealizowane: {kpi.completedOrders}
              </p>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-5 space-y-1">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Produkty do uzupełnienia
              </p>
              <p className="text-2xl font-bold text-amber-600">
                {kpi.lowStockCount}
              </p>
              <p className="text-xs text-slate-500">
                Stan &lt; 10 szt. (w tym brak na stanie: {kpi.outOfStockCount})
              </p>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-5 space-y-1">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Recepty do weryfikacji
              </p>
              <p className="text-2xl font-bold text-sky-700">
                {kpi.pendingRecepty}
              </p>
              <p className="text-xs text-slate-500">
                Status NOWA • łącznie recept: {kpi.totalRecepty}
              </p>
            </div>
          </section>

          {/* Sekcja 2: Przychód i liczba zamówień wg statusu */}
          <section className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
              <h3 className="text-base font-bold text-slate-900">
                Przychód i liczba zamówień wg statusu
              </h3>
              <Link
                to="/panel/zamowienia"
                className="text-xs font-semibold text-teal-700 hover:underline"
              >
                Panel zamówień →
              </Link>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold uppercase tracking-wider text-slate-500">
                    <th className="py-3 px-6">Status</th>
                    <th className="py-3 px-6 text-right">Liczba zamówień</th>
                    <th className="py-3 px-6 text-right">Łączna wartość</th>
                    <th className="py-3 px-6 w-56">Udział procentowy</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-sm">
                  {ordersByStatus.map((row) => (
                    <tr key={row.status} className="hover:bg-slate-50/60">
                      <td className="py-3.5 px-6 whitespace-nowrap">
                        <OrderStatusBadge status={row.status} />
                      </td>
                      <td className="py-3.5 px-6 text-right font-medium text-slate-900">
                        {row.count}
                      </td>
                      <td className="py-3.5 px-6 text-right font-semibold text-slate-900 whitespace-nowrap">
                        {zl(row.sum)}
                      </td>
                      <td className="py-3.5 px-6">
                        <div className="flex items-center gap-3">
                          <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-teal-600 rounded-full"
                              style={{ width: `${row.share.toFixed(1)}%` }}
                            />
                          </div>
                          <span className="text-xs font-mono text-slate-600 w-12 text-right">
                            {row.share.toFixed(1)}%
                          </span>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          {/* Sekcja 3: Alert niskiego stanu magazynowego */}
          <section className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
              <h3 className="text-base font-bold text-slate-900">
                Alert niskiego stanu magazynowego (&lt; 10 szt.)
              </h3>
              <Link
                to="/panel/produkty"
                className="text-xs font-semibold text-teal-700 hover:underline"
              >
                Zarządzaj magazynem →
              </Link>
            </div>

            {lowStockProducts.length === 0 ? (
              <div className="p-8 text-center text-sm text-emerald-700 bg-emerald-50/40">
                Wszystkie produkty w katalogu mają bezpieczny stan magazynowy (&ge; 10 szt.).
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold uppercase tracking-wider text-slate-500">
                      <th className="py-3 px-6">ID</th>
                      <th className="py-3 px-6">Nazwa produktu</th>
                      <th className="py-3 px-6">Kategoria</th>
                      <th className="py-3 px-6">Aktualny stan</th>
                      <th className="py-3 px-6 text-right">Cena</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-sm">
                    {lowStockProducts.map((p) => {
                      const kat = kategorieMap[p.id_kategorii];
                      return (
                        <tr key={p.id_produktu} className="hover:bg-slate-50/60">
                          <td className="py-3.5 px-6 font-mono text-xs text-slate-500">
                            #{p.id_produktu}
                          </td>
                          <td className="py-3.5 px-6 font-semibold text-slate-900">
                            {p.nazwa_produktu}
                          </td>
                          <td className="py-3.5 px-6 text-slate-600">
                            {kat ? kat.nazwa_kategorii : `#${p.id_kategorii}`}
                          </td>
                          <td className="py-3.5 px-6 whitespace-nowrap">
                            <StockBadge qty={p.stan_magazynowy ?? 0} showExactCount={true} />
                          </td>
                          <td className="py-3.5 px-6 text-right font-medium text-slate-900 whitespace-nowrap">
                            {zl(p.cena)}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          {/* Sekcja 4 i 5: Top Klienci + Podsumowanie recept i płatności */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
            {/* Sekcja 4: Top Klienci */}
            <section className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden">
              <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
                <h3 className="text-base font-bold text-slate-900">
                  Top klienci wg wartości zamówień
                </h3>
                <Link
                  to="/panel/klienci"
                  className="text-xs font-semibold text-teal-700 hover:underline"
                >
                  Baza klientów →
                </Link>
              </div>

              {topClients.length === 0 ? (
                <div className="p-8 text-center text-sm text-slate-500">
                  Brak nieanulowanych zamówień do wyświetlenia.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <thead>
                      <tr className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold uppercase tracking-wider text-slate-500">
                        <th className="py-3 px-5">Klient</th>
                        <th className="py-3 px-5 text-right">Zamówienia</th>
                        <th className="py-3 px-5 text-right">Łączna kwota</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 text-sm">
                      {topClients.map((row) => (
                        <tr key={row.idKlienta} className="hover:bg-slate-50/60">
                          <td className="py-3.5 px-5">
                            {row.klient ? (
                              <div>
                                <p className="font-semibold text-slate-900">
                                  {row.klient.imie} {row.klient.nazwisko}
                                </p>
                                <p className="text-xs text-slate-500">
                                  {row.klient.email}
                                </p>
                              </div>
                            ) : (
                              <span className="font-mono text-xs text-slate-500">
                                Klient #{row.idKlienta}
                              </span>
                            )}
                          </td>
                          <td className="py-3.5 px-5 text-right font-medium text-slate-700">
                            {row.count}
                          </td>
                          <td className="py-3.5 px-5 text-right font-bold text-teal-700 whitespace-nowrap">
                            {zl(row.total)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>

            {/* Sekcja 5: Podsumowanie recept i płatności */}
            <div className="space-y-6">
              <section className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6 space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-base font-bold text-slate-900">
                    Podsumowanie recept wg statusu
                  </h3>
                  <Link
                    to="/panel/recepty"
                    className="text-xs font-semibold text-teal-700 hover:underline"
                  >
                    Panel recept →
                  </Link>
                </div>

                <div className="space-y-3">
                  {receptyByStatus.map((row) => (
                    <div key={row.status} className="space-y-1">
                      <div className="flex items-center justify-between text-xs">
                        <ReceptaStatusBadge status={row.status} />
                        <span className="font-semibold text-slate-800">
                          {row.count} ({row.share.toFixed(0)}%)
                        </span>
                      </div>
                      <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-teal-600 rounded-full"
                          style={{ width: `${row.share.toFixed(1)}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </section>

              <section className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6 space-y-4">
                <h3 className="text-base font-bold text-slate-900">
                  Podsumowanie płatności wg statusu
                </h3>

                <div className="space-y-3">
                  {platnosciByStatus.map((row) => (
                    <div key={row.status} className="space-y-1">
                      <div className="flex items-center justify-between text-xs">
                        <PlatnoscStatusBadge status={row.status} />
                        <span className="font-semibold text-slate-800">
                          {row.count} ({row.share.toFixed(0)}%)
                        </span>
                      </div>
                      <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-slate-700 rounded-full"
                          style={{ width: `${row.share.toFixed(1)}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
