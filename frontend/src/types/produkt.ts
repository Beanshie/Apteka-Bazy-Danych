export interface Produkt {
  id_produktu: number;
  id_kategorii: number;
  id_dostawcy: number;
  nazwa_produktu: string;
  cena: number;
  stan_magazynowy: number;
  wymaga_recepty: boolean;
}

export type ProduktCreate = Omit<Produkt, 'id_produktu' | 'stan_magazynowy' | 'wymaga_recepty'> &
  Partial<Pick<Produkt, 'stan_magazynowy' | 'wymaga_recepty'>>;
export type ProduktUpdate = Omit<Produkt, 'id_produktu'>;
