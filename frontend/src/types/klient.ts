export interface Klient {
  id_klienta: number;
  imie: string;
  nazwisko: string;
  email: string;
  telefon: string | null;
  data_rejestracji: string;
}

export interface KlientCreate {
  imie: string;
  nazwisko: string;
  email: string;
  haslo: string;
  telefon?: string | null;
}

export interface KlientUpdate {
  imie: string;
  nazwisko: string;
  email: string;
  haslo?: string | null;
  telefon?: string | null;
}
