export interface AdresKlienta {
  id_adresu: number;
  id_klienta: number;
  ulica: string;
  numer_domu: string;
  numer_mieszkania: string | null;
  kod_pocztowy: string;
  miasto: string;
}

export interface AdresKlientaCreate {
  id_klienta: number;
  ulica: string;
  numer_domu: string;
  numer_mieszkania?: string | null;
  kod_pocztowy: string;
  miasto: string;
}

export interface AdresKlientaUpdate {
  id_klienta: number;
  ulica: string;
  numer_domu: string;
  numer_mieszkania?: string | null;
  kod_pocztowy: string;
  miasto: string;
}
