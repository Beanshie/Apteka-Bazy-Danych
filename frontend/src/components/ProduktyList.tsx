import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import type { Produkt } from '../types/produkt';
import type { Kategoria } from '../types/kategoria';
import { produktyService } from '../services/produktyService';
import { kategorieService } from '../services/kategorieService';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { RxBadge, StockBadge } from './Badges';
import { zl } from '../utils/format';

type RxFilter = 'all' | 'rx' | 'otc';

const CategoryIcon = ({ categoryName }: { categoryName?: string }) => {
  const name = (categoryName ?? '').toLowerCase();

  // Witaminy i suplementy — liść
  if (name.includes('witamin') || name.includes('suplement')) {
    return (
      <svg className="w-12 h-12 text-teal-600/80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
          d="M5 21c.5-4.5 2.5-8 7-10m0 0c4.5-2 7-5 7-8-4.5 0-8.5 1.5-11 4.5S5 14 5 21z"
        />
      </svg>
    );
  }

  // Okulistyka — kropla / oko
  if (name.includes('okulist') || name.includes('ocz')) {
    return (
      <svg className="w-12 h-12 text-sky-600/80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
          d="M12 3.5c-3.2 4.5-6 7.5-6 11a6 6 0 1012 0c0-3.5-2.8-6.5-6-11z"
        />
      </svg>
    );
  }

  // Dermatologia — plaster / apteczka
  if (name.includes('dermatolog') || name.includes('maś') || name.includes('skór')) {
    return (
      <svg className="w-12 h-12 text-amber-600/80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
          d="M7.5 4.5l12 12a3.5 3.5 0 01-5 5l-12-12a3.5 3.5 0 015-5zm2 6l4 4m-1-5l-4 4"
        />
      </svg>
    );
  }

  // Układ pokarmowy / syropy — butelka
  if (name.includes('pokarm') || name.includes('syrop') || name.includes('traw')) {
    return (
      <svg className="w-12 h-12 text-emerald-600/80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
          d="M9 3h6m-5 0v3.2a2 2 0 01-.59 1.41L7.59 9.41A2 2 0 007 10.83V19a2 2 0 002 2h6a2 2 0 002-2v-8.17a2 2 0 00-.59-1.42l-1.82-1.8A2 2 0 0114 6.2V3m-4 11h4m-2-2v4"
        />
      </svg>
    );
  }

  // Antybiotyki / przeciwbólowe — kapsułka / tabletka
  if (name.includes('przeciwból') || name.includes('antybiot')) {
    return (
      <svg className="w-12 h-12 text-indigo-600/80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
          d="M10.5 6.5l7 7a4.95 4.95 0 11-7 7l-7-7a4.95 4.95 0 017-7zm-3.5 3.5l7 7"
        />
      </svg>
    );
  }

  // Domyślna ikona leku (krzyżyk apteczny)
  return (
    <svg className="w-12 h-12 text-teal-600/75" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.5"
        d="M9 4h6v5h5v6h-5v5H9v-5H4V9h5V4z"
      />
    </svg>
  );
};

