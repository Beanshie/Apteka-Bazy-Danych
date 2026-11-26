export type TypKonta = 'KLIENT' | 'PRACOWNIK';

export interface AuthLogin {
  typ_konta: TypKonta;
  identyfikator: string;
  haslo: string;
}

export interface AuthUser {
  id: number;
  typ_konta: TypKonta;
  identyfikator: string;
  imie: string;
  nazwisko: string;
  rola: string | null;
}

export interface AuthCsrf {
  token: string;
  headerName: string;
}
