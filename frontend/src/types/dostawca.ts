export interface Dostawca {
  id_dostawcy: number;
  nazwa_firmy: string;
  nip: string | null;
  telefon: string | null;
  email: string | null;
}

export interface DostawcaCreate {
  nazwa_firmy: string;
  nip?: string | null;
  telefon?: string | null;
  email?: string | null;
}

export interface DostawcaUpdate {
  nazwa_firmy: string;
  nip?: string | null;
  telefon?: string | null;
  email?: string | null;
}
