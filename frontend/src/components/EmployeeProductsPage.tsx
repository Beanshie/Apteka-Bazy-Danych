import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import axios from 'axios';
import { produktyService } from '../services/produktyService';
import { kategorieService } from '../services/kategorieService';
import { dostawcyService } from '../services/dostawcyService';
import type { Produkt } from '../types/produkt';
import type { Kategoria } from '../types/kategoria';
import type { Dostawca } from '../types/dostawca';
import { RxBadge, StockBadge } from './Badges';
import { zl } from '../utils/format';

interface ProduktFormState {
  nazwa_produktu: string;
  id_kategorii: string;
  id_dostawcy: string;
  cena: string;
  stan_magazynowy: string;
  wymaga_recepty: boolean;
}

const EMPTY_FORM: ProduktFormState = {
  nazwa_produktu: '',
  id_kategorii: '',
  id_dostawcy: '',
  cena: '',
  stan_magazynowy: '0',
  wymaga_recepty: false,
};

export const EmployeeProductsPage: React.FC = () => {
  const [produkty, setProdukty] = useState<Produkt[]>([]);
  const [kategorie, setKategorie] = useState<Kategoria[]>([]);
  const [dostawcy, setDostawcy] = useState<Dostawca[]>([]);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Filtry
  const [query, setQuery] = useState('');
  const [selectedKategoria, setSelectedKategoria] = useState('');
  const [onlyLowStock, setOnlyLowStock] = useState(false);

  // Formularz dodawania / edycji
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<ProduktFormState>(EMPTY_FORM);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [produktyData, kategorieData, dostawcyData] = await Promise.all([
        produktyService.getAll(),
        kategorieService.getAll().catch(() => [] as Kategoria[]),
        dostawcyService.getAll().catch(() => [] as Dostawca[]),
      ]);
      setProdukty(produktyData);
      setKategorie(kategorieData);
      setDostawcy(dostawcyData);
    } catch (err) {
      console.error(err);
      setError('Nie udało się pobrać listy produktów.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const kategorieMap = useMemo(() => {
    const map: Record<number, Kategoria> = {};
    for (const k of kategorie) map[k.id_kategorii] = k;
    return map;
  }, [kategorie]);

  const dostawcyMap = useMemo(() => {
    const map: Record<number, Dostawca> = {};
    for (const d of dostawcy) map[d.id_dostawcy] = d;
    return map;
  }, [dostawcy]);

  const openCreateForm = () => {
    setEditingId(null);
    setForm({
      ...EMPTY_FORM,
      id_kategorii: kategorie[0] ? String(kategorie[0].id_kategorii) : '',
      id_dostawcy: dostawcy[0] ? String(dostawcy[0].id_dostawcy) : '',
    });
    setError(null);
    setSuccessMsg(null);
    setFormOpen(true);
  };

  const openEditForm = (produkt: Produkt) => {
    setEditingId(produkt.id_produktu);
    setForm({
      nazwa_produktu: produkt.nazwa_produktu,
      id_kategorii: String(produkt.id_kategorii),
      id_dostawcy: String(produkt.id_dostawcy),
      cena: String(produkt.cena),
      stan_magazynowy: String(produkt.stan_magazynowy),
      wymaga_recepty: Boolean(produkt.wymaga_recepty),
    });
    setError(null);
    setSuccessMsg(null);
    setFormOpen(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const closeForm = () => {
    setFormOpen(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);

    const idKategorii = Number(form.id_kategorii);
    const idDostawcy = Number(form.id_dostawcy);
    const cena = Number(form.cena);
    const stan = Number(form.stan_magazynowy);

    if (!form.nazwa_produktu.trim()) {
      setError('Podaj nazwę produktu.');
      return;
    }
    if (!idKategorii || !idDostawcy) {
      setError('Wybierz kategorię oraz dostawcę.');
      return;
    }
    if (Number.isNaN(cena) || cena < 0) {
      setError('Podaj poprawną cenę (>= 0).');
      return;
    }
    if (!Number.isInteger(stan) || stan < 0) {
      setError('Stan magazynowy musi być liczbą całkowitą >= 0.');
      return;
    }

    const payload = {
      nazwa_produktu: form.nazwa_produktu.trim(),
      id_kategorii: idKategorii,
      id_dostawcy: idDostawcy,
      cena: Math.round(cena * 100) / 100,
      stan_magazynowy: stan,
      wymaga_recepty: form.wymaga_recepty,
    };

    setSaving(true);
    try {
      if (editingId !== null) {
        const updated = await produktyService.update(editingId, payload);
        setProdukty((prev) =>
          prev.map((p) => (p.id_produktu === updated.id_produktu ? updated : p)),
        );
        setSuccessMsg(`Zaktualizowano produkt #${updated.id_produktu} (${updated.nazwa_produktu}).`);
      } else {
        const created = await produktyService.create(payload);
        setProdukty((prev) => [...prev, created]);
        setSuccessMsg(`Dodano nowy produkt #${created.id_produktu} (${created.nazwa_produktu}).`);
      }
      closeForm();
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const st = err.response?.status;
        if (st === 400) {
          setError('Błąd walidacji danych produktu (400). Sprawdź poprawność pól.');
        } else if (st === 404) {
          setError('Nie znaleziono wybranej kategorii, dostawcy lub produktu (404).');
        } else if (st === 409) {
          setError('Konflikt danych podczas zapisu produktu (409).');
        } else {
          setError('Błąd serwera podczas zapisu produktu.');
        }
      } else {
        setError('Nie udało się zapisać produktu.');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (produkt: Produkt) => {
    const confirmed = window.confirm(
      `Czy na pewno chcesz usunąć produkt „${produkt.nazwa_produktu}” (#${produkt.id_produktu})?`,
    );
    if (!confirmed) return;

    setDeletingId(produkt.id_produktu);
    setError(null);
    setSuccessMsg(null);
    try {
      await produktyService.delete(produkt.id_produktu);
      setProdukty((prev) => prev.filter((p) => p.id_produktu !== produkt.id_produktu));
      if (editingId === produkt.id_produktu) {
        closeForm();
      }
      setSuccessMsg(`Usunięto produkt #${produkt.id_produktu} (${produkt.nazwa_produktu}).`);
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const st = err.response?.status;
        if (st === 409) {
          setError(
            `Nie można usunąć produktu „${produkt.nazwa_produktu}” (#${produkt.id_produktu}) — jest powiązany z istniejącymi zamówieniami w bazie danych (błąd 409 Conflict).`,
          );
        } else if (st === 404) {
          setError(`Produkt #${produkt.id_produktu} nie istnieje (404).`);
        } else if (st === 400) {
          setError(`Nieprawidłowe żądanie usunięcia produktu #${produkt.id_produktu} (400).`);
        } else {
          setError(`Błąd serwera podczas usuwania produktu #${produkt.id_produktu}.`);
        }
      } else {
        setError('Nie udało się usunąć produktu.');
      }
    } finally {
      setDeletingId(null);
    }
  };

  const hasActiveFilters = query.trim() !== '' || selectedKategoria !== '' || onlyLowStock;

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const katId = selectedKategoria ? Number(selectedKategoria) : null;

    return produkty.filter((p) => {
      if (
        q &&
        !p.nazwa_produktu.toLowerCase().includes(q) &&
        String(p.id_produktu) !== q.replace(/^#/, '')
      ) {
        return false;
      }
      if (katId !== null && p.id_kategorii !== katId) {
        return false;
      }
      if (onlyLowStock && (p.stan_magazynowy ?? 0) >= 10) {
        return false;
      }
      return true;
    });
  }, [produkty, query, selectedKategoria, onlyLowStock]);

  return (
    <div className="space-y-6">
      {/* Nagłówek i filtry */}
      <div className="bg-white rounded-2xl p-4 sm:p-6 shadow-sm border border-slate-200/80 space-y-4">
        <div className="flex flex-col sm:flex-row gap-4 justify-between items-start sm:items-center">
          <div>
            <h2 className="text-xl font-bold text-slate-900">
              Magazyn i katalog produktów
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Dodawanie, edycja stanów magazynowych i usuwanie produktów
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={openCreateForm}
              className="inline-flex items-center gap-1.5 px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white font-medium text-sm rounded-xl transition-all"
            >
              + Dodaj produkt
            </button>
            <button
              type="button"
              onClick={load}
              disabled={loading}
              className="inline-flex items-center gap-1.5 px-4 py-2 border border-slate-200 hover:bg-slate-50 disabled:opacity-50 text-slate-700 font-medium text-sm rounded-xl transition-all"
            >
              Odśwież
            </button>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3 pt-3 border-t border-slate-100">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Szukaj po nazwie lub ID produktu..."
            className="flex-1 min-w-[220px] px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
          />

          <select
            aria-label="Filtruj według kategorii"
            value={selectedKategoria}
            onChange={(e) => setSelectedKategoria(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
          >
            <option value="">Wszystkie kategorie</option>
            {kategorie.map((k) => (
              <option key={k.id_kategorii} value={k.id_kategorii}>
                {k.nazwa_kategorii}
              </option>
            ))}
          </select>

          <label className="inline-flex items-center gap-2 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-700 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={onlyLowStock}
              onChange={(e) => setOnlyLowStock(e.target.checked)}
              className="accent-teal-600 rounded"
            />
            <span>Niski stan (&lt; 10 szt.)</span>
          </label>

          {hasActiveFilters && (
            <button
              type="button"
              onClick={() => {
                setQuery('');
                setSelectedKategoria('');
                setOnlyLowStock(false);
              }}
              className="px-3 py-2 text-xs font-semibold text-slate-600 hover:text-rose-600 rounded-xl border border-slate-200 hover:bg-slate-50"
            >
              Wyczyść filtry
            </button>
          )}
        </div>
      </div>

      {/* Komunikaty */}
      {error && (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-4 text-sm text-rose-800 flex items-center justify-between gap-4">
          <span>{error}</span>
          <button
            type="button"
            onClick={() => setError(null)}
            className="text-xs font-semibold text-rose-700 hover:underline whitespace-nowrap"
          >
            Zamknij
          </button>
        </div>
      )}

      {successMsg && (
        <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-4 text-sm text-emerald-800 flex items-center justify-between gap-4">
          <span>{successMsg}</span>
          <button
            type="button"
            onClick={() => setSuccessMsg(null)}
            className="text-xs font-semibold text-emerald-700 hover:underline whitespace-nowrap"
          >
            Zamknij
          </button>
        </div>
      )}

      {/* Formularz dodawania / edycji */}
      {formOpen && (
        <form
          onSubmit={handleSubmit}
          className="bg-white rounded-2xl shadow-sm border border-teal-200 p-6 space-y-4"
        >
          <div className="flex items-center justify-between">
            <h3 className="text-base font-bold text-slate-900">
              {editingId !== null
                ? `Edycja produktu #${editingId}`
                : 'Dodaj nowy produkt'}
            </h3>
            <button
              type="button"
              onClick={closeForm}
              className="text-xs font-semibold text-slate-500 hover:text-slate-800"
            >
              Anuluj
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div className="md:col-span-2 lg:col-span-1">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1">
                Nazwa produktu *
              </label>
              <input
                type="text"
                required
                maxLength={100}
                value={form.nazwa_produktu}
                onChange={(e) => setForm({ ...form, nazwa_produktu: e.target.value })}
                placeholder="np. Ibuprom 200mg"
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-teal-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1">
                Kategoria *
              </label>
              <select
                required
                value={form.id_kategorii}
                onChange={(e) => setForm({ ...form, id_kategorii: e.target.value })}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-700 focus:outline-none focus:border-teal-500"
              >
                <option value="">Wybierz kategorię...</option>
                {kategorie.map((k) => (
                  <option key={k.id_kategorii} value={k.id_kategorii}>
                    {k.nazwa_kategorii}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1">
                Dostawca *
              </label>
              <select
                required
                value={form.id_dostawcy}
                onChange={(e) => setForm({ ...form, id_dostawcy: e.target.value })}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-700 focus:outline-none focus:border-teal-500"
              >
                <option value="">Wybierz dostawcę...</option>
                {dostawcy.map((d) => (
                  <option key={d.id_dostawcy} value={d.id_dostawcy}>
                    {d.nazwa_firmy}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1">
                Cena (PLN) *
              </label>
              <input
                type="number"
                required
                step="0.01"
                min="0"
                max="99999999.99"
                value={form.cena}
                onChange={(e) => setForm({ ...form, cena: e.target.value })}
                placeholder="12.50"
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-teal-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1">
                Stan magazynowy (szt.) *
              </label>
              <input
                type="number"
                required
                step="1"
                min="0"
                value={form.stan_magazynowy}
                onChange={(e) => setForm({ ...form, stan_magazynowy: e.target.value })}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-teal-500"
              />
            </div>

            <div className="flex items-end pb-1">
              <label className="inline-flex items-center gap-2 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-700 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={form.wymaga_recepty}
                  onChange={(e) => setForm({ ...form, wymaga_recepty: e.target.checked })}
                  className="accent-teal-600 rounded"
                />
                <span>Wymaga recepty (Rx)</span>
              </label>
            </div>
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
            <button
              type="button"
              onClick={closeForm}
              className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Anuluj
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2 rounded-xl bg-teal-600 hover:bg-teal-700 disabled:opacity-50 text-white text-sm font-semibold transition-all"
            >
              {saving
                ? 'Zapisywanie...'
                : editingId !== null
                  ? 'Zapisz zmiany'
                  : 'Dodaj produkt'}
            </button>
          </div>
        </form>
      )}

      {/* Tabela produktów */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 overflow-hidden">
        <div className="overflow-x-auto relative min-h-[260px]">
          {loading && (
            <div className="absolute inset-0 bg-white/80 backdrop-blur-sm z-20 flex flex-col items-center justify-center gap-3">
              <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
              <p className="text-sm font-medium text-slate-600">Ładowanie katalogu...</p>
            </div>
          )}

          {!loading && filtered.length === 0 ? (
            <div className="py-16 text-center text-sm text-slate-500">
              Brak produktów spełniających kryteria wyszukiwania.
            </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold uppercase tracking-wider text-slate-500">
                  <th className="py-3.5 px-4">ID</th>
                  <th className="py-3.5 px-4">Nazwa produktu</th>
                  <th className="py-3.5 px-4">Kategoria</th>
                  <th className="py-3.5 px-4">Dostawca</th>
                  <th className="py-3.5 px-4 text-right">Cena</th>
                  <th className="py-3.5 px-4">Stan</th>
                  <th className="py-3.5 px-4">Rx</th>
                  <th className="py-3.5 px-4 text-right">Akcje</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {filtered.map((produkt) => {
                  const kat = kategorieMap[produkt.id_kategorii];
                  const dost = dostawcyMap[produkt.id_dostawcy];
                  const isDeleting = deletingId === produkt.id_produktu;

                  return (
                    <tr key={produkt.id_produktu} className="hover:bg-slate-50/60">
                      <td className="py-3.5 px-4 font-mono text-xs text-slate-500">
                        #{produkt.id_produktu}
                      </td>
                      <td className="py-3.5 px-4 font-semibold text-slate-900">
                        {produkt.nazwa_produktu}
                      </td>
                      <td className="py-3.5 px-4 text-slate-600">
                        {kat ? kat.nazwa_kategorii : `#${produkt.id_kategorii}`}
                      </td>
                      <td className="py-3.5 px-4 text-slate-600">
                        {dost ? dost.nazwa_firmy : `#${produkt.id_dostawcy}`}
                      </td>
                      <td className="py-3.5 px-4 text-right font-medium text-slate-900 whitespace-nowrap">
                        {zl(produkt.cena)}
                      </td>
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <StockBadge qty={produkt.stan_magazynowy ?? 0} />
                      </td>
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <RxBadge required={Boolean(produkt.wymaga_recepty)} />
                      </td>
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <div className="inline-flex items-center gap-1.5">
                          <button
                            type="button"
                            onClick={() => openEditForm(produkt)}
                            className="px-2.5 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-100 text-slate-700 text-xs font-semibold transition-all"
                          >
                            Edytuj
                          </button>
                          <button
                            type="button"
                            disabled={isDeleting}
                            onClick={() => handleDelete(produkt)}
                            className="px-2.5 py-1.5 rounded-lg border border-rose-200 bg-rose-50 hover:bg-rose-100 disabled:opacity-40 text-rose-700 text-xs font-semibold transition-all"
                          >
                            {isDeleting ? '...' : 'Usuń'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>

        <div className="bg-slate-50/50 px-4 py-3 border-t border-slate-200/80 flex items-center justify-between text-xs text-slate-500">
          <span>
            Wyświetlono {filtered.length} z {produkty.length} produktów
          </span>
        </div>
      </div>
    </div>
  );
};
