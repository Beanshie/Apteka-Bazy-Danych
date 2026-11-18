export type StatusPlatnosci = 'OCZEKUJACA' | 'OPLACONA' | 'NIEUDANA' | 'ZWROCONA';

export interface Platnosc {
  id_platnosci: number;
  id_zamowienia: number;
  metoda_platnosci: string;
  status_platnosci: StatusPlatnosci;
  data_platnosci: string | null;
}

export interface PlatnoscCreate {
  id_zamowienia: number;
  metoda_platnosci: string;
  status_platnosci?: StatusPlatnosci;
  data_platnosci?: string | null;
}

export interface PlatnoscUpdate {
  id_zamowienia: number;
  metoda_platnosci: string;
  status_platnosci: StatusPlatnosci;
  data_platnosci?: string | null;
}
