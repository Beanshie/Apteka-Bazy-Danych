import { useCallback, useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { receptyService } from '../services/receptyService';
import { klienciService } from '../services/klienciService';
import type { Recepta, StatusRecepty } from '../types/recepta';
import type { Klient } from '../types/klient';
import { ReceptaStatusBadge } from './Badges';

const RECEPTA_STATUSES: { value: StatusRecepty; label: string }[] = [
  { value: 'NOWA', label: 'Nowa' },
  { value: 'ZWERYFIKOWANA', label: 'Zweryfikowana' },
  { value: 'ZREALIZOWANA', label: 'Zrealizowana' },
  { value: 'ODRZUCONA', label: 'Odrzucona' },
];

export const EmployeeReceptyPage: React.FC = () => {
  const [recepty, setRecepty] = useState<Recepta[]>([]);
  const [klienciMap, setKlienciMap] = useState<Record<number, Klient>>({});
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [statusFilter, setStatusFilter] = useState<string>('');
  const [query, setQuery] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [receptyData, klienciData] = await Promise.all([
        receptyService.getAll(),
        klienciService.getAll().catch(() => [] as Klient[]),
      ]);
      setRecepty(receptyData);
      const map: Record<number, Klient> = {};
      for (const k of klienciData) {
        map[k.id_klienta] = k;
      }
      setKlienciMap(map);
    } catch (err) {
      console.error(err);
      setError('Nie udało się pobrać listy recept.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleStatusChange = async (recepta: Recepta, nextStatus: StatusRecepty) => {
    if (recepta.status_recepty === nextStatus) return;
    setSavingId(recepta.id_recepty);
    setError(null);
    try {
      const updated = await receptyService.update(recepta.id_recepty, {
        id_klienta: recepta.id_klienta,
        kod_recepty: recepta.kod_recepty,
        pesel_pacjenta: recepta.pesel_pacjenta,
        status_recepty: nextStatus,
      });
      setRecepty((prev) =>
        prev.map((r) => (r.id_recepty === updated.id_recepty ? updated : r)),
      );
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const st = err.response?.status;
        if (st === 400) {
          setError(`Nieprawidłowe dane lub niedozwolona zmiana statusu dla recepty #${recepta.id_recepty}.`);
        } else if (st === 404) {
          setError(`Recepta #${recepta.id_recepty} nie została znaleziona.`);
        } else if (st === 409) {
          setError(`Konflikt podczas aktualizacji recepty #${recepta.id_recepty}.`);
        } else {
          setError(`Błąd serwera podczas aktualizacji recepty #${recepta.id_recepty}.`);
        }
      } else {
        setError('Nie udało się zaktualizować statusu recepty.');
      }
    } finally {
      setSavingId(null);
    }
  };

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return [...recepty]
      .sort((a, b) => b.id_recepty - a.id_recepty)
      .filter((r) => {
        if (statusFilter && r.status_recepty !== statusFilter) return false;
        if (q) {
          const klient = klienciMap[r.id_klienta];
          const klientText = klient
            ? `${klient.imie} ${klient.nazwisko} ${klient.email}`.toLowerCase()
            : '';
          const match =
            r.kod_recepty.toLowerCase().includes(q) ||
            r.pesel_pacjenta.toLowerCase().includes(q) ||
            String(r.id_recepty) === q ||
            klientText.includes(q);
          if (!match) return false;
        }
        return true;
      });
  }, [recepty, klienciMap, statusFilter, query]);

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-2xl p-4 sm:p-6 shadow-sm border border-slate-200/80 space-y-4">
        <div className="flex flex-col sm:flex-row gap-4 justify-between items-start sm:items-center">
          <div>
            <h2 className="text-xl font-bold text-slate-900">Panel recept</h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Weryfikacja i zmiana statusów e-recept klientów
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
            placeholder="Szukaj po kodzie recepty, PESEL lub kliencie..."
            className="flex-1 min-w-[220px] px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
          />

          <select
            aria-label="Filtruj status recepty"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
          >
            <option value="">Wszystkie statusy</option>
            {RECEPTA_STATUSES.map((s) => (
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
            className="text-xs font-semibold text-rose-700 hover:underline"
          >
            Zamknij
          </button>
        </div>
      )}

      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden">
        <div className="overflow-x-auto relative min-h-[240px]">
          {loading && (
            <div className="absolute inset-0 bg-white/80 backdrop-blur-sm z-20 flex flex-col items-center justify-center gap-3">
              <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
              <p className="text-sm font-medium text-slate-600">Ładowanie recept...</p>
            </div>
          )}

          {!loading && filtered.length === 0 ? (
            <div className="py-16 text-center text-sm text-slate-500">
              Brak recept spełniających kryteria wyszukiwania.
            </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold uppercase tracking-wider text-slate-500">
                  <th className="py-3.5 px-4">ID</th>
                  <th className="py-3.5 px-4">Kod recepty</th>
                  <th className="py-3.5 px-4">PESEL pacjenta</th>
                  <th className="py-3.5 px-4">Klient</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4 text-right">Zmiana statusu</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {filtered.map((recepta) => {
                  const klient = klienciMap[recepta.id_klienta];
                  const busy = savingId === recepta.id_recepty;

                  return (
                    <tr key={recepta.id_recepty} className="hover:bg-slate-50/60">
                      <td className="py-3.5 px-4 font-mono text-xs text-slate-500">
                        #{recepta.id_recepty}
                      </td>
                      <td className="py-3.5 px-4 font-mono font-semibold text-slate-900">
                        {recepta.kod_recepty}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-slate-700">
                        {recepta.pesel_pacjenta}
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
                            Klient #{recepta.id_klienta}
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <ReceptaStatusBadge status={recepta.status_recepty} />
                      </td>
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <div className="inline-flex items-center gap-1.5">
                          <button
                            type="button"
                            disabled={busy || recepta.status_recepty === 'ZWERYFIKOWANA'}
                            onClick={() => handleStatusChange(recepta, 'ZWERYFIKOWANA')}
                            className="px-2.5 py-1.5 rounded-lg border border-emerald-200 bg-emerald-50 hover:bg-emerald-100 disabled:opacity-40 text-emerald-700 text-xs font-semibold transition-all"
                          >
                            Zweryfikuj
                          </button>
                          <button
                            type="button"
                            disabled={busy || recepta.status_recepty === 'ZREALIZOWANA'}
                            onClick={() => handleStatusChange(recepta, 'ZREALIZOWANA')}
                            className="px-2.5 py-1.5 rounded-lg border border-teal-200 bg-teal-50 hover:bg-teal-100 disabled:opacity-40 text-teal-700 text-xs font-semibold transition-all"
                          >
                            Zrealizuj
                          </button>
                          <button
                            type="button"
                            disabled={busy || recepta.status_recepty === 'ODRZUCONA'}
                            onClick={() => handleStatusChange(recepta, 'ODRZUCONA')}
                            className="px-2.5 py-1.5 rounded-lg border border-rose-200 bg-rose-50 hover:bg-rose-100 disabled:opacity-40 text-rose-700 text-xs font-semibold transition-all"
                          >
                            Odrzuć
                          </button>
                        </div>
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
            Wyświetlono {filtered.length} z {recepty.length} recept
          </span>
        </div>
      </div>
    </div>
  );
};
