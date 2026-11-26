export type RolaPracownika = 'FARMACEUTA' | 'KIEROWNIK' | 'ADMIN';

export interface Pracownik {
  id_pracownika: number;
  login: string;
  imie: string;
  nazwisko: string;
  rola: RolaPracownika;
  aktywny: boolean;
}

export interface PracownikCreate {
  login: string;
  haslo: string;
  imie: string;
  nazwisko: string;
  rola?: RolaPracownika;
  aktywny?: boolean;
}

export interface PracownikUpdate {
  login: string;
  haslo?: string | null;
  imie: string;
  nazwisko: string;
  rola: RolaPracownika;
  aktywny: boolean;
}
