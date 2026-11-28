import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition-colors ${
    isActive
      ? 'bg-teal-50 text-teal-700 font-semibold'
      : 'text-slate-700 hover:bg-slate-100 hover:text-slate-900'
  }`;

export const Layout = () => {
  const { count } = useCart();
  const { user, loading, isAuthenticated, isKlient, logout } = useAuth();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const isEmployee = isAuthenticated && user !== null && user.typ_konta === 'PRACOWNIK';

  // Zamknięcie sidebara przy zmianie trasy
  useEffect(() => {
    setSidebarOpen(false);
  }, [location.pathname]);

  // Zamknięcie sidebara klawiszem Escape
  useEffect(() => {
    if (!sidebarOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setSidebarOpen(false);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [sidebarOpen]);

  const handleLogout = () => {
    setSidebarOpen(false);
    void logout();
  };

  return (
    <div className="bg-slate-50 min-h-screen text-slate-800 antialiased">
      {/* Górny pasek nawigacji */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
          {/* Lewa strona: hamburger + logo */}
          <div className="flex items-center gap-3 min-w-0">
            <button
              type="button"
              onClick={() => setSidebarOpen((open) => !open)}
              aria-label={sidebarOpen ? 'Zamknij menu' : 'Otwórz menu'}
              aria-expanded={sidebarOpen}
              className="inline-flex items-center justify-center w-10 h-10 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-100 transition-colors shrink-0"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M4 6h16M4 12h16M4 18h16"
                />
              </svg>
            </button>

            <Link to="/" className="flex items-center gap-2.5 min-w-0">
              <div className="w-9 h-9 rounded-xl bg-teal-600 flex items-center justify-center text-white shadow-sm shadow-teal-600/20 shrink-0">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
                  />
                </svg>
              </div>
              <span className="text-base sm:text-lg font-bold text-slate-900 truncate whitespace-nowrap">
                System Apteczny
              </span>
            </Link>
          </div>

          {/* Prawa strona: maksymalnie do prawej krawędzi, jedna linia */}
          <div className="flex items-center gap-3 ml-auto shrink-0 whitespace-nowrap">
            {loading ? (
              <span className="text-xs text-slate-400">Ładowanie...</span>
            ) : isAuthenticated && user ? (
              <>
                {isKlient && (
                  <Link
                    to="/koszyk"
                    className="relative inline-flex items-center gap-2 px-3.5 py-2 rounded-xl border border-slate-200 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth="2"
                        d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
                      />
                    </svg>
                    <span className="hidden sm:inline">Koszyk</span>
                    {count > 0 && (
                      <span className="absolute -top-2 -right-2 min-w-5 h-5 px-1 rounded-full bg-teal-600 text-white text-[11px] font-semibold flex items-center justify-center">
                        {count}
                      </span>
                    )}
                  </Link>
                )}

                <button
                  type="button"
                  onClick={handleLogout}
                  className="inline-flex items-center px-3.5 py-2 rounded-xl border border-slate-200 text-sm font-medium text-slate-700 hover:bg-slate-50 hover:text-rose-600 transition-colors"
                >
                  Wyloguj
                </button>

                <span className="text-sm font-semibold text-slate-900 pl-1">
                  {user.imie} {user.nazwisko}
                </span>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  className="inline-flex items-center px-4 py-2 rounded-xl bg-teal-600 hover:bg-teal-700 text-sm font-medium text-white transition-colors shadow-sm shadow-teal-600/20"
                >
                  Zaloguj się
                </Link>

                <Link
                  to="/koszyk"
                  className="relative inline-flex items-center gap-2 px-3.5 py-2 rounded-xl border border-slate-200 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
                    />
                  </svg>
                  <span className="hidden sm:inline">Koszyk</span>
                  {count > 0 && (
                    <span className="absolute -top-2 -right-2 min-w-5 h-5 px-1 rounded-full bg-teal-600 text-white text-[11px] font-semibold flex items-center justify-center">
                      {count}
                    </span>
                  )}
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* Półprzezroczysty backdrop dla sidebara */}
      <div
        onClick={() => setSidebarOpen(false)}
        aria-hidden="true"
        className={`fixed inset-0 z-40 bg-slate-900/40 backdrop-blur-[1px] transition-opacity duration-200 ${
          sidebarOpen ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'
        }`}
      />

      {/* Wysuwany lewy pasek boczny (Drawer) */}
      <aside
        aria-label="Menu główne"
        className={`fixed top-0 left-0 bottom-0 z-50 w-72 max-w-[85vw] bg-white border-r border-slate-200 shadow-xl flex flex-col transform transition-transform duration-200 ease-out ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="h-16 px-4 border-b border-slate-200 flex items-center justify-between shrink-0">
          <Link
            to="/"
            onClick={() => setSidebarOpen(false)}
            className="flex items-center gap-2.5"
          >
            <div className="w-8 h-8 rounded-lg bg-teal-600 flex items-center justify-center text-white">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
                />
              </svg>
            </div>
            <span className="font-bold text-slate-900">System Apteczny</span>
          </Link>

          <button
            type="button"
            onClick={() => setSidebarOpen(false)}
            aria-label="Zamknij menu"
            className="inline-flex items-center justify-center w-8 h-8 rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto p-4 space-y-6">
          <div className="space-y-1">
            <p className="px-3.5 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
              Nawigacja
            </p>
            <NavLink to="/" end className={navLinkClass} onClick={() => setSidebarOpen(false)}>
              <span>Katalog produktów</span>
            </NavLink>

            {isKlient && (
              <>
                <NavLink
                  to="/zamowienia"
                  className={navLinkClass}
                  onClick={() => setSidebarOpen(false)}
                >
                  <span>Moje zamówienia</span>
                </NavLink>
                <NavLink
                  to="/koszyk"
                  className={navLinkClass}
                  onClick={() => setSidebarOpen(false)}
                >
                  <span>Koszyk</span>
                  {count > 0 && (
                    <span className="px-2 py-0.5 rounded-full bg-teal-100 text-teal-700 text-xs font-semibold">
                      {count}
                    </span>
                  )}
                </NavLink>
              </>
            )}

            {!isAuthenticated && !loading && (
              <>
                <NavLink
                  to="/koszyk"
                  className={navLinkClass}
                  onClick={() => setSidebarOpen(false)}
                >
                  <span>Koszyk</span>
                  {count > 0 && (
                    <span className="px-2 py-0.5 rounded-full bg-teal-100 text-teal-700 text-xs font-semibold">
                      {count}
                    </span>
                  )}
                </NavLink>
                <NavLink
                  to="/login"
                  className={navLinkClass}
                  onClick={() => setSidebarOpen(false)}
                >
                  <span>Zaloguj się</span>
                </NavLink>
              </>
            )}
          </div>

          {isEmployee && (
            <div className="space-y-1">
              <p className="px-3.5 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
                Panel pracownika
              </p>
              <NavLink
                to="/panel/zamowienia"
                className={navLinkClass}
                onClick={() => setSidebarOpen(false)}
              >
                <span>Panel zamówień</span>
              </NavLink>
              <NavLink
                to="/panel/recepty"
                className={navLinkClass}
                onClick={() => setSidebarOpen(false)}
              >
                <span>Recepty</span>
              </NavLink>
              <NavLink
                to="/panel/produkty"
                className={navLinkClass}
                onClick={() => setSidebarOpen(false)}
              >
                <span>Magazyn / Produkty</span>
              </NavLink>
              <NavLink
                to="/panel/klienci"
                className={navLinkClass}
                onClick={() => setSidebarOpen(false)}
              >
                <span>Klienci</span>
              </NavLink>
              <NavLink
                to="/raporty"
                className={navLinkClass}
                onClick={() => setSidebarOpen(false)}
              >
                <span>Raporty</span>
              </NavLink>
            </div>
          )}
        </nav>

        {isAuthenticated && user && (
          <div className="p-4 border-t border-slate-200 space-y-3 shrink-0">
            <p className="px-2 text-sm font-semibold text-slate-900 truncate">
              {user.imie} {user.nazwisko}
            </p>
            <button
              type="button"
              onClick={handleLogout}
              className="w-full inline-flex items-center justify-center px-4 py-2 rounded-xl border border-slate-200 text-sm font-medium text-slate-700 hover:bg-rose-50 hover:border-rose-200 hover:text-rose-700 transition-colors"
            >
              Wyloguj
            </button>
          </div>
        )}
      </aside>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  );
};
