export const CategoryIcon = ({
  categoryName,
  className = 'w-12 h-12',
}: {
  categoryName?: string;
  className?: string;
}) => {
  const name = (categoryName ?? '').toLowerCase();

  // Witaminy i suplementy — liść
  if (name.includes('witamin') || name.includes('suplement')) {
    return (
      <svg className={`${className} text-teal-600/80`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
          d="M5 21c.5-4.5 2.5-8 7-10m0 0c4.5-2 7-5 7-8-4.5 0-8.5 1.5-11 4.5S5 14 5 21z"
        />
      </svg>
    );
  }

  // Okulistyka — kropla
  if (name.includes('okulist') || name.includes('ocz')) {
    return (
      <svg className={`${className} text-sky-600/80`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
          d="M12 3.5c-3.2 4.5-6 7.5-6 11a6 6 0 1012 0c0-3.5-2.8-6.5-6-11z"
        />
      </svg>
    );
  }

  // Dermatologia — plaster
  if (name.includes('dermatolog') || name.includes('maś') || name.includes('skór')) {
    return (
      <svg className={`${className} text-amber-600/80`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
          d="M7.5 4.5l12 12a3.5 3.5 0 01-5 5l-12-12a3.5 3.5 0 015-5zm2 6l4 4m-1-5l-4 4"
        />
      </svg>
    );
  }

  // Układ pokarmowy / syropy — butelka
  if (name.includes('pokarm') || name.includes('syrop') || name.includes('traw')) {
    return (
      <svg className={`${className} text-emerald-600/80`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
          d="M9 3h6m-5 0v3.2a2 2 0 01-.59 1.41L7.59 9.41A2 2 0 007 10.83V19a2 2 0 002 2h6a2 2 0 002-2v-8.17a2 2 0 00-.59-1.42l-1.82-1.8A2 2 0 0114 6.2V3m-4 11h4m-2-2v4"
        />
      </svg>
    );
  }

  // Antybiotyki / przeciwbólowe — kapsułka
  if (name.includes('przeciwból') || name.includes('antybiot')) {
    return (
      <svg className={`${className} text-indigo-600/80`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
          d="M10.5 6.5l7 7a4.95 4.95 0 11-7 7l-7-7a4.95 4.95 0 017-7zm-3.5 3.5l7 7"
        />
      </svg>
    );
  }

  // Domyślna ikona leku (krzyżyk apteczny)
  return (
    <svg className={`${className} text-teal-600/75`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.5"
        d="M9 4h6v5h5v6h-5v5H9v-5H4V9h5V4z"
      />
    </svg>
  );
};
