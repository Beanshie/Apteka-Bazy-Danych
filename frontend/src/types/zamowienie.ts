export type StatusZamowienia =
  | 'NOWE'
  | 'OPLACONE'
  | 'W_REALIZACJI'
  | 'WYSLANE'
  | 'ZREALIZOWANE'
  | 'ANULOWANE';

export interface SzczegolZamowienia {
  id_szczegolu: number;
  id_zamowienia: number;
  id_produktu: number;
  id_recepty: number | null;
  ilosc: number;
  cena_jednostkowa: number;
}

export type SzczegolZamowieniaCreate = Pick<SzczegolZamowienia, 'id_produktu'> &
  Partial<Pick<SzczegolZamowienia, 'id_recepty' | 'ilosc'>>;

export type SzczegolZamowieniaUpdate = Pick<SzczegolZamowienia, 'id_produktu' | 'ilosc'> &
  Partial<Pick<SzczegolZamowienia, 'id_recepty'>>;

export interface Zamowienie {
  id_zamowienia: number;
  id_klienta: number;
  id_adresu: number;
  data_zamowienia: string;
  status_zamowienia: StatusZamowienia;
  suma_calkowita: number;
  pozycje: SzczegolZamowienia[];
}

export type ZamowienieCreate = Omit<
  Zamowienie,
  'id_zamowienia' | 'data_zamowienia' | 'status_zamowienia' | 'suma_calkowita' | 'pozycje'
> &
  Partial<Pick<Zamowienie, 'status_zamowienia'>> & {
    pozycje: SzczegolZamowieniaCreate[];
  };

export type ZamowienieUpdate = Omit<
  Zamowienie,
  'id_zamowienia' | 'data_zamowienia' | 'suma_calkowita' | 'pozycje'
> & {
  pozycje: SzczegolZamowieniaCreate[];
};
