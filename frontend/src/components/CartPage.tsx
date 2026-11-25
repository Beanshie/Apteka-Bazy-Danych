import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { zl } from '../utils/format';

export const CartPage: React.FC = () => {
  const { items, count, total, setQuantity, remove, clear } = useCart();

  if (items.length === 0) {
    return (
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 py-16 text-center">
        <p className="text-slate-700 font-medium">Twój koszyk jest pusty</p>
        <Link to="/" className="inline-block mt-4 text-sm font-medium text-teal-700 hover:underline">
          Wróć do listy produktów
        </Link>
      </div>
    );
  }

  return (
    <div className="grid lg:grid-cols-[1fr_320px] gap-6 items-start">
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 divide-y divide-slate-100">
        <div className="px-6 py-4 flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-900">Koszyk ({count})</h2>
          <button onClick={clear} className="text-xs text-slate-500 hover:text-rose-600">
            Wyczyść koszyk
          </button>
        </div>

        {items.map(({ produkt, ilosc }) => (
          <div key={produkt.id_produktu} className="px-6 py-4 flex flex-wrap items-center gap-4">
            <div className="flex-1 min-w-[180px]">
              <Link to={`/produkty/${produkt.id_produktu}`} className="font-semibold text-slate-900 hover:text-teal-700">
                {produkt.nazwa_produktu}
              </Link>
              <p className="text-xs text-slate-400 mt-0.5">{zl(produkt.cena)} / szt.</p>
            </div>

            <div className="inline-flex items-center border border-slate-200 rounded-xl overflow-hidden">
              <button
                onClick={() => setQuantity(produkt.id_produktu, ilosc - 1)}
                disabled={ilosc <= 1}
                className="w-8 h-8 text-slate-600 hover:bg-slate-50 disabled:opacity-40"
              >
                −
              </button>
              <span className="w-9 text-center text-sm font-semibold">{ilosc}</span>
              <button
                onClick={() => setQuantity(produkt.id_produktu, ilosc + 1)}
                disabled={ilosc >= (produkt.stan_magazynowy ?? 0)}
                className="w-8 h-8 text-slate-600 hover:bg-slate-50 disabled:opacity-40"
              >
                +
              </button>
            </div>

            <p className="w-24 text-right font-semibold text-slate-900">{zl(ilosc * produkt.cena)}</p>

            <button
              onClick={() => remove(produkt.id_produktu)}
              className="text-xs text-slate-400 hover:text-rose-600"
              aria-label="Usuń z koszyka"
            >
              Usuń
            </button>
          </div>
        ))}
      </div>

      <aside className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6 space-y-4 lg:sticky lg:top-24">
        <h3 className="text-lg font-bold text-slate-900">Podsumowanie</h3>
        <div className="flex justify-between text-sm text-slate-600">
          <span>Liczba produktów</span>
          <span>{count}</span>
        </div>
        <div className="flex justify-between pt-3 border-t border-slate-100">
          <span className="font-medium text-slate-700">Razem</span>
          <span className="text-xl font-bold text-slate-900">{zl(total)}</span>
        </div>
      </aside>
    </div>
  );
};