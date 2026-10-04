import React, { useEffect, useState } from 'react';
import Modal from '../common/Modal';
import StatusBadge from '../common/StatusBadge';
import LoadingSpinner from '../common/LoadingSpinner';
import api from '../../utils/axios';
import { User, Phone, Mail, MapPin, Calendar, BookOpen, ShieldCheck, Bus, Heart } from 'lucide-react';

export default function StudentApplicationDetailModal({
  isOpen,
  onClose,
  application,
  role = 'incharge', // 'admin' | 'incharge'
  onApprove,
  onReject,
  onEdit
}) {
  const [student, setStudent] = useState(null);
  const [photoUrl, setPhotoUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    let objectUrl = '';

    if (isOpen && application?.id) {
      setLoading(true);
      setError('');
      setStudent(null);
      setPhotoUrl('');

      const endpointPrefix = role === 'admin' ? '/admin' : '/incharge';

      // 1. Fetch complete student details
      api.get(`${endpointPrefix}/applications/${application.id}/student-details`)
        .then((res) => {
          if (active) {
            setStudent(res.data);
          }
        })
        .catch((err) => {
          if (active) {
            setError(err.response?.data?.message || 'Unable to load student profile');
          }
        })
        .finally(() => {
          if (active) setLoading(false);
        });

      // 2. Fetch student photo
      api.get(`${endpointPrefix}/applications/${application.id}/student-photo`, { responseType: 'blob' })
        .then((res) => {
          if (active && res.data) {
            objectUrl = URL.createObjectURL(res.data);
            setPhotoUrl(objectUrl);
          }
        })
        .catch(() => {
          // Photo not found or unavailable, fallback gracefully
        });
    }

    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [isOpen, application?.id, role]);

  if (!isOpen || !application) return null;

  const formatDate = (val) => {
    if (!val) return '—';
    try {
      const d = new Date(val);
      return Number.isNaN(d.getTime()) ? val : d.toLocaleDateString();
    } catch {
      return val;
    }
  };

  const formatDateTime = (val) => {
    if (!val) return '—';
    try {
      const d = new Date(val);
      return Number.isNaN(d.getTime()) ? val : d.toLocaleString();
    } catch {
      return val;
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Complete Student & Application Details" size="xl">
      <div className="space-y-6">
        {loading ? (
          <div className="py-12 flex flex-col items-center justify-center gap-3">
            <LoadingSpinner size="lg" />
            <p className="text-sm text-gray-500">Loading student record...</p>
          </div>
        ) : error ? (
          <div className="p-4 rounded-lg bg-red-50 text-red-700 text-sm border border-red-200">
            {error}
          </div>
        ) : (
          <>
            {/* Header: Photo + Core Identification */}
            <div className="flex flex-col sm:flex-row items-center sm:items-start gap-5 p-4 rounded-xl bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-100">
              <div className="relative w-28 h-32 rounded-lg overflow-hidden border-2 border-white shadow-md bg-gray-100 flex-shrink-0 flex items-center justify-center">
                {photoUrl ? (
                  <img
                    src={photoUrl}
                    alt={student?.name || application.studentName || 'Student'}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="flex flex-col items-center justify-center text-gray-400 p-2 text-center">
                    <User size={36} className="text-gray-300 mb-1" />
                    <span className="text-[10px] font-medium leading-tight">No Photo Available</span>
                  </div>
                )}
              </div>

              <div className="flex-1 text-center sm:text-left space-y-1">
                <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2">
                  <h3 className="text-xl font-bold text-gray-900">
                    {student?.name || application.studentName || '—'}
                  </h3>
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-700 text-white tracking-wide">
                    {student?.rollNumber || application.rollNumber}
                  </span>
                </div>
                <p className="text-sm font-medium text-blue-900">
                  {student?.branch || 'Department'} · Year {student?.year || '—'} · Semester {student?.semester || '—'}
                </p>
                <div className="flex flex-wrap items-center justify-center sm:justify-start gap-4 pt-2 text-xs text-gray-600">
                  <span className="flex items-center gap-1.5">
                    <Mail size={14} className="text-blue-600" />
                    {student?.email || '—'}
                  </span>
                  <span className="flex items-center gap-1.5">
                    <Phone size={14} className="text-blue-600" />
                    {student?.phoneNumber || '—'}
                  </span>
                </div>
              </div>
            </div>

            {/* Grid of Student Profile Details */}
            <div className="rounded-xl border border-gray-200 bg-white p-4 space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-gray-500 flex items-center gap-1.5">
                <BookOpen size={15} className="text-blue-600" /> Personal & Academic Information
              </h4>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm">
                <div>
                  <span className="block text-xs text-gray-500">Date of Birth</span>
                  <span className="font-medium text-gray-800">{formatDate(student?.dob)}</span>
                </div>
                <div>
                  <span className="block text-xs text-gray-500">Gender</span>
                  <span className="font-medium text-gray-800">{student?.gender || '—'}</span>
                </div>
                <div>
                  <span className="block text-xs text-gray-500">Blood Group</span>
                  <span className="font-medium text-gray-800 flex items-center gap-1">
                    <Heart size={13} className="text-red-500" />
                    {student?.bloodGroup || '—'}
                  </span>
                </div>
                <div>
                  <span className="block text-xs text-gray-500">Parent / Guardian Name</span>
                  <span className="font-medium text-gray-800">{student?.parentName || '—'}</span>
                </div>
                <div>
                  <span className="block text-xs text-gray-500">Parent Contact</span>
                  <span className="font-medium text-gray-800">{student?.parentPhoneNumber || '—'}</span>
                </div>
                <div>
                  <span className="block text-xs text-gray-500">Student Status</span>
                  <span className={`inline-block px-2 py-0.5 rounded text-xs font-semibold ${student?.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}`}>
                    {student?.status || 'ACTIVE'}
                  </span>
                </div>
                <div className="col-span-2 sm:col-span-3">
                  <span className="block text-xs text-gray-500 flex items-center gap-1">
                    <MapPin size={13} className="text-blue-600" /> Residential Address
                  </span>
                  <span className="font-medium text-gray-800 whitespace-pre-wrap">{student?.address || '—'}</span>
                </div>
              </div>
            </div>

            {/* Bus Application Information */}
            <div className="rounded-xl border border-gray-200 bg-white p-4 space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-gray-500 flex items-center gap-1.5">
                <Bus size={15} className="text-blue-600" /> Applied Bus & Route Details
              </h4>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm">
                <div>
                  <span className="block text-xs text-gray-500">Assigned Bus</span>
                  <span className="font-bold text-blue-900">{application.busNumber || '—'}</span>
                </div>
                <div>
                  <span className="block text-xs text-gray-500">Route</span>
                  <span className="font-medium text-gray-800">
                    {application.startingPoint && application.endingPoint
                      ? `${application.startingPoint} → ${application.endingPoint}`
                      : '—'}
                  </span>
                </div>
                <div>
                  <span className="block text-xs text-gray-500">Boarding Point</span>
                  <span className="font-medium text-gray-800">
                    {application.boardingPoint?.stationName || application.boardingPointName || '—'}
                  </span>
                </div>
                <div>
                  <span className="block text-xs text-gray-500">Academic Year</span>
                  <span className="font-medium text-gray-800">{application.academicYear || '—'}</span>
                </div>
                <div>
                  <span className="block text-xs text-gray-500">Application Status</span>
                  <div className="mt-0.5">
                    <StatusBadge status={application.status} />
                  </div>
                </div>
                <div>
                  <span className="block text-xs text-gray-500">Applied Date</span>
                  <span className="font-medium text-gray-800">
                    {formatDateTime(application.appliedAt || application.applicationDate || application.createdAt)}
                  </span>
                </div>
                {application.reviewedAt && (
                  <div>
                    <span className="block text-xs text-gray-500">Reviewed Date</span>
                    <span className="font-medium text-gray-800">{formatDateTime(application.reviewedAt)}</span>
                  </div>
                )}
                {application.remarks && (
                  <div className="col-span-2 sm:col-span-3">
                    <span className="block text-xs text-gray-500">Remarks / Decision Reason</span>
                    <span className={`font-medium text-xs px-2.5 py-1 rounded inline-block mt-0.5 ${application.status === 'REJECTED' ? 'bg-red-50 text-red-700 border border-red-200' : 'bg-gray-100 text-gray-800'}`}>
                      {application.remarks}
                    </span>
                  </div>
                )}
              </div>
            </div>
          </>
        )}

        {/* Modal Actions */}
        <div className="flex flex-wrap items-center justify-end gap-3 pt-2 border-t border-gray-100">
          {onEdit && ['PENDING', 'UNDER_REVIEW'].includes(application.status) && (
            <button
              type="button"
              className="btn-secondary"
              onClick={() => {
                onClose();
                onEdit(application);
              }}
            >
              Edit Application
            </button>
          )}
          {onApprove && ['PENDING', 'UNDER_REVIEW'].includes(application.status) && (
            <button
              type="button"
              className="btn-primary bg-green-600 hover:bg-green-700"
              onClick={() => {
                onClose();
                onApprove(application);
              }}
            >
              Approve Application
            </button>
          )}
          {onReject && ['PENDING', 'UNDER_REVIEW'].includes(application.status) && (
            <button
              type="button"
              className="btn-secondary bg-red-50 text-red-700 hover:bg-red-100 border-red-200"
              onClick={() => {
                onClose();
                onReject(application);
              }}
            >
              Reject Application
            </button>
          )}
          <button type="button" className="btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </Modal>
  );
}
