import React, { useEffect, useState } from 'react';
import Modal from '../common/Modal';
import StatusBadge from '../common/StatusBadge';
import LoadingSpinner from '../common/LoadingSpinner';
import api from '../../utils/axios';
import { formatINR } from '../../utils/currency';
import {
  User, Phone, Mail, MapPin, BookOpen, ShieldCheck, Bus, Heart,
  CreditCard, Ticket, ArrowLeftRight, Calendar, AlertCircle
} from 'lucide-react';

export default function StudentDetailModal({ isOpen, onClose, studentId, initialStudent }) {
  const [data, setData] = useState(null);
  const [photoUrl, setPhotoUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('overview'); // 'overview' | 'transport' | 'fees' | 'passes' | 'transfers'
  const [selectedYearId,setSelectedYearId]=useState('');

  useEffect(() => {
    let active = true;
    let objectUrl = '';

    if (isOpen && studentId) {
      setLoading(true);
      setError('');
      setData(null);
      setPhotoUrl('');
      setActiveTab('overview');

      // 1. Fetch complete transport & student details
      api.get(`/admin/students/${studentId}/transport`)
        .then((res) => {
          if (active) {
            setData(res.data);const years=res.data?.history||[];setSelectedYearId(String((years.find(y=>y.active)||years[0])?.academicYearId||''));
          }
        })
        .catch((err) => {
          if (active) {
            setError(err.response?.data?.message || 'Unable to load student transport details');
          }
        })
        .finally(() => {
          if (active) setLoading(false);
        });

      // 2. Fetch student photo
      api.get(`/admin/students/${studentId}/photo`, { responseType: 'blob' })
        .then((res) => {
          if (active && res.data) {
            objectUrl = URL.createObjectURL(res.data);
            setPhotoUrl(objectUrl);
          }
        })
        .catch(() => {
          // Photo not found or unavailable
        });
    }

    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [isOpen, studentId]);

  if (!isOpen) return null;

  const student = data?.student || initialStudent;
  const years=data?.history||[];
  const selectedHistory=years.find(y=>String(y.academicYearId)===selectedYearId);
  const assignments = selectedHistory?.allocations || data?.assignments || [];
  const fees = selectedHistory?.fees || data?.fees || [];
  const passes = selectedHistory?.passes || data?.passes || [];
  const transfers = selectedHistory?.transfers || data?.transfers || [];
  const applications=selectedHistory?.applications||data?.applications||[];
  const payments=selectedHistory?.payments||data?.payments||[];

  const formatDate = (val) => {
    if (!val) return '—';
    try {
      const d = new Date(val);
      return Number.isNaN(d.getTime()) ? val : d.toLocaleDateString();
    } catch {
      return val;
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Complete Student & Transportation Profile" size="xl">
      <div className="space-y-5">
        {loading ? (
          <div className="py-12 flex flex-col items-center justify-center gap-3">
            <LoadingSpinner size="lg" />
            <p className="text-sm text-gray-500">Loading student transport records...</p>
          </div>
        ) : error ? (
          <div className="p-4 rounded-lg bg-red-50 text-red-700 text-sm border border-red-200">
            {error}
          </div>
        ) : (
          <>
            {/* Header: Photo + Student Identification */}
            <div className="flex flex-col sm:flex-row items-center sm:items-start gap-5 p-4 rounded-xl bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-100">
              <div className="relative w-24 h-28 rounded-lg overflow-hidden border-2 border-white shadow-md bg-gray-100 shrink-0 flex items-center justify-center">
                {photoUrl ? (
                  <img
                    src={photoUrl}
                    alt={student?.name || 'Student photo'}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="flex flex-col items-center justify-center text-gray-400 p-2 text-center">
                    <User size={32} className="text-gray-300 mb-1" />
                    <span className="text-[9px] font-medium leading-tight">No Photo</span>
                  </div>
                )}
              </div>

              <div className="flex-1 text-center sm:text-left space-y-1">
                <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2">
                  <h3 className="text-xl font-bold text-gray-900">{student?.name || '—'}</h3>
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-700 text-white tracking-wide">
                    {student?.rollNumber || '—'}
                  </span>
                  <span className={`px-2 py-0.5 rounded text-xs font-semibold ${student?.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}`}>
                    {student?.status || 'ACTIVE'}
                  </span>
                </div>
                <p className="text-sm font-medium text-blue-900">
                  {selectedHistory?.branch || student?.branch || 'Department'} · Year {selectedHistory?.year || student?.year || '—'} · Semester {selectedHistory?.semester || student?.semester || '—'}
                </p>
                <div className="flex flex-wrap items-center justify-center sm:justify-start gap-4 pt-2 text-xs text-gray-600">
                  {student?.email && (
                    <span className="flex items-center gap-1.5">
                      <Mail size={14} className="text-blue-600" />
                      {student.email}
                    </span>
                  )}
                  {student?.phoneNumber && (
                    <span className="flex items-center gap-1.5">
                      <Phone size={14} className="text-blue-600" />
                      {student.phoneNumber}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Navigation Tabs */}
            {years.length>0&&<label className="block max-w-xs text-sm font-medium text-gray-700">Academic Year<select className="input-field mt-1" value={selectedYearId} onChange={e=>setSelectedYearId(e.target.value)}>{years.map(y=><option key={y.academicYearId} value={y.academicYearId}>{y.academicYear}{y.active?' (Active)':''}</option>)}</select></label>}
            <div className="flex border-b border-gray-200 text-xs sm:text-sm font-medium overflow-x-auto">
              <button
                type="button" onClick={()=>setActiveTab('applications')} className={`px-3 py-2 border-b-2 whitespace-nowrap ${activeTab==='applications'?'border-blue-600 text-blue-600 font-semibold':'border-transparent text-gray-500'}`}>Applications ({applications.length})</button>
              <button
                type="button"
                onClick={() => setActiveTab('overview')}
                className={`px-3 py-2 border-b-2 whitespace-nowrap transition-colors ${activeTab === 'overview' ? 'border-blue-600 text-blue-600 font-semibold' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
              >
                Personal Details
              </button>
              <button
                type="button" onClick={()=>setActiveTab('payments')} className={`px-3 py-2 border-b-2 whitespace-nowrap ${activeTab==='payments'?'border-blue-600 text-blue-600 font-semibold':'border-transparent text-gray-500'}`}>Payments ({payments.length})</button>
              <button
                type="button"
                onClick={() => setActiveTab('transport')}
                className={`px-3 py-2 border-b-2 whitespace-nowrap transition-colors flex items-center gap-1.5 ${activeTab === 'transport' ? 'border-blue-600 text-blue-600 font-semibold' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
              >
                <Bus size={14} /> Bus Assignments ({assignments.length})
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('fees')}
                className={`px-3 py-2 border-b-2 whitespace-nowrap transition-colors flex items-center gap-1.5 ${activeTab === 'fees' ? 'border-blue-600 text-blue-600 font-semibold' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
              >
                <CreditCard size={14} /> Fees ({fees.length})
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('passes')}
                className={`px-3 py-2 border-b-2 whitespace-nowrap transition-colors flex items-center gap-1.5 ${activeTab === 'passes' ? 'border-blue-600 text-blue-600 font-semibold' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
              >
                <Ticket size={14} /> Passes ({passes.length})
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('transfers')}
                className={`px-3 py-2 border-b-2 whitespace-nowrap transition-colors flex items-center gap-1.5 ${activeTab === 'transfers' ? 'border-blue-600 text-blue-600 font-semibold' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
              >
                <ArrowLeftRight size={14} /> Transfers ({transfers.length})
              </button>
            </div>

            {/* Tab 1: Personal Details */}
            {activeTab === 'overview' && (
              <div className="rounded-xl border border-gray-200 bg-white p-4 space-y-3">
                <h4 className="text-xs font-bold uppercase tracking-wider text-gray-500 flex items-center gap-1.5">
                  <BookOpen size={15} className="text-blue-600" /> Student Profile & Contact
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
                      <Heart size={13} className="text-red-500" /> {student?.bloodGroup || '—'}
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
                    <span className="block text-xs text-gray-500">Department</span>
                    <span className="font-medium text-gray-800">{selectedHistory?.branch || student?.branch || '—'}</span>
                  </div>
                  <div className="col-span-2 sm:col-span-3">
                    <span className="block text-xs text-gray-500 flex items-center gap-1">
                      <MapPin size={13} className="text-blue-600" /> Residential Address
                    </span>
                    <span className="font-medium text-gray-800 whitespace-pre-wrap">{student?.address || '—'}</span>
                  </div>
                </div>
              </div>
            )}

            {/* Tab 2: Bus Assignments */}
            {activeTab === 'transport' && (
              <div className="space-y-3">
                {assignments.length === 0 ? (
                  <div className="text-center py-8 text-gray-500 bg-gray-50 rounded-xl border">
                    <Bus size={32} className="mx-auto text-gray-400 mb-2" />
                    <p className="text-sm font-medium">No bus allocations on record for this student.</p>
                  </div>
                ) : (
                  <div className="overflow-x-auto rounded-lg border border-gray-200">
                    <table className="min-w-full divide-y divide-gray-200 text-sm">
                      <thead className="bg-gray-50 text-xs font-semibold text-gray-600 uppercase">
                        <tr>
                          <th className="px-4 py-2 text-left">Academic Year</th>
                          <th className="px-4 py-2 text-left">Bus Number</th>
                          <th className="px-4 py-2 text-left">Boarding Point</th>
                          <th className="px-4 py-2 text-left">Status</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-100 bg-white">
                        {assignments.map((a, i) => (
                          <tr key={a.id || i}>
                            <td className="px-4 py-2.5 font-medium text-gray-700">{a.academicYear || '—'}</td>
                            <td className="px-4 py-2.5 font-bold text-blue-900">{a.busNumber || a.bus || '—'}</td>
                            <td className="px-4 py-2.5 text-gray-800">{a.boardingPoint || '—'}</td>
                            <td className="px-4 py-2.5">
                              <StatusBadge status={a.status} />
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}

            {activeTab === 'applications' && (
              <HistoryCards rows={applications} empty="No applications for this academic year" render={(a) => <><b>{a.busNumber || '—'} · {a.boardingPointName || '—'}</b><span>{a.startingPoint || '—'} → {a.endingPoint || '—'} · {a.status || '—'}</span></>} />
            )}
            {activeTab === 'payments' && (
              <HistoryCards rows={payments} empty="No payment transactions for this academic year" render={(p) => <><b>{formatINR(p.amount)} · {p.orderId || 'No receipt ID'}</b><span>{p.status || '—'} · {formatDate(p.paidAt || p.createdAt)}</span></>} />
            )}

            {/* Tab 3: Fees */}
            {activeTab === 'fees' && (
              <div className="space-y-3">
                {fees.length === 0 ? (
                  <div className="text-center py-8 text-gray-500 bg-gray-50 rounded-xl border">
                    <CreditCard size={32} className="mx-auto text-gray-400 mb-2" />
                    <p className="text-sm font-medium">No fee records found for this student.</p>
                  </div>
                ) : (
                  fees.map((fee, idx) => (
                    <div key={fee.id || idx} className="p-4 rounded-xl border border-gray-200 bg-gray-50 space-y-2">
                      <div className="flex justify-between items-center">
                        <span className="font-semibold text-blue-900 text-sm">Academic Year: {fee.academicYear || '—'}</span>
                        <StatusBadge status={fee.status} />
                      </div>
                      <div className="grid grid-cols-3 gap-2 text-xs pt-1">
                        <div>
                          <span className="text-gray-500 block">Total Fee</span>
                          <span className="font-semibold text-gray-900">{formatINR(fee.totalAmount)}</span>
                        </div>
                        <div>
                          <span className="text-gray-500 block">Paid Amount</span>
                          <span className="font-semibold text-green-700">{formatINR(fee.paidAmount)}</span>
                        </div>
                        <div>
                          <span className="text-gray-500 block">Remaining</span>
                          <span className="font-semibold text-red-600">{formatINR(fee.remainingAmount)}</span>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}

            {/* Tab 4: Passes */}
            {activeTab === 'passes' && (
              <div className="space-y-3">
                {passes.length === 0 ? (
                  <div className="text-center py-8 text-gray-500 bg-gray-50 rounded-xl border">
                    <Ticket size={32} className="mx-auto text-gray-400 mb-2" />
                    <p className="text-sm font-medium">No bus passes issued for this student yet.</p>
                  </div>
                ) : (
                  passes.map((p, idx) => (
                    <div key={p.id || idx} className="p-4 rounded-xl border border-gray-200 bg-gray-50 space-y-2">
                      <div className="flex justify-between items-center">
                        <span className="font-bold text-blue-900 text-sm">Pass #{p.passNumber}</span>
                        <StatusBadge status={p.status} />
                      </div>
                      <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 text-xs pt-1">
                        <div>
                          <span className="text-gray-500 block">Bus</span>
                          <span className="font-medium text-gray-800">{p.busNumber || '—'}</span>
                        </div>
                        <div>
                          <span className="text-gray-500 block">Boarding Point</span>
                          <span className="font-medium text-gray-800">{p.boardingPoint || '—'}</span>
                        </div>
                        <div>
                          <span className="text-gray-500 block">Valid Until</span>
                          <span className="font-medium text-gray-800">{formatDate(p.validUntil)}</span>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}

            {/* Tab 5: Transfers */}
            {activeTab === 'transfers' && (
              <div className="space-y-3">
                {transfers.length === 0 ? (
                  <div className="text-center py-8 text-gray-500 bg-gray-50 rounded-xl border">
                    <ArrowLeftRight size={32} className="mx-auto text-gray-400 mb-2" />
                    <p className="text-sm font-medium">No bus transfer requests submitted by this student.</p>
                  </div>
                ) : (
                  transfers.map((t, idx) => (
                    <div key={t.id || idx} className="p-4 rounded-xl border border-gray-200 bg-gray-50 space-y-2">
                      <div className="flex justify-between items-center">
                        <span className="font-bold text-blue-900 text-sm">{t.currentBusNumber} → {t.requestedBusNumber}</span>
                        <StatusBadge status={t.status} />
                      </div>
                      <p className="text-xs text-gray-600">Reason: {t.reason || '—'}</p>
                      {t.remarks && (
                        <p className="text-xs text-amber-800 bg-amber-50 p-2 rounded border border-amber-200">
                          Remarks: {t.remarks}
                        </p>
                      )}
                    </div>
                  ))
                )}
              </div>
            )}
          </>
        )}

        <div className="flex justify-end pt-3 border-t border-gray-100">
          <button type="button" className="btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </Modal>
  );
}

const HistoryCards=({rows,empty,render})=><div className="space-y-2">{rows.length?rows.map((row,index)=><div key={row.id||index} className="flex flex-col rounded-lg border bg-gray-50 p-3 text-sm">{render(row)}</div>):<p className="rounded-lg bg-gray-50 p-6 text-center text-sm text-gray-500">{empty}</p>}</div>;
