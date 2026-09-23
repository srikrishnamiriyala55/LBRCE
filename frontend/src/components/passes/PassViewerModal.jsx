import React from 'react';
import { Download } from 'lucide-react';
import Modal from '../common/Modal';
import PassCard from './PassCard';

export default function PassViewerModal({ pass, photoUrl, onClose, onDownload, downloading }) {
  if (!pass) return null;
  return <Modal isOpen={Boolean(pass)} onClose={onClose} title="Student Bus Pass" size="lg">
    <PassCard pass={pass} photoUrl={photoUrl}/>
    <div className="mt-4 flex justify-end gap-3">
      <button type="button" onClick={onClose} className="btn-secondary">Close</button>
      <button type="button" disabled={downloading} onClick={onDownload} className="btn-primary flex items-center gap-2 disabled:opacity-60"><Download size={16} />{downloading ? 'Downloading...' : 'Download PDF'}</button>
    </div>
  </Modal>;
}
