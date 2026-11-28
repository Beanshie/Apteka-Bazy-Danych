import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import axios from 'axios';
import type { Produkt } from '../types/produkt';
import type { Kategoria } from '../types/kategoria';
import { produktyService } from '../services/produktyService';
import { kategorieService } from '../services/kategorieService';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { RxBadge, StockBadge } from './Badges';
import { CategoryIcon } from './CategoryIcon';
import { zl } from '../utils/format';

export const ProduktDetails: React.FC = () => {
  const { id } = useParams();
  const { items, add } = useCart();
  const { user } = useAuth();

  const [produkt, setProdukt] = useState<Produkt | null>(null);
  const [kategoria, setKategoria] = useState<Kategoria | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [qty, setQty] = useState(1);
  const [added, setAdded] = useState(false);

  const isEmployee = user?.typ_konta === 'PRACOWNIK';

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    setKategoria(null);
    setQty(1);
    setAdded(false);

    produktyService
      .getById(Number(id))
      .then(async (data) => {
        if (!active) return;
        setProdukt(data);
        const kat = await kategorieService.getById(data.id_kategorii).catch(() => null);
        if (!active) return;
        setKategoria(kat);
      })
      .catch((err) => {
        if (!active) return;
        setError(
          axios.isAxiosError(err) && err.response?.status === 404
            ? 'Nie znaleziono wybranego produktu.'
            : 'Nie udało się pobrać danych produktu.',
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [id]);

  if (loading) {
    return (
      <div className="py-24 flex flex-col items-center gap-3">
        <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
        <p className="text-sm font-medium text-slate-600">Ładowanie produktu...</p>
      </div>
    );
  }

  if (error || !produkt) {
    return (
      <div className="space-y-4">
        <Link to="/" className="text-sm text-slate-500 hover:text-teal-700">
          ← Wróć do katalogu produktów
        </Link>
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-5 text-rose-900 text-sm">
          {error ?? 'Nie znaleziono produktu.'}
        </div>
      </div>
    );
  }

  const stock = produkt.stan_magazynowy ?? 0;
  const inCart = items.find((i) => i.produkt.id_produktu === produkt.id_produktu)?.ilosc ?? 0;
  const maxQty = Math.max(stock - inCart, 0);
  const q = Math.min(qty, Math.max(maxQty, 1));

  const handleAdd = () => {
    add(produkt, q);
    setAdded(true);
    setQty(1);
  };

  return (
    <div className="space-y-6">
      <Link
        to="/"
        className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-teal-700 transition-colors"
      >
        ← Wróć do katalogu produktów
      </Link>

      {/* Główna karta produktu */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6 sm:p-8 grid md:grid-cols-2 gap-8 items-start">
        <div className="aspect-square max-h-80 w-full rounded-2xl bg-gradient-to-br from-slate-50 to-teal-50/70 border border-slate-100 flex items-center justify-center">
          <CategoryIcon categoryName={kategoria?.nazwa_kategorii} className="w-28 h-28" />
        </div>

        <div className="space-y-5">
          {kategoria && (
            <p className="text-xs font-semibold uppercase tracking-wider text-teal-700">
              {kategoria.nazwa_kategorii}
            </p>
          )}

          <h2 className="text-2xl font-bold text-slate-900 leading-tight">
            {produkt.nazwa_produktu}
          </h2>

          <div className="flex flex-wrap gap-2">
            <RxBadge required={Boolean(produkt.wymaga_recepty)} />
            <StockBadge qty={stock} />
          </div>

          <div className="pt-4 border-t border-slate-100">
            <p className="text-3xl font-bold text-slate-900">{zl(produkt.cena)}</p>
            <p className="text-xs text-slate-400 mt-1">cena brutto za 1 opakowanie</p>
          </div>

          {produkt.wymaga_recepty && (
            <p className="text-xs text-purple-700 bg-purple-50 border border-purple-200 rounded-xl p-3">
              Ten produkt jest wydawany wyłącznie z przepisu lekarza (wymagana ważna e-recepta).
            </p>
          )}

          {!isEmployee && (
            <div className="space-y-3 pt-2">
              <div className="flex items-center gap-3">
                <span className="text-sm text-slate-600">Ilość:</span>
                <div className="inline-flex items-center border border-slate-200 rounded-xl overflow-hidden">
                  <button
                    type="button"
                    onClick={() => setQty(Math.max(1, q - 1))}
                    disabled={maxQty === 0 || q <= 1}
                    className="w-9 h-9 text-lg text-slate-600 hover:bg-slate-50 disabled:opacity-40"
                  >
                    −
                  </button>
                  <span className="w-10 text-center text-sm font-semibold">
                    {maxQty === 0 ? 0 : q}
                  </span>
                  <button
                    type="button"
                    onClick={() => setQty(Math.min(maxQty, q + 1))}
                    disabled={maxQty === 0 || q >= maxQty}
                    className="w-9 h-9 text-lg text-slate-600 hover:bg-slate-50 disabled:opacity-40"
                  >
                    +
                  </button>
                </div>
              </div>

              <button
                type="button"
                onClick={handleAdd}
                disabled={maxQty === 0}
                className="w-full inline-flex items-center justify-center gap-2 px-4 py-3 bg-teal-600 hover:bg-teal-700 active:bg-teal-800 disabled:bg-slate-200 disabled:text-slate-400 disabled:cursor-not-allowed text-white font-semibold text-sm rounded-xl transition-all shadow-sm shadow-teal-600/20"
              >
                {stock === 0
                  ? 'Produkt niedostępny'
                  : maxQty === 0
                    ? 'Maksymalna ilość w koszyku'
                    : 'Dodaj do koszyka'}
              </button>

              {added && (
                <div className="flex items-center justify-between text-sm bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl px-4 py-3">
                  <span>Dodano do koszyka ✓</span>
                  <Link to="/koszyk" className="font-semibold underline hover:text-emerald-900">
                    Przejdź do koszyka
                  </Link>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Informacje o produkcie (tylko realnie dostępne dane opisowe) */}
      {kategoria?.opis && (
        <section className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6 space-y-2">
          <h3 className="text-base font-bold text-slate-900">Informacje o produkcie</h3>
          <p className="text-sm text-slate-600 leading-relaxed">{kategoria.opis}</p>
        </section>
      )}
    </div>
  );
};
