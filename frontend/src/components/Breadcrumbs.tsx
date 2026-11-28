import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface Crumb {
  label: string;
  to?: string;
}

export const Breadcrumbs: React.FC = () => {
  const { pathname } = useLocation();
  const { user } = useAuth();

  if (pathname === '/') return null;

  const isEmployee = user?.typ_konta === 'PRACOWNIK';
  const crumbs: Crumb[] = [{ label: 'Strona główna', to: '/' }];

  if (pathname === '/koszyk') {
    crumbs.push({ label: 'Koszyk' });
  } else if (pathname === '/zamowienie/nowe' || pathname === '/checkout') {
    crumbs.push({ label: 'Koszyk', to: '/koszyk' });
    crumbs.push({ label: 'Zamówienie' });
  } else if (pathname === '/zamowienia') {
    crumbs.push({ label: 'Moje zamówienia' });
  } else if (/^\/zamowienia\/\d+$/.test(pathname)) {
    const id = pathname.split('/')[2];
    if (isEmployee) {
      crumbs.push({ label: 'Panel pracownika' });
      crumbs.push({ label: 'Zamówienia', to: '/panel/zamowienia' });
    } else {
      crumbs.push({ label: 'Moje zamówienia', to: '/zamowienia' });
    }
    crumbs.push({ label: `Szczegóły zamówienia #${id}` });
  } else if (/^\/produkty\/\d+$/.test(pathname)) {
    crumbs.push({ label: 'Szczegóły produktu' });
  } else if (pathname === '/login') {
    crumbs.push({ label: 'Logowanie' });
  } else if (pathname === '/panel/login') {
    crumbs.push({ label: 'Panel pracownika' });
    crumbs.push({ label: 'Logowanie' });
  } else if (pathname === '/rejestracja') {
    crumbs.push({ label: 'Rejestracja' });
  } else if (pathname === '/panel/zamowienia') {
    crumbs.push({ label: 'Panel pracownika' });
    crumbs.push({ label: 'Zamówienia' });
  } else if (pathname === '/panel/recepty') {
    crumbs.push({ label: 'Panel pracownika' });
    crumbs.push({ label: 'Recepty' });
  } else if (pathname === '/panel/produkty') {
    crumbs.push({ label: 'Panel pracownika' });
    crumbs.push({ label: 'Magazyn i produkty' });
  } else if (pathname === '/panel/klienci') {
    crumbs.push({ label: 'Panel pracownika' });
    crumbs.push({ label: 'Klienci' });
  } else if (pathname === '/raporty') {
    crumbs.push({ label: 'Panel pracownika' });
    crumbs.push({ label: 'Raporty' });
  } else {
    const segments = pathname.split('/').filter(Boolean);
    segments.forEach((seg) => crumbs.push({ label: seg }));
  }

  return (
    <div className="bg-white/70 border-b border-slate-200/70">
      <nav
        aria-label="Ścieżka nawigacji"
        className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-2.5"
      >
        <ol className="flex flex-wrap items-center gap-1.5 text-xs text-slate-500">
          {crumbs.map((crumb, idx) => {
            const isLast = idx === crumbs.length - 1;
            return (
              <li key={`${crumb.label}-${idx}`} className="inline-flex items-center gap-1.5">
                {idx > 0 && <span className="text-slate-300 select-none">/</span>}
                {crumb.to && !isLast ? (
                  <Link
                    to={crumb.to}
                    className="hover:text-teal-700 transition-colors font-medium"
                  >
                    {crumb.label}
                  </Link>
                ) : (
                  <span className={isLast ? 'font-semibold text-slate-800' : 'text-slate-500'}>
                    {crumb.label}
                  </span>
                )}
              </li>
            );
          })}
        </ol>
      </nav>
    </div>
  );
};
