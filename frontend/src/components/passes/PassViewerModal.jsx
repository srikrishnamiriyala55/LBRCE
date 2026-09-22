import React from 'react';
import { Download } from 'lucide-react';
import Modal from '../common/Modal';
import StatusBadge from '../common/StatusBadge';

export default function PassViewerModal({ pass, photoUrl, onClose, onDownload, downloading }) {
  if (!pass) return null;
  const item = (label, value) => <div><p className="text-xs uppercase text-gray-500">{label}</p><p className="font-medium text-gray-900">{value || '—'}</p></div>;
  return <Modal isOpen={Boolean(pass)} onClose={onClose} title="Student Bus Pass" size="lg">
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <div className="bg-blue-800 p-4 text-center text-white">
        <img src="/logo.jpg" alt="LBRCE Logo" className="mx-auto mb-2 h-14 w-14 rounded-full bg-white p-1" />
        <h2 className="font-bold">LAKIREDDY BALI REDDY COLLEGE OF ENGINEERING</h2>
        <p className="text-sm text-blue-200">DIGITAL BUS PASS</p>
      </div>
      <div className="p-4 sm:p-6">
        <div className="mb-5 flex flex-col items-center gap-3 sm:flex-row sm:items-start sm:justify-between">
          {photoUrl ? <img src={photoUrl} alt={`${pass.studentName} student`} className="h-32 w-28 rounded-lg border-2 border-blue-100 object-cover" /> : <div className="flex h-32 w-28 items-center justify-center rounded-lg border bg-gray-50 text-center text-xs text-gray-400">Photo unavailable</div>}
          <div className="text-center sm:text-right"><p className="text-xs text-gray-500">Pass Number</p><p className="font-bold">{pass.passNumber}</p><div className="mt-2"><StatusBadge status={pass.status} /></div></div>
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {item('Student Name', pass.studentName)}{item('Roll Number', pass.rollNumber)}
          {item('Branch', pass.branch)}{item('Year / Semester', `${pass.year || '—'} / ${pass.semester || '—'}`)}
          {item('Phone Number', pass.phoneNumber)}{item('Bus Number', pass.busNumber)}
          {item('Starting Point', pass.startingPoint)}{item('Ending Point', pass.endingPoint)}
          {item('Boarding Point', pass.boardingPoint)}
          {item('Academic Year', pass.academicYear)}{item('Valid Until', pass.validUntil)}
        </div>
      </div>
    </div>
    <div className="mt-4 flex justify-end gap-3">
      <button type="button" onClick={onClose} className="btn-secondary">Close</button>
      <button type="button" disabled={downloading} onClick={onDownload} className="btn-primary flex items-center gap-2 disabled:opacity-60"><Download size={16} />{downloading ? 'Downloading...' : 'Download PDF'}</button>
    </div>
  </Modal>;
}
