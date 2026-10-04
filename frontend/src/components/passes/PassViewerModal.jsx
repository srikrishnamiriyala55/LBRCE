import React, { useState } from 'react';
import { Download } from 'lucide-react';
import Modal from '../common/Modal';
import PassCard from './PassCard';
import { downloadElementAsPdf } from '../../utils/pdfDownload';

export default function PassViewerModal({ pass, photoUrl, onClose, onDownload, downloading }) {
  const [generating, setGenerating] = useState(false);

  if (!pass) return null;

  const handleDownload = async () => {
    if (onDownload) {
      onDownload();
      return;
    }
    setGenerating(true);
    try {
      await downloadElementAsPdf('bus-pass', `bus-pass-${pass.rollNumber}`);
    } catch (err) {
      console.error('PDF generation failed:', err);
    } finally {
      setGenerating(false);
    }
  };

  const isBusy = downloading || generating;

  return (
    <Modal isOpen={Boolean(pass)} onClose={onClose} title="Student Bus Pass" size="lg">
      <PassCard pass={pass} photoUrl={photoUrl} />
      <div className="mt-4 flex justify-end gap-3">
        <button type="button" onClick={onClose} className="btn-secondary">Close</button>
        <button
          type="button"
          disabled={isBusy}
          onClick={handleDownload}
          className="btn-primary flex items-center gap-2 disabled:opacity-60"
        >
          <Download size={16} />
          {isBusy ? 'Generating PDF...' : 'Download PDF'}
        </button>
      </div>
    </Modal>
  );
}
