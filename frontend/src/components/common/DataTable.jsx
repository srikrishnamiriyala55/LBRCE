import React from 'react';
import EmptyState from './EmptyState';
import LoadingSpinner from './LoadingSpinner';

const DataTable = ({ columns, data, loading, emptyMessage = 'No data found' }) => {
  if (loading) {
    return <LoadingSpinner message="Loading data..." />;
  }

  if (!data || data.length === 0) {
    return <EmptyState title="No Data" message={emptyMessage} />;
  }

  return (
    <div className="w-full overflow-x-auto overscroll-x-contain rounded-lg border border-gray-200" role="region" aria-label="Scrollable data table" tabIndex="0">
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
          {data.map((row, rowIndex) => (
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
    </div>
  );
};

export default DataTable;
