import { useCallback, useEffect, useMemo, useState } from 'react';
import type { Produkt } from '../types/produkt';
import { produktyService } from '../services/produktyService';
import { Link, useNavigate } from 'react-router-dom';
import { RxBadge, StockBadge } from './Badges';
import { zl } from '../utils/format';

type Status = 'connecting' | 'success' | 'error';

const STATUS: Record<Status, { badge: string; dot: string; label: string }> = {
  connecting: {
    badge: 'bg-amber-50 text-amber-700 border-amber-200',
    dot: 'bg-amber-500 animate-pulse',
    label: 'Łączenie z API...',
  },
  success: {
    badge: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    dot: 'bg-emerald-500',
    label: 'Połączono z API',
  },
  error: {
    badge: 'bg-rose-50 text-rose-700 border-rose-200',
    dot: 'bg-rose-500',
    label: 'Błąd połączenia',
  },
};

export const ProduktyList: React.FC = () => {
  const navigate = useNavigate();
  const [produkty, setProdukty] = useState<Produkt[]>([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState<Status>('connecting');
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    setStatus('connecting');
    try {
      setProdukty(await produktyService.getAll());
      setStatus('success');
    } catch (err) {
      console.error(err);
      setProdukty([]);
      setStatus('error');
      setError(err instanceof Error ? err.message : 'Nieznany błąd');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const filtered = useMemo(() => {
    const q = query.toLowerCase().trim();
    if (!q) return produkty;
    return produkty.filter(
      (l) =>
        l.nazwa_produktu?.toLowerCase().includes(q) ||
        String(l.id_produktu) === q,
    );
  }, [produkty, query]);

  const st = STATUS[status];

  return (
    <div className="space-y-6">
      {/* Controls */}
      <div className="bg-white rounded-2xl p-4 sm:p-6 shadow-sm border border-slate-200/80 flex flex-col md:flex-row gap-4 justify-between items-stretch md:items-center">
        <div className="relative flex-1">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Szukaj według nazwy lub ID..."
            className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 transition-all placeholder:text-slate-400"
          />
        </div>

        <div className="flex items-center gap-3 flex-wrap">
          <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium border ${st.badge}`}>
            <span className={`w-2 h-2 rounded-full ${st.dot}`} />
            <span>{st.label}</span>
          </div>
          <button
            onClick={load}
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-teal-600 hover:bg-teal-700 active:bg-teal-800 text-white font-medium text-sm rounded-xl transition-all shadow-sm hover:shadow-md shadow-teal-600/20"
          >
            <svg className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>Odśwież</span>
          </button>
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-5 text-rose-900">
          <h3 className="font-semibold text-rose-950">
            Nie udało się pobrać danych (
            <code className="text-xs bg-rose-100 px-1.5 py-0.5 rounded font-mono">/api/v1/produkty</code>)
          </h3>
          <p className="text-xs sm:text-sm text-rose-800 mt-1">{error}</p>
          <ul className="text-xs text-rose-800 space-y-1 list-disc pl-4 mt-3">
            <li>Sprawdź, czy backend działa i czy proxy API jest poprawnie skonfigurowane.</li>
          </ul>
        </div>
      )}

      {/* Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden">
        <div className="overflow-x-auto relative min-h-[300px]">
          {loading && (
            <div className="absolute inset-0 bg-white/80 backdrop-blur-sm z-20 flex flex-col items-center justify-center gap-3">
              <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
              <p className="text-sm font-medium text-slate-600">Pobieranie listy produktów...</p>
            </div>
          )}

          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold uppercase tracking-wider text-slate-500">
                <th className="py-3.5 px-4 sm:px-6 w-16">ID</th>
                <th className="py-3.5 px-4 sm:px-6">Nazwa produktu</th>
                <th className="py-3.5 px-4 sm:px-6">Cena</th>
                <th className="py-3.5 px-4 sm:px-6">Recepta (Rx)</th>
                <th className="py-3.5 px-4 sm:px-6">Dostępność</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-sm">
              {filtered.map((produkt) => (
                <tr
                  key={produkt.id_produktu}
                  onClick={() => navigate(`/produkty/${produkt.id_produktu}`)}
                  className="hover:bg-slate-50/80 transition-colors cursor-pointer"
                >
                  <td className="py-4 px-4 sm:px-6 font-mono text-xs text-slate-400">#{produkt.id_produktu}</td>
                  <td className="py-4 px-4 sm:px-6 font-semibold text-slate-900">
                    <Link to={`/produkty/${produkt.id_produktu}`} className="hover:text-teal-700">{produkt.nazwa_produktu}</Link>
                  </td>
                  <td className="py-4 px-4 sm:px-6 font-medium text-slate-900 whitespace-nowrap">{zl(produkt.cena)}</td>
                  <td className="py-4 px-4 sm:px-6 whitespace-nowrap">
                    <RxBadge required={!!produkt.wymaga_recepty} />
                  </td>
                  <td className="py-4 px-4 sm:px-6 whitespace-nowrap">
                    <StockBadge qty={produkt.stan_magazynowy ?? 0} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {!loading && filtered.length === 0 && (
            <div className="py-16 text-center">
              <p className="text-slate-700 font-medium">Brak danych do wyświetlenia</p>
              <p className="text-xs text-slate-400 mt-1">
                Spróbuj zmienić zapytanie wyszukiwania lub odświeżyć bazę.
              </p>
            </div>
          )}
        </div>

        <div className="bg-slate-50/50 px-4 sm:px-6 py-3 border-t border-slate-200/80 flex items-center justify-between text-xs text-slate-500">
          <span>Wyświetlono {filtered.length} z {produkty.length} pozycji</span>
          <span className="font-mono text-[11px] text-slate-400">Endpoint: /api/v1/produkty</span>
        </div>
      </div>
    </div>
  );
};