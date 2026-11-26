export interface Kategoria {
  id_kategorii: number;
  nazwa_kategorii: string;
  opis: string | null;
}

export interface KategoriaCreate {
  nazwa_kategorii: string;
  opis?: string | null;
}

export interface KategoriaUpdate {
  nazwa_kategorii: string;
  opis?: string | null;
}
