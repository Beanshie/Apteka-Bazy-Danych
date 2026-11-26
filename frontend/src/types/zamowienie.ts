import type {
  SzczegolZamowienia,
  SzczegolZamowieniaCreate,
  SzczegolZamowieniaUpdate,
} from './szczegol_zamowienia';

export type {
  SzczegolZamowienia,
  SzczegolZamowieniaCreate,
  SzczegolZamowieniaUpdate,
};

export type StatusZamowienia =
  | 'NOWE'
  | 'OPLACONE'
  | 'W_REALIZACJI'
  | 'WYSLANE'
  | 'ZREALIZOWANE'
  | 'ANULOWANE';

export interface Zamowienie {
  id_zamowienia: number;
  id_klienta: number;
  id_adresu: number;
  data_zamowienia: string;
  status_zamowienia: StatusZamowienia;
  suma_calkowita: number;
}

export interface ZamowienieCreate {
  id_klienta: number;
  id_adresu: number;
  status_zamowienia?: StatusZamowienia;
}

export interface ZamowienieUpdate {
  id_klienta: number;
  id_adresu: number;
  status_zamowienia: StatusZamowienia;
}