export const ProduktyList: React.FC = () => {
  const { items, add } = useCart();
  const { user } = useAuth();

  const [produkty, setProdukty] = useState<Produkt[]>([]);
  const [kategorie, setKategorie] = useState<Kategoria[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const [selectedKategoria, setSelectedKategoria] = useState<string>('');
  const [rxFilter, setRxFilter] = useState<RxFilter>('all');
  const [onlyAvailable, setOnlyAvailable] = useState(false);
  const [addedIds, setAddedIds] = useState<Record<number, boolean>>({});
  const timersRef = useRef<Record<number, ReturnType<typeof setTimeout>>>({});

  const isEmployee = user?.typ_konta === 'PRACOWNIK';

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [produktyData, kategorieData] = await Promise.all([
        produktyService.getAll(),
        kategorieService.getAll().catch(() => [] as Kategoria[]),
      ]);
      setProdukty(produktyData);
      setKategorie(kategorieData);
    } catch (err) {
      console.error(err);
      setProdukty([]);
      setError(err instanceof Error ? err.message : 'Nieznany błąd');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    const timers = timersRef.current;
    return () => {
      Object.values(timers).forEach(clearTimeout);
    };
  }, []);

  const kategorieMap = useMemo(() => {
    const map: Record<number, Kategoria> = {};
    for (const k of kategorie) {
      map[k.id_kategorii] = k;
    }
    return map;
  }, [kategorie]);

  const cartQtyMap = useMemo(() => {
    const map: Record<number, number> = {};
    for (const item of items) {
      map[item.produkt.id_produktu] = item.ilosc;
    }
    return map;
  }, [items]);

  const handleAddToCart = (produkt: Produkt) => {
    add(produkt, 1);
    const id = produkt.id_produktu;
    setAddedIds((prev) => ({ ...prev, [id]: true }));
    if (timersRef.current[id]) {
      clearTimeout(timersRef.current[id]);
    }
    timersRef.current[id] = setTimeout(() => {
      setAddedIds((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
    }, 1500);
  };

  const hasActiveFilters =
    query.trim() !== '' ||
    selectedKategoria !== '' ||
    rxFilter !== 'all' ||
    onlyAvailable;

  const clearFilters = () => {
    setQuery('');
    setSelectedKategoria('');
    setRxFilter('all');
    setOnlyAvailable(false);
  };

  const filtered = useMemo(() => {
    const q = query.toLowerCase().trim();
    const kategoriaId = selectedKategoria ? Number(selectedKategoria) : null;

    return produkty.filter((p) => {
      if (q) {
        const kat = kategorieMap[p.id_kategorii];
        const katText = `${kat?.nazwa_kategorii ?? ''} ${kat?.opis ?? ''}`.toLowerCase();
        const match =
          p.nazwa_produktu?.toLowerCase().includes(q) || katText.includes(q);
        if (!match) return false;
      }
      if (kategoriaId !== null && p.id_kategorii !== kategoriaId) {
        return false;
      }
      if (rxFilter === 'rx' && !p.wymaga_recepty) {
        return false;
      }
      if (rxFilter === 'otc' && p.wymaga_recepty) {
        return false;
      }
      if (onlyAvailable && (p.stan_magazynowy ?? 0) <= 0) {
        return false;
      }
      return true;
    });
  }, [produkty, kategorieMap, query, selectedKategoria, rxFilter, onlyAvailable]);

  return (
    <div className="space-y-6">
      {/* Wyszukiwarka i filtry */}
      <div className="bg-white rounded-2xl p-4 sm:p-6 shadow-sm border border-slate-200/80 space-y-4">
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Szukaj produktu po nazwie..."
            className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 transition-all placeholder:text-slate-400"
          />
        </div>

        <div className="flex flex-wrap items-center gap-3 pt-3 border-t border-slate-100">
          <select
            aria-label="Filtruj według kategorii"
            value={selectedKategoria}
            onChange={(e) => setSelectedKategoria(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
          >
            <option value="">Wszystkie kategorie</option>
            {kategorie.map((k) => (
              <option key={k.id_kategorii} value={k.id_kategorii}>
                {k.nazwa_kategorii}
              </option>
            ))}
          </select>

          <select
            aria-label="Filtruj według typu recepty"
            value={rxFilter}
            onChange={(e) => setRxFilter(e.target.value as RxFilter)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
          >
            <option value="all">Wszystkie (Rx i OTC)</option>
            <option value="rx">Na receptę (Rx)</option>
            <option value="otc">Bez recepty (OTC)</option>
          </select>

          <label className="inline-flex items-center gap-2 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-700 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={onlyAvailable}
              onChange={(e) => setOnlyAvailable(e.target.checked)}
              className="accent-teal-600 rounded"
            />
            <span>Tylko dostępne</span>
          </label>

          {hasActiveFilters && (
            <button
              type="button"
              onClick={clearFilters}
              className="px-3 py-2 text-xs font-semibold text-slate-600 hover:text-rose-600 rounded-xl border border-slate-200 hover:bg-slate-50 transition-all"
            >
              Wyczyść filtry
            </button>
          )}

          <span className="ml-auto text-xs text-slate-400">
            Wyświetlono {filtered.length} z {produkty.length}
          </span>
        </div>
      </div>

      {/* Błąd */}
      {error && (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-5 text-rose-900">
          <h3 className="font-semibold text-rose-950">
            Nie udało się pobrać listy produktów
          </h3>
          <p className="text-xs sm:text-sm text-rose-800 mt-1">{error}</p>
        </div>
      )}

      {/* Siatka kart produktów */}
      {loading ? (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 py-24 flex flex-col items-center justify-center gap-3">
          <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
          <p className="text-sm font-medium text-slate-600">Pobieranie listy produktów...</p>
        </div>
      ) : filtered.length === 0 ? (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 py-16 text-center space-y-2">
          <p className="text-slate-700 font-medium">Brak produktów do wyświetlenia</p>
          <p className="text-xs text-slate-400">
            Spróbuj zmienić wyszukiwaną frazę lub wyczyścić aktywne filtry.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {filtered.map((produkt) => {
            const kat = kategorieMap[produkt.id_kategorii];
            const stock = produkt.stan_magazynowy ?? 0;
            const inCart = cartQtyMap[produkt.id_produktu] ?? 0;
            const isJustAdded = Boolean(addedIds[produkt.id_produktu]);
            const canAddMore = stock > 0 && inCart < stock;

            return (
              <article
                key={produkt.id_produktu}
                className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden flex flex-col hover:shadow-md hover:border-slate-300/80 transition-all"
              >
                {/* Góra karty: obszar wizualny z ikoną kategorii + RxBadge */}
                <Link
                  to={`/produkty/${produkt.id_produktu}`}
                  className="relative h-36 bg-gradient-to-br from-slate-50 to-teal-50/60 flex items-center justify-center border-b border-slate-100 group"
                >
                  <div className="transform group-hover:scale-105 transition-transform duration-200">
                    <CategoryIcon categoryName={kat?.nazwa_kategorii} />
                  </div>
                  {produkt.wymaga_recepty && (
                    <div className="absolute top-3 right-3">
                      <RxBadge required={true} />
                    </div>
                  )}
                </Link>

                {/* Środek karty: kategoria, nazwa, dostępność */}
                <div className="p-4 flex-1 flex flex-col gap-2">
                  <p className="text-[11px] font-medium uppercase tracking-wider text-slate-400 truncate">
                    {kat ? kat.nazwa_kategorii : 'Produkt apteczny'}
                  </p>

                  <Link
                    to={`/produkty/${produkt.id_produktu}`}
                    className="font-semibold text-slate-900 hover:text-teal-700 leading-snug line-clamp-2 transition-colors"
                  >
                    {produkt.nazwa_produktu}
                  </Link>

                  <div className="pt-1 mt-auto">
                    <StockBadge qty={stock} />
                  </div>
                </div>

                {/* Dół karty: cena + przycisk Dodaj do koszyka */}
                <div className="px-4 pb-4 pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                  <span className="text-lg font-bold text-slate-900 whitespace-nowrap">
                    {zl(produkt.cena)}
                  </span>

                  {!isEmployee && (
                    <button
                      type="button"
                      disabled={!canAddMore && !isJustAdded}
                      onClick={() => handleAddToCart(produkt)}
                      className={`px-3 py-2 rounded-xl text-xs font-semibold transition-all whitespace-nowrap ${
                        isJustAdded
                          ? 'bg-emerald-600 text-white'
                          : 'bg-teal-600 hover:bg-teal-700 active:bg-teal-800 disabled:bg-slate-200 disabled:text-slate-400 disabled:cursor-not-allowed text-white shadow-sm shadow-teal-600/20'
                      }`}
                    >
                      {isJustAdded
                        ? 'Dodano ✓'
                        : stock <= 0
                          ? 'Niedostępny'
                          : inCart >= stock
                            ? 'Maks. w koszyku'
                            : 'Dodaj do koszyka'}
                    </button>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
};
