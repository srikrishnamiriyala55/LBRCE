import React from 'react';
import { Loader2 } from 'lucide-react';

const LoadingSpinner = ({ message = 'Loading...' }) => {
  return (
    <div className="flex flex-col items-center justify-center p-8 space-y-4">
      <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
      {message && <p className="text-gray-500 font-medium">{message}</p>}
    </div>
  );
};

export default LoadingSpinner;
