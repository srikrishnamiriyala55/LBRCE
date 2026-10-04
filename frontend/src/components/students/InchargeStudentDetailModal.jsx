import React, { useEffect, useState } from 'react';
import Modal from '../common/Modal';
import StatusBadge from '../common/StatusBadge';
import api from '../../utils/axios';
import { formatINR } from '../../utils/currency';
import { User, Mail, Phone, MapPin, Heart, BookOpen, Bus, CreditCard, Ticket } from 'lucide-react';

export default function InchargeStudentDetailModal({ isOpen, onClose, student }) {
  const [photoUrl, setPhotoUrl] = useState('');

  useEffect(() => {
    let objectUrl = '';
    if (isOpen && student?.studentId) {
      setPhotoUrl('');
      api.get(`/incharge/students/${student.studentId}/photo`, { responseType: 'blob' })
        .then((res) => {
          if (res.data) {
            objectUrl = URL.createObjectURL(res.data);
            setPhotoUrl(objectUrl);
          }
        })
        .catch(() => {
          // Photo not found
        });
    }
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [isOpen, student?.studentId]);

  if (!isOpen || !student) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Assigned Student Details" size="lg">
      <div className="space-y-5">
        {/* Header with Photo */}
        <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4 p-4 rounded-xl bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-100">
          <div className="relative w-24 h-28 rounded-lg overflow-hidden border-2 border-white shadow-md bg-gray-100 shrink-0 flex items-center justify-center">
            {photoUrl ? (
              <img src={photoUrl} alt={student.name || 'Student photo'} className="w-full h-full object-cover" />
            ) : (
              <div className="flex flex-col items-center justify-center text-gray-400 p-2 text-center">
                <User size={32} className="text-gray-300 mb-1" />
                <span className="text-[9px] font-medium leading-tight">No Photo</span>
              </div>
            )}
          </div>
          <div className="flex-1 text-center sm:text-left space-y-1">
            <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2">
              <h3 className="text-xl font-bold text-gray-900">{student.name}</h3>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-700 text-white">
                {student.rollNumber}
              </span>
            </div>
            <p className="text-sm font-medium text-blue-900">
              {student.branch || 'Department'} · Year {student.year || '—'} · Semester {student.semester || '—'}
            </p>
            <div className="flex flex-wrap items-center justify-center sm:justify-start gap-4 pt-2 text-xs text-gray-600">
              {student.email && (
                <span className="flex items-center gap-1.5">
                  <Mail size={14} className="text-blue-600" />
                  {student.email}
                </span>
              )}
              {student.phoneNumber && (
                <span className="flex items-center gap-1.5">
                  <Phone size={14} className="text-blue-600" />
                  {student.phoneNumber}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Bus & Transit Status */}
        <div className="rounded-xl border border-gray-200 bg-white p-4 space-y-3">
          <h4 className="text-xs font-bold uppercase tracking-wider text-gray-500 flex items-center gap-1.5">
            <Bus size={15} className="text-blue-600" /> Bus & Boarding Information
          </h4>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm">
            <div>
              <span className="block text-xs text-gray-500">Boarding Point</span>
              <span className="font-bold text-gray-800">{student.boardingPoint || '—'}</span>
            </div>
            <div>
              <span className="block text-xs text-gray-500">Fee Status</span>
              <div className="mt-0.5"><StatusBadge status={student.feeStatus} /></div>
            </div>
            <div>
              <span className="block text-xs text-gray-500">Bus Pass Status</span>
              <div className="mt-0.5"><StatusBadge status={student.passStatus} /></div>
            </div>
            <div>
              <span className="block text-xs text-gray-500">Required Fee</span>
              <span className="font-medium text-gray-800">{formatINR(student.requiredFee)}</span>
            </div>
            <div>
              <span className="block text-xs text-gray-500">Paid Amount</span>
              <span className="font-semibold text-green-700">{formatINR(student.paidAmount)}</span>
            </div>
            <div>
              <span className="block text-xs text-gray-500">Remaining Balance</span>
              <span className="font-semibold text-red-600">{formatINR(student.remainingFee)}</span>
            </div>
          </div>
        </div>

        {/* Personal & Parent Info */}
        <div className="rounded-xl border border-gray-200 bg-white p-4 space-y-3">
          <h4 className="text-xs font-bold uppercase tracking-wider text-gray-500 flex items-center gap-1.5">
            <BookOpen size={15} className="text-blue-600" /> Personal & Guardian Details
          </h4>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm">
            <div>
              <span className="block text-xs text-gray-500">Date of Birth</span>
              <span className="font-medium text-gray-800">{student.dob || '—'}</span>
            </div>
            <div>
              <span className="block text-xs text-gray-500">Gender</span>
              <span className="font-medium text-gray-800">{student.gender || '—'}</span>
            </div>
            <div>
              <span className="block text-xs text-gray-500">Blood Group</span>
              <span className="font-medium text-gray-800 flex items-center gap-1">
                <Heart size={13} className="text-red-500" /> {student.bloodGroup || '—'}
              </span>
            </div>
            <div>
              <span className="block text-xs text-gray-500">Parent / Guardian Name</span>
              <span className="font-medium text-gray-800">{student.parentName || '—'}</span>
            </div>
            <div>
              <span className="block text-xs text-gray-500">Parent Contact</span>
              <span className="font-medium text-gray-800">{student.parentPhoneNumber || '—'}</span>
            </div>
            <div className="col-span-2 sm:col-span-3">
              <span className="block text-xs text-gray-500 flex items-center gap-1">
                <MapPin size={13} className="text-blue-600" /> Residential Address
              </span>
              <span className="font-medium text-gray-800 whitespace-pre-wrap">{student.address || '—'}</span>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-2 border-t border-gray-100">
          <button type="button" className="btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </Modal>
  );
}
