import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { zamowieniaService } from '../services/zamowieniaService';
import { platnosciService } from '../services/platnosciService';
import { klienciService } from '../services/klienciService';
import type { StatusZamowienia, Zamowienie } from '../types/zamowienie';
import type { Platnosc, StatusPlatnosci } from '../types/platnosc';
import type { Klient } from '../types/klient';
import { OrderStatusBadge, PlatnoscStatusBadge } from './Badges';
import { formatDatePl, zl } from '../utils/format';

const ORDER_STATUSES: { value: StatusZamowienia; label: string }[] = [
  { value: 'NOWE', label: 'Nowe' },
  { value: 'OPLACONE', label: 'Opłacone' },
  { value: 'W_REALIZACJI', label: 'W realizacji' },
  { value: 'WYSLANE', label: 'Wysłane' },
  { value: 'ZREALIZOWANE', label: 'Zrealizowane' },
  { value: 'ANULOWANE', label: 'Anulowane' },
];

const PAYMENT_STATUSES: { value: StatusPlatnosci; label: string }[] = [
  { value: 'OCZEKUJACA', label: 'Oczekująca' },
  { value: 'OPLACONA', label: 'Opłacona' },
  { value: 'NIEUDANA', label: 'Nieudana' },
  { value: 'ZWROCONA', label: 'Zwrócona' },
];

