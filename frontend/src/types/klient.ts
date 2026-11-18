export interface Klient {
  id_klienta: number;
  imie: string;
  nazwisko: string;
  email: string;
  telefon: string | null;
  data_rejestracji: string;
}

export type KlientCreate = Omit<Klient, 'id_klienta' | 'telefon' | 'data_rejestracji'> &
  Partial<Pick<Klient, 'telefon'>> & {
    haslo: string;
  };

export type KlientUpdate = Omit<Klient, 'id_klienta' | 'telefon' | 'data_rejestracji'> &
  Partial<Pick<Klient, 'telefon'>> & {
    haslo?: string | null;
  };
