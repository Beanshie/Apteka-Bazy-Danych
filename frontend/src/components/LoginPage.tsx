import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../context/AuthContext';
import type { TypKonta } from '../types/auth';

const MAX_PASSWORD_BYTES = 72;

interface LoginPageProps {
  mode?: TypKonta;
}

export const LoginPage: React.FC<LoginPageProps> = ({ mode = 'KLIENT' }) => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [identyfikator, setIdentyfikator] = useState('');
  const [haslo, setHaslo] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isKlient = mode === 'KLIENT';

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (new TextEncoder().encode(haslo).length > MAX_PASSWORD_BYTES) {
      setError('Hasło może mieć maksymalnie 72 bajty (UTF-8).');
      return;
    }

    setLoading(true);
    try {
      await login({
        typ_konta: mode,
        identyfikator: identyfikator.trim(),
        haslo,
      });
      navigate(isKlient ? '/' : '/panel/zamowienia');
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const status = err.response?.status;
        if (status === 401) {
          setError('Nieprawidłowe dane logowania lub nieaktywne konto.');
        } else if (status === 400) {
          setError('Nieprawidłowe dane wejściowe. Sprawdź poprawność pól.');
        } else if (status === 403) {
          setError('Błąd weryfikacji tokenu bezpieczeństwa (CSRF). Spróbuj ponownie.');
        } else {
          setError('Nie udało się połączyć z serwerem podczas weryfikacji konta.');
        }
      } else {
        setError(err instanceof Error ? err.message : 'Wystąpił nieznany błąd logowania.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto">
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 p-6 sm:p-8 space-y-6">
        <div className="space-y-1 text-center">
          <h2 className="text-xl font-bold text-slate-900">
            {isKlient ? 'Zaloguj się' : 'Panel personelu'}
          </h2>
          <p className="text-xs text-slate-500">
            {isKlient
              ? 'Wprowadź dane swojego konta klienta'
              : 'Logowanie do konta pracownika apteki'}
          </p>
        </div>

        {error && (
          <div className="bg-rose-50 border border-rose-200 rounded-xl p-4 text-xs sm:text-sm text-rose-800">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label
              htmlFor="identyfikator"
              className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5"
            >
              {isKlient ? 'Adres e-mail' : 'Login pracownika'}
            </label>
            <input
              id="identyfikator"
              type={isKlient ? 'email' : 'text'}
              required
              maxLength={isKlient ? 100 : 50}
              value={identyfikator}
              onChange={(e) => setIdentyfikator(e.target.value)}
              placeholder={isKlient ? 'jan.kowalski@example.com' : 'jkowalski'}
              className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 transition-all placeholder:text-slate-400"
            />
          </div>

          <div>
            <label
              htmlFor="haslo"
              className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5"
            >
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

          <button
            type="submit"
            disabled={loading}
            className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-teal-600 hover:bg-teal-700 active:bg-teal-800 disabled:opacity-60 text-white font-medium text-sm rounded-xl transition-all shadow-sm hover:shadow-md shadow-teal-600/20"
          >
            {loading && (
              <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            )}
            <span>{loading ? 'Logowanie...' : 'Zaloguj się'}</span>
          </button>
        </form>

        {isKlient ? (
          <div className="pt-4 border-t border-slate-100 text-center space-y-3">
            <p className="text-xs text-slate-500">
              Nie masz jeszcze konta?{' '}
              <Link to="/rejestracja" className="font-semibold text-teal-700 hover:underline">
                Zarejestruj się
              </Link>
            </p>
            <div>
              <Link
                to="/panel/login"
                className="text-xs text-slate-400 hover:text-slate-600 transition-colors"
              >
                Logowanie dla personelu
              </Link>
            </div>
          </div>
        ) : (
          <div className="pt-4 border-t border-slate-100 text-center">
            <Link
              to="/login"
              className="text-xs text-slate-400 hover:text-slate-600 transition-colors"
            >
              ← Powrót do logowania klienta
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};
