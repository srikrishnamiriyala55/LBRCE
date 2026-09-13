import React from 'react';
import { X } from 'lucide-react';

const Modal = ({ isOpen, onClose, title, children, size = 'md' }) => {
  if (!isOpen) return null;

  const sizeClasses = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-full items-end sm:items-center justify-center p-2 sm:p-4 text-center">
        <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" onClick={onClose} />
        
        <div className={`relative flex max-h-[calc(100dvh-1rem)] w-full flex-col transform overflow-hidden rounded-t-xl sm:rounded-lg bg-white text-left shadow-xl transition-all ${sizeClasses[size]}`} role="dialog" aria-modal="true" aria-label={title}>
          <div className="flex shrink-0 items-center justify-between gap-3 px-4 sm:px-6 py-4 border-b border-gray-200">
            <h3 className="min-w-0 text-base sm:text-lg font-semibold text-gray-900">{title}</h3>
            <button onClick={onClose} aria-label="Close dialog" className="shrink-0 p-1 text-gray-400 hover:text-gray-500">
              <X size={20} />
            </button>
          </div>
          <div className="overflow-y-auto px-4 sm:px-6 py-4">
            {children}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Modal;
