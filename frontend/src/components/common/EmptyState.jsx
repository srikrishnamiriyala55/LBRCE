import React from 'react';
import { Inbox } from 'lucide-react';

const EmptyState = ({ icon: Icon = Inbox, title = 'No Data', message }) => {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center bg-gray-50 rounded-lg border border-dashed border-gray-300">
      <Icon className="w-12 h-12 text-gray-400 mb-4" />
      <h3 className="text-lg font-medium text-gray-900 mb-1">{title}</h3>
      {message && <p className="text-gray-500 max-w-sm">{message}</p>}
    </div>
  );
};

export default EmptyState;
