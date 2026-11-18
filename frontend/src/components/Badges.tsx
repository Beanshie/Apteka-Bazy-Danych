import type { StatusZamowienia } from '../types/zamowienie';

export const RxBadge = ({ required }: { required: boolean }) =>
  required ? (
    <span className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-semibold bg-purple-50 text-purple-700 border border-purple-200">
      Rx - Wymagana
    </span>
  ) : (
    <span className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-medium bg-slate-100 text-slate-600 border border-slate-200">
      Bez recepty (OTC)
    </span>
  );

export const StockBadge = ({ qty }: { qty: number }) => {
  const [style, dot, label] =
    qty === 0
      ? ['bg-rose-50 text-rose-700 border-rose-200', 'bg-rose-500', 'Brak (0 szt.)']
      : qty < 10
        ? ['bg-amber-50 text-amber-700 border-amber-200', 'bg-amber-500', `Ostatnie ${qty} szt.`]
        : ['bg-emerald-50 text-emerald-700 border-emerald-200', 'bg-emerald-500', `Dostępny (${qty} szt.)`];

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium border ${style}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${dot}`} />
      {label}
    </span>
  );
};

const ORDER_STATUS_CONFIG: Record<
  StatusZamowienia,
  { style: string; dot: string; label: string }
> = {
  NOWE: {
    style: 'bg-sky-50 text-sky-700 border-sky-200',
    dot: 'bg-sky-500',
    label: 'Nowe',
  },
  OPLACONE: {
    style: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    dot: 'bg-emerald-500',
    label: 'Opłacone',
  },
  W_REALIZACJI: {
    style: 'bg-amber-50 text-amber-700 border-amber-200',
    dot: 'bg-amber-500',
    label: 'W realizacji',
  },
  WYSLANE: {
    style: 'bg-amber-50 text-amber-700 border-amber-200',
    dot: 'bg-amber-500',
    label: 'Wysłane',
  },
  ZREALIZOWANE: {
    style: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    dot: 'bg-emerald-500',
    label: 'Zrealizowane',
  },
  ANULOWANE: {
    style: 'bg-rose-50 text-rose-700 border-rose-200',
    dot: 'bg-rose-500',
    label: 'Anulowane',
  },
};

export const OrderStatusBadge = ({ status }: { status: StatusZamowienia }) => {
  const cfg = ORDER_STATUS_CONFIG[status] ?? {
    style: 'bg-slate-100 text-slate-600 border-slate-200',
    dot: 'bg-slate-400',
    label: status,
  };

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold border ${cfg.style}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${cfg.dot}`} />
      {cfg.label}
    </span>
  );
};
