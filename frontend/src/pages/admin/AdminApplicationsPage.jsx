import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { useToast } from '../../components/common/Toast';

const AdminApplicationsPage = () => {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');

  const [dialogOpen, setDialogOpen] = useState(false);
  const [currentApp, setCurrentApp] = useState(null);
  const [actionType, setActionType] = useState('');
  const [remarks, setRemarks] = useState('');

  const { addToast } = useToast();

  useEffect(() => {
    fetchApplications();
  }, [page, statusFilter]);

  const fetchApplications = async () => {
    setLoading(true);
    try {
      const url = statusFilter
        ? `/admin/applications?status=${statusFilter}&page=${page}&size=10`
        : `/admin/applications?page=${page}&size=10`;
      const res = await api.get(url);
      setApplications(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch (err) {
      addToast('Failed to load applications', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleAction = async () => {
    try {
      if (actionType === 'APPROVE') {
        await api.put(`/admin/applications/${currentApp.id}/approve`);
        addToast('Application approved successfully', 'success');
      } else {
        await api.put(`/admin/applications/${currentApp.id}/reject`, remarks);
        addToast('Application rejected', 'success');
      }
      setDialogOpen(false);
      fetchApplications();
    } catch (err) {
      addToast(err.response?.data?.message || 'Action failed', 'error');
    }
  };

  const columns = [
    { key: 'appliedAt', label: 'Date', render: (row) => new Date(row.appliedAt || row.createdAt).toLocaleDateString() },
    { key: 'busNumber', label: 'Bus Number', render: (row) => <span className="font-bold text-blue-900">{row.busNumber}</span> },
    { key: 'journey', label: 'Start → End', render: (row) => `${row.startingPoint || '—'} → ${row.endingPoint || '—'}` },
    { key: 'boardingPoint', label: 'Boarding Point', render: (row) => row.boardingPointName || '—' },
    { key: 'academicYear', label: 'Academic Year', render: (row) => row.academicYear || '—' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    {
      key: 'actions',
      label: 'Actions',
      render: (row) => (
        row.status === 'PENDING' ? (
          <div className="flex gap-2">
            <button
              onClick={() => { setCurrentApp(row); setActionType('APPROVE'); setDialogOpen(true); }}
              className="text-xs bg-green-100 text-green-700 hover:bg-green-200 px-2.5 py-1 rounded font-medium"
            >
              Approve
            </button>
            <button
              onClick={() => { setCurrentApp(row); setActionType('REJECT'); setRemarks(''); setDialogOpen(true); }}
              className="text-xs bg-red-100 text-red-700 hover:bg-red-200 px-2.5 py-1 rounded font-medium"
            >
              Reject
            </button>
          </div>
        ) : (
          <span className="text-xs text-gray-400 font-medium">Reviewed</span>
        )
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Transportation Applications</h2>
          <p className="text-gray-500 text-sm">Review, approve, and track college-wide student bus applications</p>
        </div>
        <select
          className="input-field w-auto"
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
        >
          <option value="">All Applications</option>
          <option value="PENDING">Pending Only</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>

      <div className="card">
        <DataTable columns={columns} data={applications} loading={loading} />
        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
      </div>

      <ConfirmDialog
        isOpen={dialogOpen}
        title={`${actionType === 'APPROVE' ? 'Approve' : 'Reject'} Application`}
        message={`Are you sure you want to ${actionType.toLowerCase()} this transportation application for Bus ${currentApp?.busNumber}?`}
        onConfirm={handleAction}
        onCancel={() => setDialogOpen(false)}
      />
    </div>
  );
};

export default AdminApplicationsPage;
