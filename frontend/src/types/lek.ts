export interface Lek {
  lek_id: number;
  nazwa: string;
  cena: number;
  opis: string;
  wymagana_Recepta?: boolean;
  dostepne?: number;
}

export type DaneLek = Omit<Lek, 'lek_id'>;