import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import axios from 'axios';
import type { Lek } from '../types/lek';
import { lekiService } from '../services/lekiService';
import { useCart } from '../context/CartContext';
import { RxBadge, StockBadge } from './Badges';
import { zl } from '../utils/format';

export const LekDetails: React.FC = () => {
  const { id } = useParams();
  const { items, add } = useCart();
  const [lek, setLek] = useState<Lek | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [qty, setQty] = useState(1);
  const [added, setAdded] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError(null);
    setQty(1);
    setAdded(false);
    lekiService
      .getById(Number(id))
      .then(setLek)
      .catch((err) =>
        setError(
          axios.isAxiosError(err) && err.response?.status === 404
            ? 'Nie znaleziono leku o podanym ID.'
            : 'Nie udało się pobrać danych leku.',
        ),
      )
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="py-24 flex flex-col items-center gap-3">
        <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
        <p className="text-sm font-medium text-slate-600">Ładowanie produktu...</p>
      </div>
    );
  }

  if (error || !lek) {
    return (
      <div className="space-y-4">
        <Link to="/" className="text-sm text-slate-500 hover:text-teal-700">← Wróć do listy leków</Link>
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-5 text-rose-900 text-sm">
          {error ?? 'Nie znaleziono leku.'}
        </div>
      </div>
    );
  }

  const stock = lek.dostepne ?? 0;
  const inCart = items.find((i) => i.lek.lek_id === lek.lek_id)?.ilosc ?? 0;
  const maxQty = Math.max(stock - inCart, 0);
  const q = Math.min(qty, Math.max(maxQty, 1));

  const handleAdd = () => {
    add(lek, q);
    setAdded(true);
    setQty(1);
  };

  return (
    <div className="space-y-6">
      <Link to="/" className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-teal-700 transition-colors">
        ← Wróć do listy leków
      </Link>

      {/* Główna karta produktu */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6 grid md:grid-cols-2 gap-8">
        <div className="aspect-square rounded-xl bg-gradient-to-br from-teal-50 to-slate-100 flex items-center justify-center text-teal-600/60">
          <svg className="w-28 h-28" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
          </svg>
        </div>

        <div className="space-y-5">
          <p className="text-xs font-mono text-slate-400">ID produktu: #{lek.lek_id}</p>
          <h2 className="text-2xl font-bold text-slate-900 leading-tight">{lek.nazwa}</h2>

          <div className="flex flex-wrap gap-2">
            <RxBadge required={!!lek.wymagana_Recepta} />
            <StockBadge qty={stock} />
          </div>

          <div className="pt-4 border-t border-slate-100">
            <p className="text-3xl font-bold text-slate-900">{zl(lek.cena)}</p>
            <p className="text-xs text-slate-400 mt-1">cena brutto za 1 opakowanie</p>
          </div>

          {lek.wymagana_Recepta && (
            <p className="text-xs text-purple-700 bg-purple-50 border border-purple-200 rounded-lg p-3">
              Ten lek jest wydawany wyłącznie z przepisu lekarza.
            </p>
          )}

          {/* Ilość + koszyk */}
          <div className="space-y-3 pt-2">
            <div className="flex items-center gap-3">
              <span className="text-sm text-slate-600">Ilość:</span>
              <div className="inline-flex items-center border border-slate-200 rounded-xl overflow-hidden">
                <button
                  onClick={() => setQty(Math.max(1, q - 1))}
                  disabled={maxQty === 0 || q <= 1}
                  className="w-9 h-9 text-lg text-slate-600 hover:bg-slate-50 disabled:opacity-40"
                >
                  −
                </button>
                <span className="w-10 text-center text-sm font-semibold">{maxQty === 0 ? 0 : q}</span>
                <button
                  onClick={() => setQty(Math.min(maxQty, q + 1))}
                  disabled={maxQty === 0 || q >= maxQty}
                  className="w-9 h-9 text-lg text-slate-600 hover:bg-slate-50 disabled:opacity-40"
                >
                  +
                </button>
              </div>
              <span className="text-xs text-slate-400">z {stock} szt.</span>
            </div>

            <button
              onClick={handleAdd}
              disabled={maxQty === 0}
              className="w-full inline-flex items-center justify-center gap-2 px-4 py-3 bg-teal-600 hover:bg-teal-700 active:bg-teal-800 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-semibold text-sm rounded-xl transition-all shadow-sm shadow-teal-600/20"
            >
              {stock === 0
                ? 'Produkt niedostępny'
                : maxQty === 0
                  ? 'Cały dostępny zapas w koszyku'
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
        </div>
      </div>

      {/* Opis */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6">
        <h3 className="text-lg font-bold text-slate-900 mb-3">Opis produktu</h3>
        <p className="text-sm text-slate-600 leading-relaxed whitespace-pre-line">
          {lek.opis || 'Brak opisu.'}
        </p>
      </div>

      {/* Specyfikacja */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6">
        <h3 className="text-lg font-bold text-slate-900 mb-3">Parametry</h3>
        <dl className="text-sm divide-y divide-slate-100">
          {[
            ['Nazwa', lek.nazwa],
            ['Cena', zl(lek.cena)],
            ['Dostępność', `${stock} szt.`],
            ['Wydawany z receptą', lek.wymagana_Recepta ? 'Tak' : 'Nie'],
            ['ID', `#${lek.lek_id}`],
          ].map(([k, v]) => (
            <div key={k} className="grid grid-cols-[200px_1fr] py-2.5">
              <dt className="text-slate-500">{k}</dt>
              <dd className="font-medium text-slate-900">{v}</dd>
            </div>
          ))}
        </dl>
      </div>
    </div>
  );
};