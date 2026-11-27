import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { klienciService } from '../services/klienciService';
import { adresyService } from '../services/adresyService';
import { receptyService } from '../services/receptyService';
import { zamowieniaService } from '../services/zamowieniaService';
import type { Klient } from '../types/klient';
import type { AdresKlienta } from '../types/adres_klienta';
import type { Recepta } from '../types/recepta';
import type { Zamowienie } from '../types/zamowienie';
import { OrderStatusBadge, ReceptaStatusBadge } from './Badges';
import { zl } from '../utils/format';

const formatDatePl = (iso: string) => {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('pl-PL', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export const EmployeeClientsPage: React.FC = () => {
  const [klienci, setKlienci] = useState<Klient[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');

  // Wybrany klient i jego powiązane dane
  const [selectedKlient, setSelectedKlient] = useState<Klient | null>(null);
  const [loadingDetails, setLoadingDetails] = useState(false);
  const [detailsError, setDetailsError] = useState<string | null>(null);
  const [adresy, setAdresy] = useState<AdresKlienta[]>([]);
  const [recepty, setRecepty] = useState<Recepta[]>([]);
  const [zamowienia, setZamowienia] = useState<Zamowienie[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await klienciService.getAll();
      setKlienci(data);
    } catch (err) {
      console.error(err);
      setError('Nie udało się pobrać listy klientów.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleSelectKlient = async (klient: Klient) => {
    if (selectedKlient?.id_klienta === klient.id_klienta) {
      setSelectedKlient(null);
      return;
    }

    setSelectedKlient(klient);
    setLoadingDetails(true);
    setDetailsError(null);
    try {
      const [adresyData, receptyData, zamowieniaData] = await Promise.all([
        adresyService.getAll({ id_klienta: klient.id_klienta }),
        receptyService.getAll({ id_klienta: klient.id_klienta }),
        zamowieniaService.getByKlientId(klient.id_klienta),
      ]);
      setAdresy(adresyData);
      setRecepty(receptyData);
      setZamowienia(
        [...zamowieniaData].sort(
          (a, b) =>
            new Date(b.data_zamowienia).getTime() -
            new Date(a.data_zamowienia).getTime(),
        ),
      );
    } catch (err) {
      console.error(err);
      setDetailsError('Nie udało się pobrać szczegółów wybranego klienta.');
      setAdresy([]);
      setRecepty([]);
      setZamowienia([]);
    } finally {
      setLoadingDetails(false);
    }
  };

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return klienci;
    const idQuery = q.replace(/^#/, '');
    return klienci.filter(
      (k) =>
        `${k.imie} ${k.nazwisko}`.toLowerCase().includes(q) ||
        k.email.toLowerCase().includes(q) ||
        (k.telefon ?? '').toLowerCase().includes(q) ||
        String(k.id_klienta) === idQuery,
    );
  }, [klienci, query]);

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-2xl p-4 sm:p-6 shadow-sm border border-slate-200/80 space-y-4">
        <div className="flex flex-col sm:flex-row gap-4 justify-between items-start sm:items-center">
          <div>
            <h2 className="text-xl font-bold text-slate-900">Baza klientów</h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Podgląd kont klientów, ich adresów, e-recept i historii zamówień
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
            placeholder="Szukaj po imieniu, nazwisku, e-mailu lub ID..."
            className="flex-1 min-w-[240px] px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
          />
          {query && (
            <button
              type="button"
              onClick={() => setQuery('')}
              className="px-3 py-2 text-xs font-semibold text-slate-600 hover:text-rose-600 rounded-xl border border-slate-200 hover:bg-slate-50"
            >
              Wyczyść
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-4 text-sm text-rose-800">
          {error}
        </div>
      )}

      {/* Tabela klientów */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden">
        <div className="overflow-x-auto relative min-h-[220px]">
          {loading && (
            <div className="absolute inset-0 bg-white/80 backdrop-blur-sm z-20 flex flex-col items-center justify-center gap-3">
              <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
              <p className="text-sm font-medium text-slate-600">Ładowanie klientów...</p>
            </div>
          )}

          {!loading && filtered.length === 0 ? (
            <div className="py-16 text-center text-sm text-slate-500">
              Brak klientów spełniających kryteria wyszukiwania.
            </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold uppercase tracking-wider text-slate-500">
                  <th className="py-3.5 px-4">ID</th>
                  <th className="py-3.5 px-4">Imię i nazwisko</th>
                  <th className="py-3.5 px-4">E-mail</th>
                  <th className="py-3.5 px-4">Telefon</th>
                  <th className="py-3.5 px-4">Rejestracja</th>
                  <th className="py-3.5 px-4 text-right">Akcje</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {filtered.map((klient) => {
                  const isSelected = selectedKlient?.id_klienta === klient.id_klienta;
                  return (
                    <tr
                      key={klient.id_klienta}
                      className={isSelected ? 'bg-teal-50/50' : 'hover:bg-slate-50/60'}
                    >
                      <td className="py-3.5 px-4 font-mono text-xs text-slate-500">
                        #{klient.id_klienta}
                      </td>
                      <td className="py-3.5 px-4 font-semibold text-slate-900">
                        {klient.imie} {klient.nazwisko}
                      </td>
                      <td className="py-3.5 px-4 text-slate-600">{klient.email}</td>
                      <td className="py-3.5 px-4 text-slate-600">
                        {klient.telefon ?? <span className="text-slate-400">—</span>}
                      </td>
                      <td className="py-3.5 px-4 text-xs text-slate-500 whitespace-nowrap">
                        {formatDatePl(klient.data_rejestracji)}
                      </td>
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <button
                          type="button"
                          onClick={() => handleSelectKlient(klient)}
                          className={`px-3 py-1.5 rounded-xl border text-xs font-semibold transition-all ${
                            isSelected
                              ? 'border-teal-600 bg-teal-600 text-white'
                              : 'border-slate-200 text-teal-700 hover:bg-teal-50 hover:border-teal-200'
                          }`}
                        >
                          {isSelected ? 'Ukryj szczegóły' : 'Szczegóły'}
                        </button>
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
            Wyświetlono {filtered.length} z {klienci.length} klientów
          </span>
          <span className="font-mono text-[11px] text-slate-400">
            Endpoint: /api/v1/klienci
          </span>
        </div>
      </div>

      {/* Panel szczegółów wybranego klienta */}
      {selectedKlient && (
        <div className="bg-white rounded-2xl shadow-sm border border-teal-200 p-6 space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-100">
            <div>
              <h3 className="text-lg font-bold text-slate-900">
                Klient #{selectedKlient.id_klienta}: {selectedKlient.imie}{' '}
                {selectedKlient.nazwisko}
              </h3>
              <p className="text-xs text-slate-500">
                {selectedKlient.email}
                {selectedKlient.telefon ? ` • tel. ${selectedKlient.telefon}` : ''}
              </p>
            </div>
            <button
              type="button"
              onClick={() => setSelectedKlient(null)}
              className="text-xs font-semibold text-slate-500 hover:text-slate-800"
            >
              Zamknij panel ✕
            </button>
          </div>

          {detailsError && (
            <div className="bg-rose-50 border border-rose-200 rounded-xl p-3 text-xs text-rose-800">
              {detailsError}
            </div>
          )}

          {loadingDetails ? (
            <div className="py-10 flex items-center justify-center gap-3">
              <div className="w-6 h-6 border-2 border-teal-600 border-t-transparent rounded-full animate-spin" />
              <span className="text-sm text-slate-600">
                Pobieranie adresów, recept i zamówień...
              </span>
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Adresy */}
              <div className="space-y-3">
                <h4 className="text-sm font-bold text-slate-900">
                  Zapisane adresy ({adresy.length})
                </h4>
                {adresy.length === 0 ? (
                  <p className="text-xs text-slate-400">Brak zapisanych adresów.</p>
                ) : (
                  <ul className="space-y-2">
                    {adresy.map((a) => (
                      <li
                        key={a.id_adresu}
                        className="p-3 rounded-xl bg-slate-50 border border-slate-200/70 text-xs space-y-0.5"
                      >
                        <p className="font-semibold text-slate-800">
                          ul. {a.ulica} {a.numer_domu}
                          {a.numer_mieszkania ? `/${a.numer_mieszkania}` : ''}
                        </p>
                        <p className="text-slate-500">
                          {a.kod_pocztowy} {a.miasto}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              {/* Recepty */}
              <div className="space-y-3">
                <h4 className="text-sm font-bold text-slate-900">
                  Recepty ({recepty.length})
                </h4>
                {recepty.length === 0 ? (
                  <p className="text-xs text-slate-400">Brak recept.</p>
                ) : (
                  <ul className="space-y-2">
                    {recepty.map((r) => (
                      <li
                        key={r.id_recepty}
                        className="p-3 rounded-xl bg-slate-50 border border-slate-200/70 text-xs flex items-center justify-between gap-2"
                      >
                        <div>
                          <p className="font-mono font-semibold text-slate-800">
                            Kod: {r.kod_recepty}
                          </p>
                          <p className="font-mono text-slate-500">
                            PESEL: {r.pesel_pacjenta}
                          </p>
                        </div>
                        <ReceptaStatusBadge status={r.status_recepty} />
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              {/* Zamówienia */}
              <div className="space-y-3">
                <h4 className="text-sm font-bold text-slate-900">
                  Zamówienia ({zamowienia.length})
                </h4>
                {zamowienia.length === 0 ? (
                  <p className="text-xs text-slate-400">Brak zamówień.</p>
                ) : (
                  <ul className="space-y-2">
                    {zamowienia.map((z) => (
                      <li
                        key={z.id_zamowienia}
                        className="p-3 rounded-xl bg-slate-50 border border-slate-200/70 text-xs space-y-1.5"
                      >
                        <div className="flex items-center justify-between gap-2">
                          <Link
                            to={`/zamowienia/${z.id_zamowienia}`}
                            className="font-mono font-bold text-teal-700 hover:underline"
                          >
                            Zamówienie #{z.id_zamowienia} →
                          </Link>
                          <span className="font-semibold text-slate-900">
                            {zl(z.suma_calkowita)}
                          </span>
                        </div>
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-slate-500">
                            {formatDatePl(z.data_zamowienia)}
                          </span>
                          <OrderStatusBadge status={z.status_zamowienia} />
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