export const EmployeeOrdersPage: React.FC = () => {
  const [zamowienia, setZamowienia] = useState<Zamowienie[]>([]);
  const [platnosciMap, setPlatnosciMap] = useState<Record<number, Platnosc>>({});
  const [klienciMap, setKlienciMap] = useState<Record<number, Klient>>({});
  const [draftOrderStatus, setDraftOrderStatus] = useState<Record<number, StatusZamowienia>>({});

  const [loading, setLoading] = useState(true);
  const [savingOrderId, setSavingOrderId] = useState<number | null>(null);
  const [savingPaymentId, setSavingPaymentId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [statusFilter, setStatusFilter] = useState<string>('');
  const [query, setQuery] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [ordersData, paymentsData, clientsData] = await Promise.all([
        zamowieniaService.getAll(),
        platnosciService.getAll().catch(() => [] as Platnosc[]),
        klienciService.getAll().catch(() => [] as Klient[]),
      ]);

      setZamowienia(ordersData);

      const drafts: Record<number, StatusZamowienia> = {};
      for (const z of ordersData) {
        drafts[z.id_zamowienia] = z.status_zamowienia;
      }
      setDraftOrderStatus(drafts);

      const pMap: Record<number, Platnosc> = {};
      for (const p of paymentsData) {
        pMap[p.id_zamowienia] = p;
      }
      setPlatnosciMap(pMap);

      const kMap: Record<number, Klient> = {};
      for (const k of clientsData) {
        kMap[k.id_klienta] = k;
      }
      setKlienciMap(kMap);
    } catch (err) {
      console.error(err);
      setError('Nie udało się pobrać listy zamówień.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleSaveOrderStatus = async (zamowienie: Zamowienie) => {
    const nextStatus = draftOrderStatus[zamowienie.id_zamowienia];
    if (!nextStatus || nextStatus === zamowienie.status_zamowienia) return;

    setSavingOrderId(zamowienie.id_zamowienia);
    setError(null);
    try {
      const updated = await zamowieniaService.update(zamowienie.id_zamowienia, {
        id_klienta: zamowienie.id_klienta,
        id_adresu: zamowienie.id_adresu,
        status_zamowienia: nextStatus,
      });
      setZamowienia((prev) =>
        prev.map((z) => (z.id_zamowienia === updated.id_zamowienia ? updated : z)),
      );
      setDraftOrderStatus((prev) => ({
        ...prev,
        [updated.id_zamowienia]: updated.status_zamowienia,
      }));
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const st = err.response?.status;
        if (st === 400) {
          setError(
            `Nie udało się zmienić statusu zamówienia #${zamowienie.id_zamowienia} (400): brak wystarczającego stanu magazynowego lub nieważna recepta przy reaktywacji.`,
          );
        } else if (st === 404) {
          setError(`Nie znaleziono zamówienia #${zamowienie.id_zamowienia} lub powiązanego rekordu.`);
        } else if (st === 409) {
          setError(`Konflikt podczas aktualizacji zamówienia #${zamowienie.id_zamowienia}.`);
        } else {
          setError(`Błąd serwera podczas aktualizacji zamówienia #${zamowienie.id_zamowienia}.`);
        }
      } else {
        setError('Nie udało się zaktualizować statusu zamówienia.');
      }
    } finally {
      setSavingOrderId(null);
    }
  };

  const handlePaymentStatusChange = async (
    platnosc: Platnosc,
    nextStatus: StatusPlatnosci,
  ) => {
    if (platnosc.status_platnosci === nextStatus) return;

    setSavingPaymentId(platnosc.id_platnosci);
    setError(null);
    try {
      const updated = await platnosciService.update(platnosc.id_platnosci, {
        id_zamowienia: platnosc.id_zamowienia,
        metoda_platnosci: platnosc.metoda_platnosci,
        status_platnosci: nextStatus,
        data_platnosci:
          nextStatus === 'OPLACONA'
            ? platnosc.data_platnosci ?? new Date().toISOString()
            : platnosc.data_platnosci,
      });
      setPlatnosciMap((prev) => ({
        ...prev,
        [updated.id_zamowienia]: updated,
      }));
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const st = err.response?.status;
        if (st === 400) {
          setError(`Nieprawidłowe dane podczas aktualizacji płatności #${platnosc.id_platnosci}.`);
        } else if (st === 404) {
          setError(`Płatność #${platnosc.id_platnosci} nie została znaleziona.`);
        } else {
          setError(`Błąd serwera podczas aktualizacji płatności #${platnosc.id_platnosci}.`);
        }
      } else {
        setError('Nie udało się zaktualizować statusu płatności.');
      }
    } finally {
      setSavingPaymentId(null);
    }
  };

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return [...zamowienia]
      .sort((a, b) => {
        const timeDiff =
          new Date(b.data_zamowienia).getTime() - new Date(a.data_zamowienia).getTime();
        if (!Number.isNaN(timeDiff) && timeDiff !== 0) return timeDiff;
        return b.id_zamowienia - a.id_zamowienia;
      })
      .filter((z) => {
        if (statusFilter && z.status_zamowienia !== statusFilter) return false;
        if (q) {
          const klient = klienciMap[z.id_klienta];
          const klientText = klient
            ? `${klient.imie} ${klient.nazwisko} ${klient.email}`.toLowerCase()
            : '';
          const match =
            String(z.id_zamowienia).includes(q.replace(/^#/, '')) ||
            klientText.includes(q);
          if (!match) return false;
        }
        return true;
      });
  }, [zamowienia, klienciMap, statusFilter, query]);

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-2xl p-4 sm:p-6 shadow-sm border border-slate-200/80 space-y-4">
        <div className="flex flex-col sm:flex-row gap-4 justify-between items-start sm:items-center">
          <div>
            <h2 className="text-xl font-bold text-slate-900">
              Panel zamówień i płatności
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Obsługa wszystkich zamówień apteki oraz aktualizacja statusów płatności
            </p>
          </div>

          <button
            type="button"
            onClick={load}
            disabled={loading}
            className="inline-flex items-center gap-2 px-4 py-2 bg-teal-600 hover:bg-teal-700 disabled:opacity-60 text-white font-medium text-sm rounded-xl transition-all"
          >
            Odśwież
          </button>
        </div>

        <div className="flex flex-wrap items-center gap-3 pt-3 border-t border-slate-100">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Szukaj po ID zamówienia lub kliencie (imię, nazwisko, e-mail)..."
            className="flex-1 min-w-[240px] px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
          />

          <select
            aria-label="Filtruj status zamówienia"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
          >
            <option value="">Wszystkie statusy</option>
            {ORDER_STATUSES.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>

          {(query || statusFilter) && (
            <button
              type="button"
              onClick={() => {
                setQuery('');
                setStatusFilter('');
              }}
              className="px-3 py-2 text-xs font-semibold text-slate-600 hover:text-rose-600 rounded-xl border border-slate-200 hover:bg-slate-50"
            >
              Wyczyść filtry
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-4 text-sm text-rose-800 flex items-center justify-between gap-4">
          <span>{error}</span>
          <button
            type="button"
            onClick={() => setError(null)}
            className="text-xs font-semibold text-rose-700 hover:underline whitespace-nowrap"
          >
            Zamknij
          </button>
        </div>
      )}

      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden">
        <div className="overflow-x-auto relative min-h-[260px]">
          {loading && (
            <div className="absolute inset-0 bg-white/80 backdrop-blur-sm z-20 flex flex-col items-center justify-center gap-3">
              <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
              <p className="text-sm font-medium text-slate-600">Ładowanie zamówień...</p>
            </div>
          )}

          {!loading && filtered.length === 0 ? (
            <div className="py-16 text-center text-sm text-slate-500">
              Brak zamówień spełniających kryteria wyszukiwania.
            </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold uppercase tracking-wider text-slate-500">
                  <th className="py-3.5 px-4">Zamówienie</th>
                  <th className="py-3.5 px-4">Klient</th>
                  <th className="py-3.5 px-4">Data</th>
                  <th className="py-3.5 px-4">Kwota</th>
                  <th className="py-3.5 px-4">Status zamówienia</th>
                  <th className="py-3.5 px-4">Płatność</th>
                  <th className="py-3.5 px-4 text-right">Szczegóły</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {filtered.map((zamowienie) => {
                  const klient = klienciMap[zamowienie.id_klienta];
                  const platnosc = platnosciMap[zamowienie.id_zamowienia];
                  const draftStatus =
                    draftOrderStatus[zamowienie.id_zamowienia] ??
                    zamowienie.status_zamowienia;
                  const orderChanged = draftStatus !== zamowienie.status_zamowienia;
                  const orderBusy = savingOrderId === zamowienie.id_zamowienia;
                  const paymentBusy =
                    platnosc != null && savingPaymentId === platnosc.id_platnosci;

                  return (
                    <tr key={zamowienie.id_zamowienia} className="hover:bg-slate-50/60">
                      <td className="py-3.5 px-4 font-mono font-semibold text-slate-900 whitespace-nowrap">
                        #{zamowienie.id_zamowienia}
                      </td>
                      <td className="py-3.5 px-4">
                        {klient ? (
                          <div>
                            <p className="font-medium text-slate-900">
                              {klient.imie} {klient.nazwisko}
                            </p>
                            <p className="text-xs text-slate-500">{klient.email}</p>
                          </div>
                        ) : (
                          <span className="font-mono text-xs text-slate-500">
                            Klient #{zamowienie.id_klienta}
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-xs text-slate-600 whitespace-nowrap">
                        {formatDatePl(zamowienie.data_zamowienia)}
                      </td>
                      <td className="py-3.5 px-4 font-semibold text-slate-900 whitespace-nowrap">
                        {zl(zamowienie.suma_calkowita)}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="space-y-1.5">
                          <OrderStatusBadge status={zamowienie.status_zamowienia} />
                          <div className="flex items-center gap-1.5">
                            <select
                              aria-label={`Status zamówienia #${zamowienie.id_zamowienia}`}
                              value={draftStatus}
                              disabled={orderBusy}
                              onChange={(e) =>
                                setDraftOrderStatus((prev) => ({
                                  ...prev,
                                  [zamowienie.id_zamowienia]: e.target
                                    .value as StatusZamowienia,
                                }))
                              }
                              className="px-2 py-1 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-700 focus:outline-none focus:border-teal-500"
                            >
                              {ORDER_STATUSES.map((s) => (
                                <option key={s.value} value={s.value}>
                                  {s.label}
                                </option>
                              ))}
                            </select>
                            <button
                              type="button"
                              disabled={!orderChanged || orderBusy}
                              onClick={() => handleSaveOrderStatus(zamowienie)}
                              className="px-2.5 py-1 rounded-lg bg-teal-600 hover:bg-teal-700 disabled:opacity-40 text-white text-xs font-semibold transition-all"
                            >
                              {orderBusy ? '...' : 'Zapisz'}
                            </button>
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-4">
                        {platnosc ? (
                          <div className="space-y-1.5">
                            <div className="flex items-center gap-1.5">
                              <PlatnoscStatusBadge status={platnosc.status_platnosci} />
                              <span className="text-[11px] text-slate-500">
                                ({platnosc.metoda_platnosci})
                              </span>
                            </div>
                            <select
                              aria-label={`Status płatności dla zamówienia #${zamowienie.id_zamowienia}`}
                              value={platnosc.status_platnosci}
                              disabled={paymentBusy}
                              onChange={(e) =>
                                handlePaymentStatusChange(
                                  platnosc,
                                  e.target.value as StatusPlatnosci,
                                )
                              }
                              className="px-2 py-1 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-700 focus:outline-none focus:border-teal-500 disabled:opacity-50"
                            >
                              {PAYMENT_STATUSES.map((s) => (
                                <option key={s.value} value={s.value}>
                                  {s.label}
                                </option>
                              ))}
                            </select>
                          </div>
                        ) : (
                          <span className="text-xs text-slate-400">Brak płatności</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <Link
                          to={`/zamowienia/${zamowienie.id_zamowienia}`}
                          className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold text-teal-700 hover:bg-teal-50 hover:border-teal-200 transition-all"
                        >
                          Szczegóły →
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>

        <div className="bg-slate-50/50 px-4 py-3 border-t border-slate-200/80 flex items-center justify-between text-xs text-slate-500">
          <span>
            Wyświetlono {filtered.length} z {zamowienia.length} zamówień
          </span>
          <span className="font-mono text-[11px] text-slate-400">
            Endpoint: /api/v1/zamowienia
          </span>
        </div>
      </div>
    </div>
  );
};
