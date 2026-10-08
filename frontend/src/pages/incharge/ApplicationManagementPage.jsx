import React, { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Eye } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StudentApplicationDetailModal from '../../components/applications/StudentApplicationDetailModal';
import { useToast } from '../../components/common/Toast';

const buildInitialFilters = (urlStatus) => ({
  search: '',
  status: urlStatus || 'PENDING',
  boardingPoint: '',
  academicYearId: '',
});

export default function ApplicationManagementPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState(() => buildInitialFilters(searchParams.get('status')));
  const [dialogOpen, setDialogOpen] = useState(false);
  const [currentApp, setCurrentApp] = useState(null);
  const [viewingApp, setViewingApp] = useState(null);
  const [actionType, setActionType] = useState('');
  const [remarks, setRemarks] = useState('');
  const [summary, setSummary] = useState({});
  const [academicYears, setAcademicYears] = useState([]);
  const [yearsLoaded, setYearsLoaded] = useState(false);
  const { addToast } = useToast();

  useEffect(() => {
    let active = true;
    api.get('/incharge/academic-years').then((r) => {
      if (!active) return;
      const years = r.data || [];
      setAcademicYears(years);
      const activeYear = years.find((y) => y.active);
      if (activeYear) {
        setFilters((prev) => ({ ...prev, academicYearId: String(activeYear.id) }));
      }
      setYearsLoaded(true);
    }).catch(() => {
      if (!active) return;
      addToast('Failed to load academic years', 'error');
      setYearsLoaded(true);
    });
    return () => { active = false; };
  }, [addToast]);

  const fetchApplications = useCallback(async () => {
    setLoading(true);
    try {
      const params = { unpaged: true };
      if (filters.academicYearId) params.academicYearId = filters.academicYearId;
      const [response, summaryResponse] = await Promise.all([
        api.get('/incharge/applications', { params }),
        api.get('/incharge/applications/summary', { params: { academicYearId: filters.academicYearId || undefined } }),
      ]);
      setApplications(response.data.content || response.data || []);
      setSummary(summaryResponse.data || {});
    } catch (error) {
      addToast(error.response?.data?.message || 'Failed to load applications', 'error');
    } finally {
      setLoading(false);
    }
  }, [filters.academicYearId, addToast]);

  useEffect(() => {
    if (!yearsLoaded) return;
    fetchApplications();
  }, [yearsLoaded, fetchApplications]);

  // Keep URL in sync when status filter changes
  const handleStatusChange = (newStatus) => {
    setFilters((prev) => ({ ...prev, status: newStatus }));
    if (newStatus) {
      setSearchParams({ status: newStatus });
    } else {
      setSearchParams({});
    }
  };

  const filtered = useMemo(() => applications.filter((row) => {
    const point = row.boardingPoint?.stationName || row.boardingPointName || '';
    const haystack = [row.studentName, row.rollNumber, row.busNumber, point, row.academicYear, row.status].join(' ').toLowerCase();
    return (!filters.search || haystack.includes(filters.search.toLowerCase()))
      && (!filters.status || row.status === filters.status)
      && (!filters.boardingPoint || point.toLowerCase().includes(filters.boardingPoint.toLowerCase()));
  }), [applications, filters]);

  const decide = async () => {
    if (actionType === 'REJECT' && !remarks.trim()) { addToast('A rejection reason is required', 'error'); return; }
    try {
      if (actionType === 'APPROVE') await api.put(`/incharge/applications/${currentApp.id}/approve`);
      else await api.put(`/incharge/applications/${currentApp.id}/reject`, { remarks: remarks.trim() });
      addToast(`Application ${actionType.toLowerCase()}d successfully`, 'success');
      setDialogOpen(false);
      await fetchApplications();
    } catch (error) {
      addToast(error.response?.data?.message || 'Action failed', 'error');
    }
  };

  const actionable = (row) => ['PENDING', 'UNDER_REVIEW'].includes(row.status);
  const columns = [
    { key: 'studentName', label: 'Student Name', render: row => row.studentName || '—' },
    { key: 'rollNumber', label: 'Roll Number', render: row => row.rollNumber || '—' },
    { key: 'busNumber', label: 'Bus', render: row => row.busNumber || '—' },
    { key: 'boardingPoint', label: 'Boarding Point', render: row => row.boardingPoint?.stationName || row.boardingPointName || '—' },
    { key: 'academicYear', label: 'Academic Year', render: row => row.academicYear || '—' },
    { key: 'status', label: 'Status', render: row => <StatusBadge status={row.status} /> },
    {
      key: 'actions', label: 'Actions', render: row => (
        <div className="flex min-w-[200px] flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => setViewingApp(row)}
            className="inline-flex items-center gap-1 rounded bg-blue-100 px-2 py-1 text-xs font-medium text-blue-800 hover:bg-blue-200 transition-colors"
          >
            <Eye size={13} /> View
          </button>
          {actionable(row) && (
            <>
              <button
                type="button"
                onClick={() => { setCurrentApp(row); setActionType('APPROVE'); setRemarks(''); setDialogOpen(true); }}
                className="rounded bg-green-100 px-2 py-1 text-xs font-medium text-green-700 hover:bg-green-200 transition-colors"
              >
                Approve
              </button>
              <button
                type="button"
                onClick={() => { setCurrentApp(row); setActionType('REJECT'); setRemarks(''); setDialogOpen(true); }}
                className="rounded bg-red-100 px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-200 transition-colors"
              >
                Reject
              </button>
            </>
          )}
        </div>
      )
    },
  ];

  const summaryChips = [
    { label: 'Total', value: summary.total, statusFilter: '' },
    { label: 'Pending', value: summary.pending, statusFilter: 'PENDING' },
    { label: 'Under review', value: summary.under_review, statusFilter: 'UNDER_REVIEW' },
    { label: 'Approved', value: summary.approved, statusFilter: 'APPROVED' },
    { label: 'Rejected', value: summary.rejected, statusFilter: 'REJECTED' },
    { label: 'Cancelled', value: summary.cancelled, statusFilter: 'CANCELLED' },
  ];

  return (
    <div className="card space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold">Manage Applications</h2>
          <p className="text-sm text-gray-500">{filtered.length} matching application(s)</p>
        </div>
        <button type="button" className="btn-secondary" onClick={fetchApplications}>Refresh</button>
      </div>

      {/* Clickable summary chips */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        {summaryChips.map(({ label, value, statusFilter }) => (
          <div
            key={label}
            onClick={() => handleStatusChange(statusFilter)}
            className={`rounded-lg border bg-white p-3 cursor-pointer transition-all hover:shadow-md hover:ring-2 hover:ring-blue-300 ${filters.status === statusFilter ? 'ring-2 ring-blue-500 bg-blue-50' : ''}`}
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

      {/* Filters */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <input
          className="input-field"
          placeholder="Search student, roll, bus or status"
          value={filters.search}
          onChange={e => setFilters({ ...filters, search: e.target.value })}
        />
        <select
          className="input-field"
          value={filters.status}
          onChange={e => handleStatusChange(e.target.value)}
        >
          <option value="">All statuses</option>
          {['PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED'].map(value => (
            <option key={value} value={value}>{value.replaceAll('_', ' ')}</option>
          ))}
        </select>
        <input
          className="input-field"
          placeholder="Boarding point"
          value={filters.boardingPoint}
          onChange={e => setFilters({ ...filters, boardingPoint: e.target.value })}
        />
        <select
          className="input-field"
          value={filters.academicYearId}
          onChange={e => setFilters({ ...filters, academicYearId: e.target.value })}
        >
          <option value="">All academic years</option>
          {academicYears.map(year => (
            <option key={year.id} value={year.id}>{year.yearName}{year.active ? ' (Active)' : ''}</option>
          ))}
        </select>
      </div>

      <DataTable columns={columns} data={filtered} loading={loading} searchable={false} />

      <ConfirmDialog
        isOpen={dialogOpen}
        onCancel={() => setDialogOpen(false)}
        onConfirm={decide}
        title={`${actionType === 'APPROVE' ? 'Approve' : 'Reject'} Application`}
        message={
          <div className="space-y-3">
            <p>Confirm {actionType.toLowerCase()} for {currentApp?.studentName || currentApp?.rollNumber}.</p>
            {actionType === 'REJECT' && (
              <textarea
                className="input-field"
                maxLength="500"
                placeholder="Rejection remarks"
                value={remarks}
                onChange={e => setRemarks(e.target.value)}
              />
            )}
          </div>
        }
      />

      <StudentApplicationDetailModal
        isOpen={Boolean(viewingApp)}
        onClose={() => setViewingApp(null)}
        application={viewingApp}
        role="incharge"
        onApprove={(app) => {
          setCurrentApp(app);
          setActionType('APPROVE');
          setRemarks('');
          setDialogOpen(true);
        }}
        onReject={(app) => {
          setCurrentApp(app);
          setActionType('REJECT');
          setRemarks('');
          setDialogOpen(true);
        }}
      />
    </div>
  );
}
