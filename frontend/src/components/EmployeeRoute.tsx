import type { ReactNode } from 'react';
import { Link, Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const EmployeeRoute = ({ children }: { children?: ReactNode }) => {
  const { user, loading, isAuthenticated } = useAuth();

  if (loading) {
    return (
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/80 py-20 flex flex-col items-center justify-center gap-3">
        <div className="w-9 h-9 border-[3px] border-teal-600 border-t-transparent rounded-full animate-spin" />
        <p className="text-sm font-medium text-slate-600">Sprawdzanie uprawnień...</p>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/panel/login" replace />;
  }

  if (user.typ_konta !== 'PRACOWNIK') {
    return (
      <div className="max-w-lg mx-auto bg-white rounded-2xl shadow-sm border border-slate-200/80 p-8 text-center space-y-4">
        <h2 className="text-lg font-bold text-slate-900">
          Brak uprawnień (wymagane konto pracownika)
        </h2>
        <p className="text-sm text-slate-500">
          Ta sekcja jest dostępna wyłącznie dla zalogowanych pracowników apteki.
        </p>
        <Link
          to="/"
          className="inline-flex items-center justify-center px-5 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-sm font-medium transition-all"
        >
          Wróć na stronę główną
        </Link>
      </div>
    );
  }

  return children ? <>{children}</> : <Outlet />;
};
