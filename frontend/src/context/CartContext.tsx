import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { Lek } from '../types/lek';

export interface CartItem {
  lek: Lek;
  ilosc: number;
}

interface CartValue {
  items: CartItem[];
  count: number;
  total: number;
  add: (lek: Lek, ilosc?: number) => void;
  setQuantity: (id: number, ilosc: number) => void;
  remove: (id: number) => void;
  clear: () => void;
}

const CartContext = createContext<CartValue | null>(null);
const STORAGE_KEY = 'koszyk';

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
      total: items.reduce((s, i) => s + i.ilosc * i.lek.cena, 0),
      add: (lek, ilosc = 1) =>
        setItems((prev) => {
          const existing = prev.find((i) => i.lek.lek_id === lek.lek_id);
          const qty = Math.min((existing?.ilosc ?? 0) + ilosc, lek.dostepne ?? 0);
          if (qty <= 0) return prev;
          return existing
            ? prev.map((i) => (i.lek.lek_id === lek.lek_id ? { lek, ilosc: qty } : i))
            : [...prev, { lek, ilosc: qty }];
        }),
      setQuantity: (id, ilosc) =>
        setItems((prev) =>
          prev.map((i) =>
            i.lek.lek_id === id
              ? { ...i, ilosc: Math.max(1, Math.min(ilosc, i.lek.dostepne ?? 1)) }
              : i,
          ),
        ),
      remove: (id) => setItems((prev) => prev.filter((i) => i.lek.lek_id !== id)),
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