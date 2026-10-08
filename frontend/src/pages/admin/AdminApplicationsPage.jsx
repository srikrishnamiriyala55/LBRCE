import React, { useEffect, useState, useRef, useCallback } from 'react';
import { Eye, Edit2, Trash2 } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import Modal from '../../components/common/Modal';
import StudentApplicationDetailModal from '../../components/applications/StudentApplicationDetailModal';
import { useToast } from '../../components/common/Toast';

const detail = (label, value) => (
  <div>
    <p className="text-xs font-medium uppercase tracking-wide text-gray-500">{label}</p>
    <p className="mt-1 break-words text-sm text-gray-900">{value || '—'}</p>
  </div>
);

const STATUS_OPTIONS = [
  { value: '', label: 'All Applications' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'UNDER_REVIEW', label: 'Under Review' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

export default function AdminApplicationsPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const [rows, setRows] = useState([]);
  const [buses, setBuses] = useState([]);
  const [points, setPoints] = useState([]);
  const [loading, setLoading] = useState(true);
  // Initialize status from URL search params (e.g. ?status=PENDING from dashboard click)
  const [status, setStatus] = useState(() => searchParams.get('status') || '');
  const [search, setSearch] = useState('');
  const [dialog, setDialog] = useState(false);
  const [current, setCurrent] = useState(null);
  const [action, setAction] = useState('');
  const [remarks, setRemarks] = useState('');
  const [viewing, setViewing] = useState(null);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ busId: '', boardingPointId: '', remarks: '' });
  const [summary, setSummary] = useState({});
  const [academicYears, setAcademicYears] = useState([]);
  const [academicYearId, setAcademicYearId] = useState('');
  const [yearsLoaded, setYearsLoaded] = useState(false);
  const { addToast } = useToast();

  const [debouncedSearch, setDebouncedSearch] = useState('');
  const debounceTimer = useRef(null);
  const requestVersion = useRef(0);
  const handleSearchChange = (e) => {
    const v = e.target.value;
    setSearch(v);
    clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => setDebouncedSearch(v), 300);
  };

  useEffect(() => {
    let active = true;
    api.get('/admin/academic-years').then((response) => {
      if (!active) return;
      const years = response.data || [];
      setAcademicYears(years);
      const activeYear = years.find((year) => year.active);
      setAcademicYearId(String(activeYear?.id || (years[0]?.id ?? '')));
      setYearsLoaded(true);
    }).catch(() => {
      if (!active) return;
      addToast('Failed to load academic years', 'error');
      setYearsLoaded(true);
    });
    return () => { active = false; };
  }, [addToast]);

  // Keep URL in sync with status filter
  const handleStatusChange = (newStatus) => {
    setStatus(newStatus);
    if (newStatus) {
      setSearchParams({ status: newStatus });
    } else {
      setSearchParams({});
    }
  };

  const load = useCallback(async (signal) => {
    const version = ++requestVersion.current;
    setLoading(true);
    try {
      const [applications, busResult, summaryResult] = await Promise.allSettled([
        api.get('/admin/applications', {
          signal,
          params: { search: debouncedSearch.trim() || undefined, status: status || undefined, academicYearId: academicYearId || undefined },
        }),
        api.get('/admin/buses', { signal }),
        api.get('/admin/applications/summary', { signal, params: { academicYearId: academicYearId || undefined } }),
      ]);
      if (signal?.aborted || version !== requestVersion.current) return;
      if (applications.status === 'rejected') {
        const error = applications.reason;
        if (error?.name === 'CanceledError' || error?.code === 'ERR_CANCELED') return;
        addToast(error?.response?.data?.message || 'Failed to load applications', 'error');
      } else {
        const data = applications.value.data;
        setRows(Array.isArray(data) ? data : (data?.content || []));
      }
      if (busResult.status === 'fulfilled') setBuses(busResult.value.data || []);
      if (summaryResult.status === 'fulfilled') setSummary(summaryResult.value.data || {});
      setLoading(false);
    } catch (error) {
      if (signal?.aborted || error?.name === 'CanceledError' || error?.code === 'ERR_CANCELED') {
        return;
      }
      addToast(error?.response?.data?.message || 'Failed to load applications', 'error');
      if (version === requestVersion.current) setLoading(false);
    }
  }, [status, debouncedSearch, academicYearId, addToast]);

  useEffect(() => {
    if (!yearsLoaded) return;
    const c = new AbortController();
    load(c.signal);
    return () => c.abort();
  }, [yearsLoaded, load]);

  const openDecision = (row, nextAction) => {
    setCurrent(row);
    setAction(nextAction);
    setRemarks('');
    setDialog(true);
  };

  const decide = async () => {
    try {
      if (action === 'APPROVE') await api.put(`/admin/applications/${current.id}/approve`);
      else await api.put(`/admin/applications/${current.id}/reject`, { remarks: remarks.trim() });
      addToast(action === 'APPROVE' ? 'Application approved' : 'Application rejected', 'success');
      setDialog(false);
      setCurrent(null);
      load(new AbortController().signal);
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
      load(new AbortController().signal);
    } catch (error) {
      addToast(error.response?.data?.message || 'Application update failed', 'error');
    }
  };

  const remove = async (row) => {
    if (!window.confirm(`Delete the application for ${row.rollNumber} and ${row.busNumber}?`)) return;
    try {
      await api.delete(`/admin/applications/${row.id}`);
      addToast('Application deleted', 'success');
      load(new AbortController().signal);
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
    {
      key: 'actions', label: 'Actions', render: (row) => (
        <div className="flex min-w-[220px] flex-wrap items-center gap-2">
          <button type="button" onClick={() => setViewing(row)} className="inline-flex items-center gap-1 rounded bg-blue-100 px-2 py-1 text-xs font-medium text-blue-800"><Eye size={14} /> View</button>
          {editable(row) && <>
            <button type="button" onClick={() => openDecision(row, 'APPROVE')} className="rounded bg-green-100 px-2 py-1 text-xs font-medium text-green-700">Approve</button>
            <button type="button" onClick={() => openDecision(row, 'REJECT')} className="rounded bg-red-100 px-2 py-1 text-xs font-medium text-red-700">Reject</button>
            <button type="button" onClick={() => openEdit(row)} className="inline-flex items-center gap-1 rounded bg-gray-100 px-2 py-1 text-xs font-medium text-gray-700"><Edit2 size={14} /> Edit</button>
          </>}
          {deletable(row) && <button type="button" onClick={() => remove(row)} className="inline-flex items-center gap-1 rounded bg-red-50 px-2 py-1 text-xs font-medium text-red-700"><Trash2 size={14} /> Delete</button>}
          {row.status === 'APPROVED' && <span title="Linked to active transport records" className="rounded bg-gray-100 px-2 py-1 text-xs text-gray-500">Allocated · Locked</span>}
        </div>
      )
    },
  ];

  // Summary stat chips – clicking sets the status filter
  const summaryChips = [
    { label: 'Registered', value: summary.registered, statusFilter: '' },
    { label: 'Pending', value: summary.pending, statusFilter: 'PENDING' },
    { label: 'Under review', value: summary.under_review, statusFilter: 'UNDER_REVIEW' },
    { label: 'Approved', value: summary.approved, statusFilter: 'APPROVED' },
    { label: 'Rejected', value: summary.rejected, statusFilter: 'REJECTED' },
    { label: 'Cancelled', value: summary.cancelled, statusFilter: 'CANCELLED' },
  ];

  return <>
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Transportation Applications</h2>
          <p className="text-sm text-gray-500">Review, correct and manage student bus applications</p>
        </div>
        <select
          className="input-field w-auto"
          value={status}
          onChange={(event) => handleStatusChange(event.target.value)}
        >
          {STATUS_OPTIONS.map(opt => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
        <select className="input-field w-auto" value={academicYearId} onChange={(event) => setAcademicYearId(event.target.value)}>
          <option value="">All academic years</option>
          {academicYears.map((year) => <option key={year.id} value={year.id}>{year.yearName}{year.active ? ' (Active)' : ''}</option>)}
        </select>
      </div>

      {/* Clickable summary chips */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        {summaryChips.map(({ label, value, statusFilter }) => (
          <div
            key={label}
            onClick={() => handleStatusChange(statusFilter)}
            className={`rounded-lg border bg-white p-3 cursor-pointer transition-all hover:shadow-md hover:ring-2 hover:ring-blue-300 ${status === statusFilter ? 'ring-2 ring-blue-500 bg-blue-50' : ''}`}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleStatusChange(statusFilter); }}
            title={`Filter by ${label}`}
          >
            <p className="text-xs text-gray-500">{label}</p>
            <p className="text-xl font-bold text-blue-900">{value || 0}</p>
          </div>
        ))}
      </div>

      <div className="card">
        <div className="mb-4">
          <input
            className="input-field"
            value={search}
            onChange={handleSearchChange}
            placeholder="Search student, roll number, bus, boarding point, year or status"
          />
        </div>
        <DataTable columns={columns} data={rows} loading={loading} searchable={false} />
        <p className="mt-3 text-xs text-gray-500">
          {rows.length ? `${rows.length} matching record(s) displayed.` : 'No matching applications.'} Approved applications are linked to allocations, fees and passes.
        </p>
      </div>
    </div>

    <ConfirmDialog
      isOpen={dialog}
      title={`${action === 'APPROVE' ? 'Approve' : 'Reject'} Application`}
      message={<div className="space-y-3"><p>Confirm {action.toLowerCase()} for {current?.rollNumber || 'this student'}?</p>{action === 'REJECT' && <textarea autoFocus maxLength="500" className="input-field" placeholder="Enter rejection reason" value={remarks} onChange={(event) => setRemarks(event.target.value)} />}</div>}
      onConfirm={() => action !== 'REJECT' || remarks.trim() ? decide() : addToast('A rejection reason is required', 'error')}
      onCancel={() => setDialog(false)}
    />

    <StudentApplicationDetailModal
      isOpen={Boolean(viewing)}
      onClose={() => setViewing(null)}
      application={viewing}
      role="admin"
      onApprove={(app) => openDecision(app, 'APPROVE')}
      onReject={(app) => openDecision(app, 'REJECT')}
      onEdit={(app) => { setViewing(null); openEdit(app); }}
    />

    <Modal isOpen={Boolean(editing)} onClose={() => setEditing(null)} title="Edit Bus Application">
      <form onSubmit={saveEdit} className="space-y-4"><label className="block text-sm font-medium">Bus *<select required className="input-field mt-1" value={form.busId} onChange={(event) => changeBus(event.target.value)}><option value="">Select bus</option>{buses.filter((bus) => bus.status === 'ACTIVE').map((bus) => <option key={bus.id} value={bus.id}>{bus.busNumber} — {bus.startingPoint} to {bus.endingPoint}</option>)}</select></label><label className="block text-sm font-medium">Boarding Point *<select required className="input-field mt-1" value={form.boardingPointId} onChange={(event) => setForm({ ...form, boardingPointId: event.target.value })}><option value="">Select point</option>{points.map((point) => <option key={point.id} value={point.id}>{point.stationName} — ₹{point.feeAmount}</option>)}</select></label><label className="block text-sm font-medium">Remarks<textarea maxLength="500" className="input-field mt-1" value={form.remarks} onChange={(event) => setForm({ ...form, remarks: event.target.value })} /></label><div className="flex justify-end gap-3"><button type="button" className="btn-secondary" onClick={() => setEditing(null)}>Cancel</button><button className="btn-primary">Save Changes</button></div></form>
    </Modal>
  </>;
}
