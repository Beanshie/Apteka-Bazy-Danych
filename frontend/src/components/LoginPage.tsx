import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../context/AuthContext';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState('');
  const [haslo, setHaslo] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, haslo);
      navigate('/');
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setError('Nie udało się połączyć z serwerem podczas weryfikacji konta.');
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
          <h2 className="text-xl font-bold text-slate-900">Zaloguj się</h2>
          <p className="text-xs text-slate-500">Wprowadź dane swojego konta klienta</p>
        </div>

        {error && (
          <div className="bg-rose-50 border border-rose-200 rounded-xl p-4 text-xs sm:text-sm text-rose-800">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
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
              maxLength={255}
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

        <div className="pt-4 border-t border-slate-100 text-center text-xs text-slate-500">
          Nie masz jeszcze konta?{' '}
          <Link to="/rejestracja" className="font-semibold text-teal-700 hover:underline">
            Zarejestruj się
          </Link>
        </div>
      </div>
    </div>
  );
};
