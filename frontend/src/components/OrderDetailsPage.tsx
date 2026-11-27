import { useCallback, useEffect, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { zamowieniaService } from '../services/zamowieniaService';
import { produktyService } from '../services/produktyService';
import { adresyService } from '../services/adresyService';
import { receptyService } from '../services/receptyService';
import { platnosciService } from '../services/platnosciService';
import type { SzczegolZamowienia, Zamowienie } from '../types/zamowienie';
import type { Produkt } from '../types/produkt';
import type { AdresKlienta } from '../types/adres_klienta';
import type { Recepta } from '../types/recepta';
import type { Platnosc } from '../types/platnosc';
import { OrderStatusBadge, PlatnoscStatusBadge, ReceptaStatusBadge, RxBadge } from './Badges';
import { formatDatePl, zl } from '../utils/format';

export const OrderDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user, loading: authLoading, isAuthenticated } = useAuth();

  const [zamowienie, setZamowienie] = useState<Zamowienie | null>(null);
  const [pozycje, setPozycje] = useState<SzczegolZamowienia[]>([]);
  const [produktyMap, setProduktyMap] = useState<Record<number, Produkt>>({});
  const [adres, setAdres] = useState<AdresKlienta | null>(null);
  const [receptyMap, setReceptyMap] = useState<Record<number, Recepta>>({});
  const [platnosc, setPlatnosc] = useState<Platnosc | null>(null);

  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    const orderId = Number(id);
    if (!orderId || Number.isNaN(orderId)) {
      setError('Nieprawidłowy identyfikator zamówienia.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const [orderData, pozycjeData, produktyList] = await Promise.all([
        zamowieniaService.getById(orderId),
        zamowieniaService.getPozycje(orderId),
        produktyService.getAll().catch(() => [] as Produkt[]),
      ]);

      setZamowienie(orderData);
      setPozycje(pozycjeData);

      const pMap: Record<number, Produkt> = {};
      for (const p of produktyList) {
        pMap[p.id_produktu] = p;
      }
      setProduktyMap(pMap);

      // Pobranie adresu dostawy
      try {
        const adresData = await adresyService.getById(orderData.id_adresu);
        setAdres(adresData);
      } catch {
        setAdres(null);
      }

      // Pobranie płatności powiązanej z zamówieniem
      try {
        const platnosciList = await platnosciService.getAll({
          id_zamowienia: orderData.id_zamowienia,
        });
        setPlatnosc(platnosciList[0] ?? null);
      } catch {
        setPlatnosc(null);
      }

      // Pobranie recept przypisanych do pozycji zamówienia (jeśli występują)
      const rxIds = Array.from(
        new Set(
          pozycjeData
            .map((poz) => poz.id_recepty)
            .filter((rxId): rxId is number => rxId !== null && rxId !== undefined),
        ),
      );

      if (rxIds.length > 0) {
        const rxEntries = await Promise.all(
          rxIds.map(async (rxId) => {
            try {
              const r = await receptyService.getById(rxId);
              return [rxId, r] as const;
            } catch {
              return null;
            }
          }),
        );
        const rMap: Record<number, Recepta> = {};
        for (const entry of rxEntries) {
          if (entry) rMap[entry[0]] = entry[1];
        }
        setReceptyMap(rMap);
      } else {
        setReceptyMap({});
      }
    } catch (err) {
      console.error(err);
      setError(
        err instanceof Error
          ? err.message
          : 'Nie udało się pobrać szczegółów zamówienia.',
      );
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

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

  const handleCancelOrder = async () => {
    if (!zamowienie || zamowienie.status_zamowienia !== 'NOWE') return;
    setCancelling(true);
    setError(null);
    try {
      const updated = await zamowieniaService.cancel(zamowienie.id_zamowienia);
      setZamowienie(updated);
    } catch (err) {
      console.error(err);
      setError('Nie udało się anulować zamówienia.');
    } finally {
      setCancelling(false);
    }
  };

  const backTo = user.typ_konta === 'PRACOWNIK' ? '/panel/zamowienia' : '/zamowienia';
  const backLabel =
    user.typ_konta === 'PRACOWNIK'
      ? '← Wróć do panelu zamówień'
      : '← Wróć do moich zamówień';

  if (loading) {
    return (
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 py-20 flex flex-col items-center justify-center gap-3">
        <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
        <p className="text-sm font-medium text-slate-600">
          Ładowanie szczegółów zamówienia...
        </p>
      </div>
    );
  }

  if (error || !zamowienie) {
    return (
      <div className="space-y-4">
        <Link
          to={backTo}
          className="inline-flex items-center gap-1.5 text-sm font-medium text-teal-700 hover:underline"
        >
          {backLabel}
        </Link>
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-6 text-rose-900 space-y-3">
          <h3 className="font-semibold text-rose-950">
            Nie udało się wczytać zamówienia #{id}
          </h3>
          <p className="text-xs sm:text-sm text-rose-800">
            {error ?? 'Zamówienie nie zostało znalezione.'}
          </p>
          <button
            type="button"
            onClick={load}
            className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold transition-all"
          >
            Spróbuj ponownie
          </button>
        </div>
      </div>
    );
  }

  if (user.typ_konta === 'KLIENT' && zamowienie.id_klienta !== user.id) {
    return (
      <div className="max-w-lg mx-auto bg-white rounded-2xl shadow-sm border border-slate-200/80 p-8 text-center space-y-4">
        <h2 className="text-lg font-bold text-slate-900">
          Brak dostępu do tego zamówienia
        </h2>
        <p className="text-sm text-slate-500">
          To zamówienie nie należy do Twojego konta klienta.
        </p>
        <Link
          to="/zamowienia"
          className="inline-flex items-center justify-center px-5 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-sm font-medium transition-all"
        >
          Wróć do moich zamówień
        </Link>
      </div>
    );
  }

  const usedRecepty = Object.values(receptyMap);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <Link
          to={backTo}
          className="inline-flex items-center gap-1.5 text-sm font-medium text-teal-700 hover:underline"
        >
          {backLabel}
        </Link>

        {zamowienie.status_zamowienia === 'NOWE' && (
          <button
            type="button"
            disabled={cancelling}
            onClick={handleCancelOrder}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl border border-rose-200 bg-rose-50 hover:bg-rose-100 disabled:opacity-50 text-rose-700 text-xs font-semibold transition-all"
          >
            {cancelling ? 'Anulowanie...' : 'Anuluj zamówienie'}
          </button>
        )}
      </div>

      {/* Nagłówek zamówienia */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-slate-100">
          <div>
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-bold text-slate-900">
                Zamówienie #{zamowienie.id_zamowienia}
              </h2>
              <OrderStatusBadge status={zamowienie.status_zamowienia} />
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Złożono: {formatDatePl(zamowienie.data_zamowienia)}
            </p>
          </div>

          <div className="sm:text-right">
            <span className="text-xs uppercase tracking-wider font-semibold text-slate-400">
              Kwota całkowita
            </span>
            <p className="text-2xl font-bold text-teal-700">
              {zl(zamowienie.suma_calkowita)}
            </p>
          </div>
        </div>

        {/* Informacje o adresie, płatności i recepcie */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/70 space-y-1">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Adres dostawy
            </span>
            {adres ? (
              <div className="text-slate-800">
                <p className="font-semibold">
                  ul. {adres.ulica} {adres.numer_domu}
                  {adres.numer_mieszkania ? `/${adres.numer_mieszkania}` : ''}
                </p>
                <p className="text-xs text-slate-500">
                  {adres.kod_pocztowy} {adres.miasto}
                </p>
              </div>
            ) : (
              <p className="text-slate-600 font-mono text-xs">
                ID adresu: #{zamowienie.id_adresu}
              </p>
            )}
          </div>

          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/70 space-y-1">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Płatność
            </span>
            {platnosc ? (
              <div className="text-slate-800 space-y-1">
                <p className="font-semibold">{platnosc.metoda_platnosci}</p>
                <PlatnoscStatusBadge status={platnosc.status_platnosci} />
              </div>
            ) : (
              <p className="text-xs text-slate-500">
                Brak zarejestrowanej płatności
              </p>
            )}
          </div>

          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/70 space-y-1">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              e-Recepta (Rx)
            </span>
            {usedRecepty.length > 0 ? (
              <div className="space-y-1.5">
                {usedRecepty.map((r) => (
                  <div key={r.id_recepty} className="text-slate-800 space-y-1">
                    <p className="font-mono font-semibold text-xs">
                      Kod: {r.kod_recepty}
                    </p>
                    <ReceptaStatusBadge status={r.status_recepty} />
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-500">
                Zamówienie bez leków na receptę
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Tabela pozycji zamówienia */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
          <h3 className="text-base font-bold text-slate-900">
            Pozycje zamówienia ({pozycje.length})
          </h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold uppercase tracking-wider text-slate-500">
                <th className="py-3.5 px-4 sm:px-6">Produkt</th>
                <th className="py-3.5 px-4 sm:px-6">Recepta</th>
                <th className="py-3.5 px-4 sm:px-6 text-center">Ilość</th>
                <th className="py-3.5 px-4 sm:px-6 text-right">Cena jedn.</th>
                <th className="py-3.5 px-4 sm:px-6 text-right">Wartość</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-sm">
              {pozycje.map((pozycja) => {
                const prod = produktyMap[pozycja.id_produktu];
                const recepta =
                  pozycja.id_recepty != null
                    ? receptyMap[pozycja.id_recepty]
                    : undefined;
                const wartosc = pozycja.ilosc * pozycja.cena_jednostkowa;

                return (
                  <tr key={pozycja.id_szczegolu} className="hover:bg-slate-50/60">
                    <td className="py-4 px-4 sm:px-6">
                      <Link
                        to={`/produkty/${pozycja.id_produktu}`}
                        className="font-semibold text-slate-900 hover:text-teal-700"
                      >
                        {prod
                          ? prod.nazwa_produktu
                          : `Produkt #${pozycja.id_produktu}`}
                      </Link>
                      <p className="text-xs text-slate-400 font-mono">
                        ID produktu: #{pozycja.id_produktu}
                      </p>
                    </td>
                    <td className="py-4 px-4 sm:px-6 whitespace-nowrap">
                      {pozycja.id_recepty ? (
                        <div className="flex flex-col gap-1 items-start">
                          <RxBadge required={true} />
                          <span className="text-[11px] font-mono text-slate-500">
                            {recepta
                              ? `Kod: ${recepta.kod_recepty}`
                              : `Recepta #${pozycja.id_recepty}`}
                          </span>
                        </div>
                      ) : (
                        <RxBadge required={Boolean(prod?.wymaga_recepty)} />
                      )}
                    </td>
                    <td className="py-4 px-4 sm:px-6 text-center font-medium text-slate-700">
                      {pozycja.ilosc} szt.
                    </td>
                    <td className="py-4 px-4 sm:px-6 text-right text-slate-600 whitespace-nowrap">
                      {zl(pozycja.cena_jednostkowa)}
                    </td>
                    <td className="py-4 px-4 sm:px-6 text-right font-semibold text-slate-900 whitespace-nowrap">
                      {zl(wartosc)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
            <tfoot>
              <tr className="bg-slate-50/70 border-t border-slate-200">
                <td
                  colSpan={4}
                  className="py-4 px-4 sm:px-6 text-right font-semibold text-slate-700"
                >
                  Razem do zapłaty:
                </td>
                <td className="py-4 px-4 sm:px-6 text-right text-lg font-bold text-teal-700 whitespace-nowrap">
                  {zl(zamowienie.suma_calkowita)}
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>
  );
};
