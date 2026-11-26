export type StatusRecepty = 'NOWA' | 'ZWERYFIKOWANA' | 'ZREALIZOWANA' | 'ODRZUCONA';

export interface Recepta {
  id_recepty: number;
  id_klienta: number;
  kod_recepty: string;
  pesel_pacjenta: string;
  status_recepty: StatusRecepty;
}

export type ReceptaCreate = Omit<Recepta, 'id_recepty' | 'status_recepty'> &
  Partial<Pick<Recepta, 'status_recepty'>>;
export type ReceptaUpdate = Omit<Recepta, 'id_recepty'>;
