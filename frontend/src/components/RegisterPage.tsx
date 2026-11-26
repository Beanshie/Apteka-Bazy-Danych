import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../context/AuthContext';

const MAX_PASSWORD_BYTES = 72;

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { register } = useAuth();

  const [imie, setImie] = useState('');
  const [nazwisko, setNazwisko] = useState('');
  const [email, setEmail] = useState('');
  const [haslo, setHaslo] = useState('');
  const [telefon, setTelefon] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (new TextEncoder().encode(haslo).length > MAX_PASSWORD_BYTES) {
      setError('Hasło może mieć maksymalnie 72 bajty (UTF-8).');
      return;
    }

    setLoading(true);
    try {
      await register({
        imie: imie.trim(),
        nazwisko: nazwisko.trim(),
        email: email.trim(),
        haslo,
        telefon: telefon.trim() ? telefon.trim() : null,
      });
      navigate('/');
    } catch (err) {
      if (axios.isAxiosError(err)) {
        if (err.response?.status === 409) {
          setError('Konto z podanym adresem e-mail już istnieje.');
        } else if (err.response?.status === 400) {
          setError('Nieprawidłowe dane formularza. Sprawdź wszystkie pola.');
        } else if (err.response?.status === 403) {
          setError('Błąd weryfikacji sesji (CSRF) po utworzeniu konta. Zaloguj się ręcznie.');
        } else {
          setError('Błąd serwera podczas rejestracji konta.');
        }
      } else {
        setError(err instanceof Error ? err.message : 'Wystąpił nieznany błąd rejestracji.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto">
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6 sm:p-8 space-y-6">
        <div className="space-y-1 text-center">
          <h2 className="text-xl font-bold text-slate-900">Utwórz konto</h2>
          <p className="text-xs text-slate-500">Zarejestruj się jako klient apteki</p>
        </div>

        {error && (
          <div className="bg-rose-50 border border-rose-200 rounded-xl p-4 text-xs sm:text-sm text-rose-800">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label htmlFor="imie" className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                Imię
              </label>
              <input
                id="imie"
                type="text"
                required
                maxLength={50}
                value={imie}
                onChange={(e) => setImie(e.target.value)}
                placeholder="Jan"
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 transition-all placeholder:text-slate-400"
              />
            </div>

            <div>
              <label htmlFor="nazwisko" className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                Nazwisko
              </label>
              <input
                id="nazwisko"
                type="text"
                required
                maxLength={50}
                value={nazwisko}
                onChange={(e) => setNazwisko(e.target.value)}
                placeholder="Kowalski"
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 transition-all placeholder:text-slate-400"
              />
            </div>
          </div>

          <div>
            <label htmlFor="email" className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
              Adres e-mail
            </label>
            <input
              id="email"
              type="email"
              required
              maxLength={100}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="jan.kowalski@example.com"
              className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 transition-all placeholder:text-slate-400"
            />
          </div>

          <div>
            <label htmlFor="haslo" className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
              Hasło
            </label>
            <input
              id="haslo"
              type="password"
              required
              maxLength={MAX_PASSWORD_BYTES}
              value={haslo}
              onChange={(e) => setHaslo(e.target.value)}
              placeholder="••••••••"
              className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 transition-all placeholder:text-slate-400"
            />
          </div>

          <div>
            <label htmlFor="telefon" className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
              Telefon <span className="font-normal text-slate-400 normal-case">(opcjonalnie)</span>
            </label>
            <input
              id="telefon"
              type="tel"
              maxLength={20}
              value={telefon}
              onChange={(e) => setTelefon(e.target.value)}
              placeholder="+48 123 456 789"
              className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 transition-all placeholder:text-slate-400"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-teal-600 hover:bg-teal-700 active:bg-teal-800 disabled:opacity-60 text-white font-medium text-sm rounded-xl transition-all shadow-sm hover:shadow-md shadow-teal-600/20"
          >
            {loading && (
              <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            )}
            <span>{loading ? 'Rejestracja...' : 'Zarejestruj się'}</span>
          </button>
        </form>

        <div className="pt-4 border-t border-slate-100 text-center text-xs text-slate-500">
          Masz już konto?{' '}
          <Link to="/login" className="font-semibold text-teal-700 hover:underline">
            Zaloguj się
          </Link>
        </div>
      </div>
    </div>
  );
};
