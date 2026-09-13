import React from 'react';
import Modal from './Modal';

const ConfirmDialog = ({ isOpen, onConfirm, onCancel, title, message, confirmText = 'Confirm', cancelText = 'Cancel' }) => {
  return (
    <Modal isOpen={isOpen} onClose={onCancel} title={title} size="sm">
      <div className="mb-6">
        {typeof message === 'string'
          ? <p className="text-gray-600">{message}</p>
          : <div className="text-gray-600">{message}</div>}
      </div>
      <div className="flex justify-end gap-3">
        <button onClick={onCancel} className="btn-secondary">
          {cancelText}
        </button>
        <button onClick={onConfirm} className="btn-primary bg-red-600 hover:bg-red-700">
          {confirmText}
        </button>
      </div>
    </Modal>
  );
};

export default ConfirmDialog;
