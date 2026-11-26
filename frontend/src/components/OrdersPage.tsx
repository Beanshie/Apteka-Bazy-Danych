import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { zamowieniaService } from '../services/zamowieniaService';
import type { Zamowienie } from '../types/zamowienie';
import { OrderStatusBadge } from './Badges';
import { zl } from '../utils/format';

const formatDatePl = (iso: string) => {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('pl-PL', {
    year: 'numeric',
    month: 'long',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export const OrdersPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, loading: authLoading, isAuthenticated, isKlient } = useAuth();

  const [zamowienia, setZamowienia] = useState<Zamowienie[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!user || !isKlient) return;
    setLoading(true);
    setError(null);
    try {
      const data = await zamowieniaService.getByKlientId(user.id);
      setZamowienia(data);
    } catch (err) {
      console.error(err);
      setError(
        err instanceof Error
          ? err.message
          : 'Nie udało się pobrać listy zamówień.',
      );
    } finally {
      setLoading(false);
    }
  }, [user, isKlient]);

  useEffect(() => {
    load();
  }, [load]);

  const sorted = useMemo(() => {
    return [...zamowienia].sort((a, b) => {
      const timeDiff =
        new Date(b.data_zamowienia).getTime() -
        new Date(a.data_zamowienia).getTime();
      if (!Number.isNaN(timeDiff) && timeDiff !== 0) return timeDiff;
      return b.id_zamowienia - a.id_zamowienia;
    });
  }, [zamowienia]);

  if (authLoading) {
    return (
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 py-20 flex flex-col items-center justify-center gap-3">
        <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
        <p className="text-sm font-medium text-slate-600">Sprawdzanie sesji...</p>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (!isKlient) {
    return (
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 py-16 text-center space-y-3">
        <p className="text-slate-700 font-medium">
          Historia zamówień klienta jest dostępna wyłącznie dla kont klientów.
        </p>
        <Link to="/" className="inline-block text-sm font-medium text-teal-700 hover:underline">
          Wróć do listy produktów
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-2xl p-4 sm:p-6 shadow-sm border border-slate-200/80 flex flex-col sm:flex-row gap-4 justify-between items-start sm:items-center">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Moje zamówienia</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Historia zamówień dla konta {user.imie} {user.nazwisko} ({user.identyfikator})
          </p>
        </div>

        <button
          type="button"
          onClick={load}
          disabled={loading}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-teal-600 hover:bg-teal-700 active:bg-teal-800 disabled:opacity-60 text-white font-medium text-sm rounded-xl transition-all shadow-sm hover:shadow-md shadow-teal-600/20"
        >
          <svg
            className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
            />
          </svg>
          <span>Odśwież</span>
        </button>
      </div>

      {error && (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-5 text-rose-900 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <h3 className="font-semibold text-rose-950">
              Nie udało się pobrać zamówień
            </h3>
            <p className="text-xs sm:text-sm text-rose-800 mt-1">{error}</p>
          </div>
          <button
            type="button"
            onClick={load}
            className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold transition-all whitespace-nowrap"
          >
            Spróbuj ponownie
          </button>
        </div>
      )}

      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden">
        <div className="overflow-x-auto relative min-h-[260px]">
          {loading && (
            <div className="absolute inset-0 bg-white/80 backdrop-blur-sm z-20 flex flex-col items-center justify-center gap-3">
              <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
              <p className="text-sm font-medium text-slate-600">
                Pobieranie Twoich zamówień...
              </p>
            </div>
          )}

          {!loading && !error && sorted.length === 0 ? (
            <div className="py-16 text-center space-y-3">
              <p className="text-slate-700 font-medium">
                Nie masz jeszcze żadnych zamówień
              </p>
              <p className="text-xs text-slate-400">
                Dodaj wybrane produkty do koszyka i złóż swoje pierwsze zamówienie.
              </p>
              <div className="pt-2">
                <Link
                  to="/"
                  className="inline-flex items-center justify-center px-5 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-sm font-medium transition-all shadow-sm shadow-teal-600/20"
                >
                  Przejdź do katalogu produktów
                </Link>
              </div>
            </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold uppercase tracking-wider text-slate-500">
                  <th className="py-3.5 px-4 sm:px-6">Zamówienie</th>
                  <th className="py-3.5 px-4 sm:px-6">Data złożenia</th>
                  <th className="py-3.5 px-4 sm:px-6">Status</th>
                  <th className="py-3.5 px-4 sm:px-6">Kwota całkowita</th>
                  <th className="py-3.5 px-4 sm:px-6 text-right">Akcje</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {sorted.map((zamowienie) => (
                  <tr
                    key={zamowienie.id_zamowienia}
                    onClick={() =>
                      navigate(`/zamowienia/${zamowienie.id_zamowienia}`)
                    }
                    className="hover:bg-slate-50/80 transition-colors cursor-pointer"
                  >
                    <td className="py-4 px-4 sm:px-6 font-mono font-semibold text-slate-900">
                      #{zamowienie.id_zamowienia}
                    </td>
                    <td className="py-4 px-4 sm:px-6 text-slate-600 whitespace-nowrap">
                      {formatDatePl(zamowienie.data_zamowienia)}
                    </td>
                    <td className="py-4 px-4 sm:px-6 whitespace-nowrap">
                      <OrderStatusBadge status={zamowienie.status_zamowienia} />
                    </td>
                    <td className="py-4 px-4 sm:px-6 font-semibold text-slate-900 whitespace-nowrap">
                      {zl(zamowienie.suma_calkowita)}
                    </td>
                    <td
                      className="py-4 px-4 sm:px-6 text-right whitespace-nowrap"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <Link
                        to={`/zamowienia/${zamowienie.id_zamowienia}`}
                        className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold text-teal-700 hover:bg-teal-50 hover:border-teal-200 transition-all"
                      >
                        Szczegóły →
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="bg-slate-50/50 px-4 sm:px-6 py-3 border-t border-slate-200/80 flex items-center justify-between text-xs text-slate-500">
          <span>Łącznie zamówień: {sorted.length}</span>
          <span className="font-mono text-[11px] text-slate-400">
            Endpoint: /api/v1/zamowienia?id_klienta={user.id}
          </span>
        </div>
      </div>
    </div>
  );
};
