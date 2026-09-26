import React, { useEffect, useState } from 'react';
import { Eye, Edit2, Trash2 } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import Modal from '../../components/common/Modal';
import { useToast } from '../../components/common/Toast';

const detail = (label, value) => (
  <div>
    <p className="text-xs font-medium uppercase tracking-wide text-gray-500">{label}</p>
    <p className="mt-1 break-words text-sm text-gray-900">{value || '—'}</p>
  </div>
);

export default function AdminApplicationsPage() {
  const [rows, setRows] = useState([]);
  const [buses, setBuses] = useState([]);
  const [points, setPoints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pages, setPages] = useState(0);
  const [status, setStatus] = useState('');
  const [dialog, setDialog] = useState(false);
  const [current, setCurrent] = useState(null);
  const [action, setAction] = useState('');
  const [remarks, setRemarks] = useState('');
  const [viewing, setViewing] = useState(null);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ busId: '', boardingPointId: '', remarks: '' });
  const { addToast } = useToast();

  const load = async () => {
    setLoading(true);
    try {
      const query = `page=${page}&size=10${status ? `&status=${status}` : ''}`;
      const [applications, busResponse] = await Promise.all([
        api.get(`/admin/applications?${query}`),
        api.get('/admin/buses'),
      ]);
      setRows(applications.data.content || []);
      setPages(applications.data.totalPages || 0);
      setBuses(busResponse.data || []);
    } catch (error) {
      addToast(error.response?.data?.message || 'Failed to load applications', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [page, status]);

  const openDecision = (row, nextAction) => {
    setCurrent(row);
    setAction(nextAction);
    setRemarks('');
    setDialog(true);
  };

  const decide = async () => {
    try {
      if (action === 'APPROVE') await api.put(`/admin/applications/${current.id}/approve`);
      else await api.put(`/admin/applications/${current.id}/reject`, remarks.trim());
      addToast(action === 'APPROVE' ? 'Application approved' : 'Application rejected', 'success');
      setDialog(false);
      setCurrent(null);
      load();
    } catch (error) {
      addToast(error.response?.data?.message || 'Action failed', 'error');
    }
  };

  const openEdit = async (row) => {
    try {
      const response = await api.get(`/admin/buses/${row.busId}/boarding-points`);
      setPoints((response.data || []).filter((point) => point.status === 'ACTIVE'));
      setEditing(row);
      setForm({ busId: String(row.busId), boardingPointId: String(row.boardingPointId), remarks: row.remarks || '' });
    } catch (error) {
      addToast(error.response?.data?.message || 'Unable to load boarding points', 'error');
    }
  };

  const changeBus = async (value) => {
    setForm((previous) => ({ ...previous, busId: value, boardingPointId: '' }));
    if (!value) { setPoints([]); return; }
    try {
      const response = await api.get(`/admin/buses/${value}/boarding-points`);
      setPoints((response.data || []).filter((point) => point.status === 'ACTIVE'));
    } catch (error) {
      addToast(error.response?.data?.message || 'Unable to load boarding points', 'error');
    }
  };

  const saveEdit = async (event) => {
    event.preventDefault();
    try {
      await api.put(`/admin/applications/${editing.id}`, {
        busId: Number(form.busId), boardingPointId: Number(form.boardingPointId), remarks: form.remarks,
      });
      addToast('Application updated', 'success');
      setEditing(null);
      load();
    } catch (error) {
      addToast(error.response?.data?.message || 'Application update failed', 'error');
    }
  };

  const remove = async (row) => {
    if (!window.confirm(`Delete the application for ${row.rollNumber} and ${row.busNumber}?`)) return;
    try {
      await api.delete(`/admin/applications/${row.id}`);
      addToast('Application deleted', 'success');
      load();
    } catch (error) {
      addToast(error.response?.data?.message || 'Application deletion failed', 'error');
    }
  };

  const editable = (row) => ['PENDING', 'UNDER_REVIEW'].includes(row.status);
  const deletable = (row) => row.status !== 'APPROVED';
  const formatDateTime = (value) => value ? new Date(value).toLocaleString() : '—';
  const columns = [
    { key: 'appliedAt', label: 'Date', render: (row) => new Date(row.appliedAt).toLocaleDateString() },
    { key: 'student', label: 'Student', render: (row) => <div><div className="font-medium">{row.studentName || '—'}</div><div className="text-xs text-gray-500">{row.rollNumber || '—'}</div></div> },
    { key: 'busNumber', label: 'Bus', render: (row) => <span className="font-bold text-blue-900">{row.busNumber}</span> },
    { key: 'boardingPointName', label: 'Boarding Point' },
    { key: 'academicYear', label: 'Academic Year' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    { key: 'actions', label: 'Actions', render: (row) => <div className="flex min-w-[220px] flex-wrap items-center gap-2">
      <button type="button" onClick={() => setViewing(row)} className="inline-flex items-center gap-1 rounded bg-blue-100 px-2 py-1 text-xs font-medium text-blue-800"><Eye size={14} /> View</button>
      {editable(row) && <>
        <button type="button" onClick={() => openDecision(row, 'APPROVE')} className="rounded bg-green-100 px-2 py-1 text-xs font-medium text-green-700">Approve</button>
        <button type="button" onClick={() => openDecision(row, 'REJECT')} className="rounded bg-red-100 px-2 py-1 text-xs font-medium text-red-700">Reject</button>
        <button type="button" onClick={() => openEdit(row)} className="inline-flex items-center gap-1 rounded bg-gray-100 px-2 py-1 text-xs font-medium text-gray-700"><Edit2 size={14} /> Edit</button>
      </>}
      {deletable(row) && <button type="button" onClick={() => remove(row)} className="inline-flex items-center gap-1 rounded bg-red-50 px-2 py-1 text-xs font-medium text-red-700"><Trash2 size={14} /> Delete</button>}
      {row.status === 'APPROVED' && <span title="Linked to active transport records" className="rounded bg-gray-100 px-2 py-1 text-xs text-gray-500">Allocated · Locked</span>}
    </div> },
  ];

  return <>
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><div><h2 className="text-2xl font-bold text-gray-800">Transportation Applications</h2><p className="text-sm text-gray-500">Review, correct and manage student bus applications</p></div><select className="input-field w-auto" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }}><option value="">All Applications</option><option>PENDING</option><option>UNDER_REVIEW</option><option>APPROVED</option><option>REJECTED</option><option>CANCELLED</option></select></div>
      <div className="card"><DataTable columns={columns} data={rows} loading={loading} /><p className="mt-3 text-xs text-gray-500">Approved applications are linked to allocations, fees and passes. They can be viewed here but must be changed through the relevant transport workflow.</p><Pagination page={page} totalPages={pages} onPageChange={setPage} /></div>
    </div>

    <ConfirmDialog isOpen={dialog} title={`${action === 'APPROVE' ? 'Approve' : 'Reject'} Application`} message={<div className="space-y-3"><p>Confirm {action.toLowerCase()} for {current?.rollNumber || 'this student'}?</p>{action === 'REJECT' && <textarea autoFocus maxLength="500" className="input-field" placeholder="Enter rejection reason" value={remarks} onChange={(event) => setRemarks(event.target.value)} />}</div>} onConfirm={() => action !== 'REJECT' || remarks.trim() ? decide() : addToast('A rejection reason is required', 'error')} onCancel={() => setDialog(false)} />

    <Modal isOpen={Boolean(viewing)} onClose={() => setViewing(null)} title="Application Details" size="lg">
      {viewing && <div className="space-y-5"><div className="grid grid-cols-1 gap-4 sm:grid-cols-2">{detail('Student', viewing.studentName)}{detail('Roll Number', viewing.rollNumber)}{detail('Bus', viewing.busNumber)}{detail('Route', `${viewing.startingPoint || '—'} → ${viewing.endingPoint || '—'}`)}{detail('Boarding Point', viewing.boardingPointName)}{detail('Academic Year', viewing.academicYear)}{detail('Applied At', formatDateTime(viewing.appliedAt))}{detail('Reviewed At', formatDateTime(viewing.reviewedAt))}{detail('Remarks', viewing.remarks)}<div><p className="text-xs font-medium uppercase tracking-wide text-gray-500">Status</p><div className="mt-1"><StatusBadge status={viewing.status} /></div></div></div><div className="flex flex-wrap justify-end gap-2">{editable(viewing) && <button type="button" className="btn-secondary" onClick={() => { setViewing(null); openEdit(viewing); }}>Edit</button>}<button type="button" className="btn-primary" onClick={() => setViewing(null)}>Close</button></div></div>}
    </Modal>

    <Modal isOpen={Boolean(editing)} onClose={() => setEditing(null)} title="Edit Bus Application">
      <form onSubmit={saveEdit} className="space-y-4"><label className="block text-sm font-medium">Bus *<select required className="input-field mt-1" value={form.busId} onChange={(event) => changeBus(event.target.value)}><option value="">Select bus</option>{buses.filter((bus) => bus.status === 'ACTIVE').map((bus) => <option key={bus.id} value={bus.id}>{bus.busNumber} — {bus.startingPoint} to {bus.endingPoint}</option>)}</select></label><label className="block text-sm font-medium">Boarding Point *<select required className="input-field mt-1" value={form.boardingPointId} onChange={(event) => setForm({ ...form, boardingPointId: event.target.value })}><option value="">Select point</option>{points.map((point) => <option key={point.id} value={point.id}>{point.stationName} — ₹{point.feeAmount}</option>)}</select></label><label className="block text-sm font-medium">Remarks<textarea maxLength="500" className="input-field mt-1" value={form.remarks} onChange={(event) => setForm({ ...form, remarks: event.target.value })} /></label><div className="flex justify-end gap-3"><button type="button" className="btn-secondary" onClick={() => setEditing(null)}>Cancel</button><button className="btn-primary">Save Changes</button></div></form>
    </Modal>
  </>;
}
