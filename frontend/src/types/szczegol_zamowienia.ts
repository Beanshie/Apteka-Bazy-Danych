export interface SzczegolZamowienia {
  id_szczegolu: number;
  id_zamowienia: number;
  id_produktu: number;
  id_recepty: number | null;
  ilosc: number;
  cena_jednostkowa: number;
}

export interface SzczegolZamowieniaCreate {
  id_produktu: number;
  id_recepty?: number | null;
  ilosc?: number;
}

export interface SzczegolZamowieniaUpdate {
  id_produktu: number;
  id_recepty?: number | null;
  ilosc: number;
}
