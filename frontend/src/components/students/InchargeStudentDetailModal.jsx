import React, { useEffect, useMemo, useState } from 'react';
import Modal from '../common/Modal';
import StatusBadge from '../common/StatusBadge';
import LoadingSpinner from '../common/LoadingSpinner';
import api from '../../utils/axios';
import { formatINR } from '../../utils/currency';
import { User, Mail, Phone, MapPin, Heart, BookOpen } from 'lucide-react';

const formatDate = (value) => value ? new Date(value).toLocaleDateString() : '—';
const Empty = ({ children }) => <div className="rounded-lg bg-gray-50 p-6 text-center text-sm text-gray-500">{children}</div>;
const Cards = ({ rows, empty, children }) => rows?.length
  ? <div className="space-y-2">{rows.map((row, index) => <div key={row.id || index} className="rounded-lg border bg-gray-50 p-3 text-sm">{children(row)}</div>)}</div>
  : <Empty>{empty}</Empty>;

export default function InchargeStudentDetailModal({ isOpen, onClose, student }) {
  const [photoUrl, setPhotoUrl] = useState('');
  const [history, setHistory] = useState([]);
  const [selectedYearId, setSelectedYearId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    let objectUrl = '';
    if (!isOpen || !student?.studentId) return undefined;
    setLoading(true); setError(''); setHistory([]); setPhotoUrl('');
    api.get(`/incharge/students/${student.studentId}/history`)
      .then(({ data }) => {
        if (!active) return;
        const years = Array.isArray(data) ? data : [];
        setHistory(years);
        const preferred = years.find((item) => item.academicYear === student.academicYear)
          || years.find((item) => item.active) || years[0];
        setSelectedYearId(String(preferred?.academicYearId || ''));
      })
      .catch((err) => active && setError(err.response?.data?.message || 'Unable to load student history'))
      .finally(() => active && setLoading(false));
    api.get(`/incharge/students/${student.studentId}/photo`, { responseType: 'blob' })
      .then((res) => {
        if (active && res.data) { objectUrl = URL.createObjectURL(res.data); setPhotoUrl(objectUrl); }
      }).catch(() => {});
    return () => { active = false; if (objectUrl) URL.revokeObjectURL(objectUrl); };
  }, [isOpen, student?.studentId, student?.academicYear]);

  const selected = useMemo(() => history.find((item) => String(item.academicYearId) === selectedYearId), [history, selectedYearId]);
  if (!isOpen || !student) return null;

  return <Modal isOpen={isOpen} onClose={onClose} title="Complete Student History" size="xl">
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4 p-4 rounded-xl bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-100">
        <div className="w-24 h-28 rounded-lg overflow-hidden border-2 border-white shadow-md bg-gray-100 shrink-0 flex items-center justify-center">
          {photoUrl ? <img src={photoUrl} alt={`${student.name || 'Student'} photo`} className="w-full h-full object-cover" /> : <User size={34} className="text-gray-300" />}
        </div>
        <div className="flex-1 text-center sm:text-left space-y-1">
          <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2"><h3 className="text-xl font-bold">{student.name || '—'}</h3><span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-700 text-white">{student.rollNumber || '—'}</span></div>
          <p className="text-sm font-medium text-blue-900">{selected?.branch || student.branch || 'Department'} · Year {selected?.year || student.year || '—'} · Semester {selected?.semester || student.semester || '—'}</p>
          <div className="flex flex-wrap justify-center sm:justify-start gap-4 pt-2 text-xs text-gray-600">{student.email && <span className="flex items-center gap-1"><Mail size={14}/>{student.email}</span>}{student.phoneNumber && <span className="flex items-center gap-1"><Phone size={14}/>{student.phoneNumber}</span>}</div>
        </div>
      </div>

      {loading ? <div className="py-12 flex justify-center"><LoadingSpinner size="lg" /></div> : error ? <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div> : history.length === 0 ? <Empty>No historical records are available for buses managed by you.</Empty> : <>
        <label className="block max-w-sm text-sm font-medium text-gray-700">Academic Year<select className="input-field mt-1" value={selectedYearId} onChange={(e) => setSelectedYearId(e.target.value)}>{history.map((item) => <option key={item.academicYearId} value={item.academicYearId}>{item.academicYear}{item.active ? ' (Active)' : ''}</option>)}</select></label>
        <section className="rounded-xl border bg-white p-4"><h4 className="mb-3 text-xs font-bold uppercase tracking-wider text-gray-500 flex items-center gap-1"><BookOpen size={15}/> Personal & Academic Details</h4><div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
          <Info label="Academic status" value={selected?.academicStatus} badge/><Info label="Date of birth" value={formatDate(student.dob)}/><Info label="Gender" value={student.gender}/><Info label="Blood group" value={student.bloodGroup} icon={<Heart size={13} className="text-red-500"/>}/><Info label="Parent / guardian" value={student.parentName}/><Info label="Parent contact" value={student.parentPhoneNumber}/><Info label="Address" value={student.address} icon={<MapPin size={13}/>} wide/>
        </div></section>
        <HistorySection title={`Applications (${selected?.applications?.length || 0})`}><Cards rows={selected?.applications} empty="No applications in this academic year">{(row) => <Row status={row.status} title={`${row.busNumber || '—'} · ${row.boardingPointName || '—'}`} detail={`${row.startingPoint || '—'} → ${row.endingPoint || '—'}`}/>}</Cards></HistorySection>
        <HistorySection title={`Bus allocations (${selected?.allocations?.length || 0})`}><Cards rows={selected?.allocations} empty="No bus allocation in this academic year">{(row) => <Row status={row.status} title={`${row.busNumber || '—'} · ${row.boardingPoint || '—'}`} detail={`${row.startingPoint || '—'} → ${row.endingPoint || '—'} · In-charge: ${row.inchargeName || '—'}`}/>}</Cards></HistorySection>
        <HistorySection title={`Fees & payments`}><Cards rows={selected?.fees} empty="No fee record in this academic year">{(row) => <Row status={row.status} title={`Total ${formatINR(row.totalAmount)}`} detail={`Paid ${formatINR(row.paidAmount)} · Pending ${formatINR(row.remainingAmount)}`}/>}</Cards><div className="mt-2"><Cards rows={selected?.payments} empty="No payment transactions in this academic year">{(row) => <Row status={row.status} title={`${formatINR(row.amount)} · ${row.orderId || 'No receipt ID'}`} detail={formatDate(row.paidAt || row.createdAt)}/>}</Cards></div></HistorySection>
        <HistorySection title={`Bus passes (${selected?.passes?.length || 0})`}><Cards rows={selected?.passes} empty="No bus pass in this academic year">{(row) => <Row status={row.status} title={`${row.passNumber || 'Pass'} · ${row.busNumber || '—'}`} detail={`${row.boardingPoint || '—'} · Valid until ${formatDate(row.validUntil)}`}/>}</Cards></HistorySection>
        <HistorySection title={`Transfers (${selected?.transfers?.length || 0})`}><Cards rows={selected?.transfers} empty="No transfer requests in this academic year">{(row) => <Row status={row.status} title={`${row.currentBusNumber || '—'} → ${row.requestedBusNumber || '—'}`} detail={row.reason || 'No reason recorded'}/>}</Cards></HistorySection>
      </>}
      <div className="flex justify-end pt-2 border-t"><button type="button" className="btn-secondary" onClick={onClose}>Close</button></div>
    </div>
  </Modal>;
}

const Info = ({ label, value, icon, badge, wide }) => <div className={wide ? 'col-span-2 md:col-span-4' : ''}><span className="block text-xs text-gray-500">{label}</span><span className="font-medium text-gray-800 flex items-center gap-1">{icon}{badge && value ? <StatusBadge status={value}/> : value || '—'}</span></div>;
const HistorySection = ({ title, children }) => <section className="rounded-xl border bg-white p-4"><h4 className="mb-3 text-sm font-bold text-gray-800">{title}</h4>{children}</section>;
const Row = ({ status, title, detail }) => <div className="flex justify-between gap-3"><span><b>{title}</b><small className="block text-gray-500">{detail}</small></span>{status && <StatusBadge status={status}/>}</div>;
