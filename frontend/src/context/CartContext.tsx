import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { Produkt } from '../types/produkt';

export interface CartItem {
  produkt: Produkt;
  ilosc: number;
}

interface CartValue {
  items: CartItem[];
  count: number;
  total: number;
  add: (produkt: Produkt, ilosc?: number) => void;
  setQuantity: (id: number, ilosc: number) => void;
  remove: (id: number) => void;
  clear: () => void;
}

const CartContext = createContext<CartValue | null>(null);
const STORAGE_KEY = 'koszyk-produkty';

export const CartProvider = ({ children }: { children: ReactNode }) => {
  const [items, setItems] = useState<CartItem[]>(() => {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]');
    } catch {
      return [];
    }
  });

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  }, [items]);

  const value = useMemo<CartValue>(
    () => ({
      items,
      count: items.reduce((s, i) => s + i.ilosc, 0),
      total: items.reduce((s, i) => s + i.ilosc * i.produkt.cena, 0),
      add: (produkt, ilosc = 1) =>
        setItems((prev) => {
          const existing = prev.find((i) => i.produkt.id_produktu === produkt.id_produktu);
          const qty = Math.min((existing?.ilosc ?? 0) + ilosc, produkt.stan_magazynowy ?? 0);
          if (qty <= 0) return prev;
          return existing
            ? prev.map((i) => (i.produkt.id_produktu === produkt.id_produktu ? { produkt, ilosc: qty } : i))
            : [...prev, { produkt, ilosc: qty }];
        }),
      setQuantity: (id, ilosc) =>
        setItems((prev) =>
          prev.map((i) =>
            i.produkt.id_produktu === id
              ? { ...i, ilosc: Math.max(1, Math.min(ilosc, i.produkt.stan_magazynowy ?? 1)) }
              : i,
          ),
        ),
      remove: (id) => setItems((prev) => prev.filter((i) => i.produkt.id_produktu !== id)),
      clear: () => setItems([]),
    }),
    [items],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
};

// eslint-disable-next-line react-refresh/only-export-components
export const useCart = () => {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart musi być użyty wewnątrz CartProvider');
  return ctx;
};