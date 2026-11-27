export const zl = (n: number) => n.toFixed(2).replace('.', ',') + ' zł';

export const formatDatePl = (iso: string, month: 'long' | 'short' = 'long') => {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('pl-PL', {
    year: 'numeric',
    month,
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};
