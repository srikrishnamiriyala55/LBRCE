import React, { useMemo, useState } from 'react';
import { Search, X } from 'lucide-react';
import EmptyState from './EmptyState';
import LoadingSpinner from './LoadingSpinner';

const DataTable = ({ columns, data, loading, emptyMessage = 'No data found', searchable = true, searchPlaceholder = 'Search these records...' }) => {
  const [search, setSearch] = useState('');
  const filteredData = useMemo(() => {
    const term = search.trim().toLocaleLowerCase();
    if (!term) return data || [];
    return (data || []).filter((row) => Object.values(row || {}).some((value) => {
      if (value === null || value === undefined || typeof value === 'object') return false;
      return String(value).toLocaleLowerCase().includes(term);
    }));
  }, [data, search]);

  if (loading) {
    return <LoadingSpinner message="Loading data..." />;
  }

  if (!data || data.length === 0) {
    return <EmptyState title="No Data" message={emptyMessage} />;
  }

  return (
    <div className="space-y-3">
      {searchable && <div className="relative max-w-lg">
        <Search aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
        <input
          type="text"
          inputMode="search"
          className="input-field"
          style={{ paddingLeft: '2.75rem', paddingRight: '2.5rem' }}
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder={searchPlaceholder}
          aria-label={searchPlaceholder}
        />
        {search && <button type="button" aria-label="Clear search" onClick={() => setSearch('')} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-700"><X size={17} /></button>}
      </div>}
      {filteredData.length === 0 ? <EmptyState title="No Matching Records" message="No records match the entered search." /> : <div className="w-full overflow-x-auto overscroll-x-contain rounded-lg border border-gray-200" role="region" aria-label="Scrollable data table" tabIndex="0">
      <table className="min-w-max w-full divide-y divide-gray-200 bg-white">
        <thead className="bg-gray-50">
          <tr>
            {columns.map((col, index) => (
              <th
                key={index}
                className="px-3 sm:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {filteredData.map((row, rowIndex) => (
            <tr key={rowIndex} className="hover:bg-gray-50">
              {columns.map((col, colIndex) => (
                <td key={colIndex} className="px-3 sm:px-6 py-3 sm:py-4 whitespace-nowrap text-sm text-gray-700">
                  {col.render ? col.render(row, rowIndex) : row[col.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      </div>}
    </div>
  );
};

export default DataTable;
