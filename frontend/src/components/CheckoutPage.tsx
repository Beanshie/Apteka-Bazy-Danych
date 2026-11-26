import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, Navigate } from 'react-router-dom';
import axios from 'axios';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { adresyService } from '../services/adresyService';
import { receptyService } from '../services/receptyService';
import { zamowieniaService } from '../services/zamowieniaService';
import { platnosciService } from '../services/platnosciService';
import type { AdresKlienta } from '../types/adres_klienta';
import type { Zamowienie } from '../types/zamowienie';
import { RxBadge } from './Badges';
import { zl } from '../utils/format';

const METODY_PLATNOSCI = [
  { value: 'BLIK', label: 'BLIK', desc: 'Szybka płatność kodem BLIK' },
  { value: 'KARTA', label: 'Karta płatnicza', desc: 'Visa / Mastercard' },
  { value: 'PRZELEW', label: 'Przelew online', desc: 'Szybki przelew bankowy' },
  { value: 'PRZY_ODBIORZE', label: 'Przy odbiorze', desc: 'Płatność gotówką lub kartą u kuriera' },
] as const;

export const CheckoutPage: React.FC = () => {
  const { user, loading: authLoading, isAuthenticated, isKlient } = useAuth();
  const { items, count, total, clear } = useCart();

  const [adresy, setAdresy] = useState<AdresKlienta[]>([]);
  const [loadingAdresy, setLoadingAdresy] = useState(true);
  const [selectedAdresId, setSelectedAdresId] = useState<number | 'new'>('new');

  // Formularz nowego adresu
  const [ulica, setUlica] = useState('');
  const [numerDomu, setNumerDomu] = useState('');
  const [numerMieszkania, setNumerMieszkania] = useState('');
  const [kodPocztowy, setKodPocztowy] = useState('');
  const [miasto, setMiasto] = useState('');
  const [savingAdres, setSavingAdres] = useState(false);

  // Recepta (Rx)
  const [kodRecepty, setKodRecepty] = useState('');
  const [peselPacjenta, setPeselPacjenta] = useState('');

  // Płatność
  const [metodaPlatnosci, setMetodaPlatnosci] = useState<string>('BLIK');

  // Stan wysyłki i potwierdzenia
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdOrder, setCreatedOrder] = useState<Zamowienie | null>(null);

  const requiresRx = useMemo(
    () => items.some((item) => Boolean(item.produkt.wymaga_recepty)),
    [items],
  );

  const loadAdresy = useCallback(async () => {
    if (!user || !isKlient) return;
    setLoadingAdresy(true);
    try {
      const list = await adresyService.getAll({ id_klienta: user.id });
      setAdresy(list);
      if (list.length > 0) {
        setSelectedAdresId(list[0].id_adresu);
      } else {
        setSelectedAdresId('new');
      }
    } catch (err) {
      console.error(err);
      setAdresy([]);
      setSelectedAdresId('new');
    } finally {
      setLoadingAdresy(false);
    }
  }, [user, isKlient]);

  useEffect(() => {
    loadAdresy();
  }, [loadAdresy]);

  if (authLoading) {
    return (
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 py-20 flex flex-col items-center justify-center gap-3">
        <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
        <p className="text-sm font-medium text-slate-600">Sprawdzanie sesji...</p>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (!isKlient) {
    return (
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 py-16 text-center space-y-3">
        <p className="text-slate-700 font-medium">
          Składanie zamówień jest dostępne wyłącznie dla kont klientów.
        </p>
        <Link to="/" className="inline-block text-sm font-medium text-teal-700 hover:underline">
          Wróć do listy produktów
        </Link>
      </div>
    );
  }

  // Ekran potwierdzenia po złożeniu zamówienia
  if (createdOrder) {
    return (
      <div className="max-w-xl mx-auto bg-white rounded-2xl shadow-sm border border-slate-200/80 p-8 text-center space-y-5">
        <div className="w-14 h-14 mx-auto rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-600 flex items-center justify-center">
          <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
          </svg>
        </div>

        <div className="space-y-1">
          <span className="inline-block px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            Zamówienie przyjęte
          </span>
          <h2 className="text-2xl font-bold text-slate-900 pt-2">
            Dziękujemy za złożenie zamówienia!
          </h2>
          <p className="text-sm text-slate-500">
            Numer Twojego zamówienia:{' '}
            <span className="font-mono font-bold text-slate-900">
              #{createdOrder.id_zamowienia}
            </span>
          </p>
        </div>

        <div className="bg-slate-50 rounded-xl p-4 border border-slate-200/80 text-sm space-y-2 text-left">
          <div className="flex justify-between text-slate-600">
            <span>Status zamówienia:</span>
            <span className="font-semibold text-slate-900">{createdOrder.status_zamowienia}</span>
          </div>
          <div className="flex justify-between text-slate-600">
            <span>Metoda płatności:</span>
            <span className="font-semibold text-slate-900">{metodaPlatnosci}</span>
          </div>
          <div className="flex justify-between text-slate-600 pt-2 border-t border-slate-200">
            <span>Suma całkowita:</span>
            <span className="font-bold text-teal-700">{zl(createdOrder.suma_calkowita)}</span>
          </div>
        </div>

        <div className="pt-2 flex flex-wrap items-center justify-center gap-3">
          <Link
            to={`/zamowienia/${createdOrder.id_zamowienia}`}
            className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-teal-600 hover:bg-teal-700 text-white font-medium text-sm rounded-xl transition-all shadow-sm shadow-teal-600/20"
          >
            Szczegóły zamówienia
          </Link>
          <Link
            to="/zamowienia"
            className="inline-flex items-center justify-center gap-2 px-5 py-2.5 border border-slate-200 hover:bg-slate-50 text-slate-700 font-medium text-sm rounded-xl transition-all"
          >
            Moje zamówienia
          </Link>
          <Link
            to="/"
            className="inline-flex items-center justify-center gap-2 px-5 py-2.5 border border-slate-200 hover:bg-slate-50 text-slate-700 font-medium text-sm rounded-xl transition-all"
          >
            Powrót do listy produktów
          </Link>
        </div>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 py-16 text-center">
        <p className="text-slate-700 font-medium">Twój koszyk jest pusty</p>
        <Link to="/" className="inline-block mt-4 text-sm font-medium text-teal-700 hover:underline">
          Wróć do listy produktów
        </Link>
      </div>
    );
  }

  const handleSaveNewAdres = async () => {
    setError(null);
    if (!ulica.trim() || !numerDomu.trim() || !kodPocztowy.trim() || !miasto.trim()) {
      setError('Wypełnij wszystkie wymagane pola adresu dostawy.');
      return null;
    }
    setSavingAdres(true);
    try {
      const created = await adresyService.create({
        id_klienta: user.id,
        ulica: ulica.trim(),
        numer_domu: numerDomu.trim(),
        numer_mieszkania: numerMieszkania.trim() ? numerMieszkania.trim() : null,
        kod_pocztowy: kodPocztowy.trim(),
        miasto: miasto.trim(),
      });
      setAdresy((prev) => [...prev, created]);
      setSelectedAdresId(created.id_adresu);
      setUlica('');
      setNumerDomu('');
      setNumerMieszkania('');
      setKodPocztowy('');
      setMiasto('');
      return created.id_adresu;
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 400) {
        setError('Nieprawidłowe dane adresowe. Sprawdź długość i poprawność pól.');
      } else {
        setError('Nie udało się zapisać nowego adresu dostawy.');
      }
      return null;
    } finally {
      setSavingAdres(false);
    }
  };

  const isRxMissing = requiresRx && (!kodRecepty.trim() || !peselPacjenta.trim());

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (requiresRx) {
      if (!kodRecepty.trim()) {
        setError('W koszyku znajdują się leki wydawane na receptę (Rx). Podaj kod recepty.');
        return;
      }
      if (kodRecepty.trim().length > 10) {
        setError('Kod recepty może mieć maksymalnie 10 znaków.');
        return;
      }
      if (!peselPacjenta.trim() || peselPacjenta.trim().length !== 11) {
        setError('Podaj poprawny 11-cyfrowy numer PESEL pacjenta wymagany dla recepty.');
        return;
      }
    }

    setSubmitting(true);
    try {
      // 1. Ustalenie lub utworzenie adresu dostawy
      let finalAdresId: number;
      if (selectedAdresId === 'new') {
        const createdAdresId = await handleSaveNewAdres();
        if (!createdAdresId) {
          setSubmitting(false);
          return;
        }
        finalAdresId = createdAdresId;
      } else {
        finalAdresId = selectedAdresId;
      }

      // 2. Obsługa recepty (jeśli wymagana przez produkty Rx w koszyku)
      let idRecepty: number | null = null;
      if (requiresRx) {
        const trimmedCode = kodRecepty.trim();
        const trimmedPesel = peselPacjenta.trim();

        const existingRecepty = await receptyService.getAll({
          id_klienta: user.id,
          kod_recepty: trimmedCode,
        });
        const validExisting = existingRecepty.find(
          (r) => r.status_recepty === 'NOWA' || r.status_recepty === 'ZWERYFIKOWANA',
        );

        if (validExisting) {
          idRecepty = validExisting.id_recepty;
        } else {
          const nowaRecepta = await receptyService.create({
            id_klienta: user.id,
            kod_recepty: trimmedCode,
            pesel_pacjenta: trimmedPesel,
            status_recepty: 'NOWA',
          });
          idRecepty = nowaRecepta.id_recepty;
        }
      }

      // 3. Utworzenie nagłówka zamówienia (POST /api/v1/zamowienia) i dodanie pozycji
      const naglowekZamowienia = await zamowieniaService.create({
        id_klienta: user.id,
        id_adresu: finalAdresId,
        status_zamowienia: 'NOWE',
      });

      try {
        for (const { produkt, ilosc } of items) {
          await zamowieniaService.addPozycja(naglowekZamowienia.id_zamowienia, {
            id_produktu: produkt.id_produktu,
            ilosc,
            id_recepty: produkt.wymaga_recepty ? idRecepty : null,
          });
        }
      } catch (itemErr) {
        await zamowieniaService.delete(naglowekZamowienia.id_zamowienia).catch(() => {});
        throw itemErr;
      }

      const utworzoneZamowienie = await zamowieniaService.getById(
        naglowekZamowienia.id_zamowienia,
      );

      // 4. Rejestracja płatności dla zamówienia (POST /api/v1/platnosci)
      try {
        await platnosciService.create({
          id_zamowienia: utworzoneZamowienie.id_zamowienia,
          metoda_platnosci: metodaPlatnosci,
          status_platnosci: 'OCZEKUJACA',
        });
      } catch (paymentErr) {
        console.warn('Nie udało się zarejestrować rekordu płatności:', paymentErr);
      }

      // 5. Wyczyszczenie koszyka i wyświetlenie potwierdzenia
      clear();
      setCreatedOrder(utworzoneZamowienie);
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const status = err.response?.status;
        if (status === 400) {
          setError(
            'Błąd walidacji zamówienia (400): brak wystarczającego stanu magazynowego, nieprawidłowa recepta lub błędne dane wejściowe.',
          );
        } else if (status === 404) {
          setError('Nie znaleziono powiązanego rekordu (klienta, adresu, produktu lub recepty).');
        } else if (status === 409) {
          setError('Konflikt danych (409) — np. podany kod recepty jest już przypisany do innego rekordu.');
        } else {
          setError('Wystąpił błąd serwera podczas składania zamówienia.');
        }
      } else {
        setError(err instanceof Error ? err.message : 'Nieznany błąd podczas składania zamówienia.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="grid lg:grid-cols-[1fr_360px] gap-6 items-start">
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-slate-900">Finalizacja zamówienia</h2>
            <p className="text-xs text-slate-500">
              Klient: {user.imie} {user.nazwisko} ({user.identyfikator})
            </p>
          </div>
          <Link to="/koszyk" className="text-xs font-medium text-teal-700 hover:underline">
            ← Wróć do koszyka
          </Link>
        </div>

        {error && (
          <div className="bg-rose-50 border border-rose-200 rounded-2xl p-4 text-xs sm:text-sm text-rose-800">
            {error}
          </div>
        )}

        {/* 1. Adres dostawy */}
        <section className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-bold text-slate-900">1. Adres dostawy</h3>
            {adresy.length > 0 && selectedAdresId !== 'new' && (
              <button
                type="button"
                onClick={() => setSelectedAdresId('new')}
                className="text-xs font-semibold text-teal-700 hover:underline"
              >
                + Dodaj nowy adres
              </button>
            )}
          </div>

          {loadingAdresy ? (
            <p className="text-xs text-slate-500">Pobieranie zapisanych adresów...</p>
          ) : (
            <div className="space-y-3">
              {adresy.map((adres) => (
                <label
                  key={adres.id_adresu}
                  className={`flex items-start gap-3 p-3.5 rounded-xl border cursor-pointer transition-all ${
                    selectedAdresId === adres.id_adresu
                      ? 'border-teal-600 bg-teal-50/40'
                      : 'border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <input
                    type="radio"
                    name="adres"
                    checked={selectedAdresId === adres.id_adresu}
                    onChange={() => setSelectedAdresId(adres.id_adresu)}
                    className="mt-1 accent-teal-600"
                  />
                  <div className="text-sm">
                    <p className="font-semibold text-slate-900">
                      ul. {adres.ulica} {adres.numer_domu}
                      {adres.numer_mieszkania ? `/${adres.numer_mieszkania}` : ''}
                    </p>
                    <p className="text-xs text-slate-500">
                      {adres.kod_pocztowy} {adres.miasto}
                    </p>
                  </div>
                </label>
              ))}

              {selectedAdresId === 'new' && (
                <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                      Nowy adres dostawy
                    </span>
                    {adresy.length > 0 && (
                      <button
                        type="button"
                        onClick={() => setSelectedAdresId(adresy[0].id_adresu)}
                        className="text-xs text-slate-500 hover:text-slate-800"
                      >
                        Wybierz zapisany adres
                      </button>
                    )}
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div className="sm:col-span-2">
                      <label className="block text-xs font-medium text-slate-600 mb-1">
                        Ulica *
                      </label>
                      <input
                        type="text"
                        required={selectedAdresId === 'new'}
                        maxLength={100}
                        value={ulica}
                        onChange={(e) => setUlica(e.target.value)}
                        placeholder="np. Floriańska"
                        className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-slate-600 mb-1">
                        Nr domu *
                      </label>
                      <input
                        type="text"
                        required={selectedAdresId === 'new'}
                        maxLength={10}
                        value={numerDomu}
                        onChange={(e) => setNumerDomu(e.target.value)}
                        placeholder="np. 12A"
                        className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div>
                      <label className="block text-xs font-medium text-slate-600 mb-1">
                        Nr mieszkania
                      </label>
                      <input
                        type="text"
                        maxLength={10}
                        value={numerMieszkania}
                        onChange={(e) => setNumerMieszkania(e.target.value)}
                        placeholder="opcjonalnie"
                        className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-slate-600 mb-1">
                        Kod pocztowy *
                      </label>
                      <input
                        type="text"
                        required={selectedAdresId === 'new'}
                        maxLength={10}
                        value={kodPocztowy}
                        onChange={(e) => setKodPocztowy(e.target.value)}
                        placeholder="31-155"
                        className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-slate-600 mb-1">
                        Miasto *
                      </label>
                      <input
                        type="text"
                        required={selectedAdresId === 'new'}
                        maxLength={50}
                        value={miasto}
                        onChange={(e) => setMiasto(e.target.value)}
                        placeholder="Kraków"
                        className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                      />
                    </div>
                  </div>

                  <div className="flex justify-end">
                    <button
                      type="button"
                      disabled={savingAdres}
                      onClick={handleSaveNewAdres}
                      className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-900 disabled:opacity-50 text-white text-xs font-medium transition-all"
                    >
                      {savingAdres ? 'Zapisywanie adresu...' : 'Zapisz adres na koncie'}
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </section>

        {/* 2. Recepta (Rx) - wyświetlana gdy w koszyku jest produkt wymagający recepty */}
        {requiresRx && (
          <section className="bg-white rounded-2xl shadow-sm border border-amber-200 p-6 space-y-4">
            <div className="flex items-center justify-between gap-2">
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  2. Dane e-Recepty (wymagane dla leków Rx)
                </h3>
                <p className="text-xs text-slate-500">
                  W koszyku znajdują się produkty wydawane wyłącznie z przepisu lekarza.
                </p>
              </div>
              <RxBadge required={true} />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="kod_recepty" className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                  Kod recepty (do 10 znaków) *
                </label>
                <input
                  id="kod_recepty"
                  type="text"
                  required
                  maxLength={10}
                  value={kodRecepty}
                  onChange={(e) => setKodRecepty(e.target.value)}
                  placeholder="np. RX1234"
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                />
              </div>

              <div>
                <label htmlFor="pesel_pacjenta" className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                  PESEL pacjenta (11 cyfr) *
                </label>
                <input
                  id="pesel_pacjenta"
                  type="text"
                  required
                  minLength={11}
                  maxLength={11}
                  value={peselPacjenta}
                  onChange={(e) => setPeselPacjenta(e.target.value.replace(/\D/g, ''))}
                  placeholder="np. 90010112345"
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                />
              </div>
            </div>
          </section>
        )}

        {/* 3. Metoda płatności */}
        <section className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6 space-y-4">
          <h3 className="text-base font-bold text-slate-900">
            {requiresRx ? '3. Metoda płatności' : '2. Metoda płatności'}
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {METODY_PLATNOSCI.map((m) => (
              <label
                key={m.value}
                className={`flex items-start gap-3 p-3.5 rounded-xl border cursor-pointer transition-all ${
                  metodaPlatnosci === m.value
                    ? 'border-teal-600 bg-teal-50/40'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <input
                  type="radio"
                  name="metoda_platnosci"
                  value={m.value}
                  checked={metodaPlatnosci === m.value}
                  onChange={() => setMetodaPlatnosci(m.value)}
                  className="mt-1 accent-teal-600"
                />
                <div>
                  <p className="text-sm font-semibold text-slate-900">{m.label}</p>
                  <p className="text-xs text-slate-500">{m.desc}</p>
                </div>
              </label>
            ))}
          </div>
        </section>
      </div>

      {/* Podsumowanie zamówienia */}
      <aside className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6 space-y-4 lg:sticky lg:top-24">
        <h3 className="text-lg font-bold text-slate-900">Podsumowanie zamówienia</h3>

        <div className="divide-y divide-slate-100 max-h-72 overflow-y-auto pr-1">
          {items.map(({ produkt, ilosc }) => (
            <div key={produkt.id_produktu} className="py-3 flex justify-between items-start gap-3 text-sm">
              <div className="space-y-1">
                <p className="font-medium text-slate-900 leading-snug">{produkt.nazwa_produktu}</p>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-slate-400">
                    {ilosc} × {zl(produkt.cena)}
                  </span>
                  {produkt.wymaga_recepty && <RxBadge required={true} />}
                </div>
              </div>
              <span className="font-semibold text-slate-900 whitespace-nowrap">
                {zl(ilosc * produkt.cena)}
              </span>
            </div>
          ))}
        </div>

        <div className="space-y-2 pt-3 border-t border-slate-100 text-sm">
          <div className="flex justify-between text-slate-600">
            <span>Liczba sztuk</span>
            <span>{count}</span>
          </div>
          <div className="flex justify-between items-baseline pt-2 border-t border-slate-100">
            <span className="font-medium text-slate-700">Do zapłaty</span>
            <span className="text-xl font-bold text-slate-900">{zl(total)}</span>
          </div>
        </div>

        {isRxMissing && (
          <p className="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-xl p-3">
            Uzupełnij kod recepty oraz PESEL pacjenta, aby odblokować złożenie zamówienia.
          </p>
        )}

        <button
          type="submit"
          disabled={submitting || isRxMissing}
          className="w-full inline-flex items-center justify-center gap-2 px-4 py-3 bg-teal-600 hover:bg-teal-700 active:bg-teal-800 disabled:opacity-50 text-white font-semibold text-sm rounded-xl transition-all shadow-sm hover:shadow-md shadow-teal-600/20"
        >
          {submitting && (
            <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
          )}
          <span>{submitting ? 'Składanie zamówienia...' : 'Potwierdź i zamów'}</span>
        </button>
      </aside>
    </form>
  );
};
