import { Link, Outlet } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

export const Layout = () => {
  const { count } = useCart();
  const { user, loading, isAuthenticated, isKlient, logout } = useAuth();

  return (
    <div className="bg-slate-50 min-h-screen text-slate-800 antialiased">
      <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
          <Link to="/" className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-teal-600 flex items-center justify-center text-white shadow-md shadow-teal-600/20">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
            </div>
            <div>
              <h1 className="text-lg font-bold text-slate-900 leading-tight">System Apteczny</h1>
              <p className="text-xs text-slate-500">Menedżer Bazy Produktów REST API</p>
            </div>
          </Link>

          <div className="flex items-center gap-3">
            {loading ? (
              <span className="text-xs text-slate-400 px-2">Sprawdzanie sesji...</span>
            ) : isAuthenticated && user ? (
              <div className="flex items-center gap-3">
                {isKlient ? (
                  <Link
                    to="/zamowienia"
                    className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl border border-slate-200 text-sm font-medium text-slate-700 hover:bg-slate-50 hover:text-teal-700 transition-all"
                  >
                    Moje zamówienia
                  </Link>
                ) : (
                  <>
                    <Link
                      to="/panel/zamowienia"
                      className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm font-medium text-slate-700 hover:bg-slate-50 hover:text-teal-700 transition-all"
                    >
                      Panel zamówień
                    </Link>
                    <Link
                      to="/panel/recepty"
                      className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm font-medium text-slate-700 hover:bg-slate-50 hover:text-teal-700 transition-all"
                    >
                      Recepty
                    </Link>
                    <Link
                      to="/panel/produkty"
                      className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm font-medium text-slate-700 hover:bg-slate-50 hover:text-teal-700 transition-all"
                    >
                      Magazyn / Produkty
                    </Link>
                    <Link
                      to="/panel/klienci"
                      className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm font-medium text-slate-700 hover:bg-slate-50 hover:text-teal-700 transition-all"
                    >
                      Klienci
                    </Link>
                    <Link
                      to="/raporty"
                      className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm font-medium text-slate-700 hover:bg-slate-50 hover:text-teal-700 transition-all"
                    >
                      Raporty
                    </Link>
                  </>
                )}
                <div className="hidden sm:block text-right">
                  <p className="text-sm font-semibold text-slate-900 leading-tight">
                    {user.imie} {user.nazwisko}
                    {user.rola && (
                      <span className="ml-1.5 px-1.5 py-0.5 rounded bg-teal-50 text-teal-700 text-[10px] font-semibold uppercase">
                        {user.rola}
                      </span>
                    )}
                  </p>
                  <p className="text-xs text-slate-500">{user.identyfikator}</p>
                </div>
                <button
                  type="button"
                  onClick={() => void logout()}
                  className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl border border-slate-200 text-sm font-medium text-slate-700 hover:bg-slate-50 hover:text-rose-600 transition-all"
                >
                  Wyloguj
                </button>
              </div>
            ) : (
              <Link
                to="/login"
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-teal-600 hover:bg-teal-700 text-sm font-medium text-white transition-all shadow-sm shadow-teal-600/20"
              >
                Zaloguj się
              </Link>
            )}

            <Link
              to="/koszyk"
              className="relative inline-flex items-center gap-2 px-4 py-2 rounded-xl border border-slate-200 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-all"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              <span>Koszyk</span>
              {count > 0 && (
                <span className="absolute -top-2 -right-2 min-w-5 h-5 px-1 rounded-full bg-teal-600 text-white text-[11px] font-semibold flex items-center justify-center">
                  {count}
                </span>
              )}
            </Link>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  );
};