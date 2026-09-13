import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { useToast } from '../../components/common/Toast';

const ApplicationManagementPage = () => {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState('PENDING');
  
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
      const res = await api.get(`/incharge/applications?status=${statusFilter === 'ALL' ? '' : statusFilter}&page=${page}&size=10`);
      setApplications(res.data.content || res.data);
      setTotalPages(res.data.totalPages || 1);
    } catch (err) {
      addToast('Failed to load applications', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleAction = async () => {
    try {
      if (actionType === 'APPROVE') {
        await api.put(`/incharge/applications/${currentApp.id}/approve`);
      } else {
        await api.put(`/incharge/applications/${currentApp.id}/reject`, { remarks });
      }
      addToast(`Application ${actionType.toLowerCase()}d successfully`, 'success');
      setDialogOpen(false);
      fetchApplications();
    } catch (err) {
      addToast(err.response?.data?.message || 'Action failed', 'error');
    }
  };

  const columns = [
    { key: 'studentName', label: 'Student Name' },
    { key: 'rollNumber', label: 'Roll Number' },
    { key: 'boardingPoint', label: 'Boarding Point', render: (row) => row.boardingPoint?.stationName || row.boardingPointName },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    { key: 'actions', label: 'Actions', render: (row) => (
      row.status === 'PENDING' && (
        <div className="flex gap-2">
          <button onClick={() => { setCurrentApp(row); setActionType('APPROVE'); setRemarks(''); setDialogOpen(true); }} className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded">Approve</button>
          <button onClick={() => { setCurrentApp(row); setActionType('REJECT'); setRemarks(''); setDialogOpen(true); }} className="text-xs bg-red-100 text-red-700 px-2 py-1 rounded">Reject</button>
        </div>
      )
    )}
  ];

  return (
    <div className="card">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3 mb-6">
        <h2 className="text-xl font-bold">Manage Applications</h2>
        <select className="input-field w-auto" value={statusFilter} onChange={(e) => {setStatusFilter(e.target.value); setPage(0);}}>
          <option value="ALL">All Status</option>
          <option value="PENDING">Pending</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>

      <DataTable columns={columns} data={applications} loading={loading} />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />

      <ConfirmDialog 
        isOpen={dialogOpen} 
        onCancel={() => setDialogOpen(false)} 
        onConfirm={handleAction} 
        title={`${actionType === 'APPROVE' ? 'Approve' : 'Reject'} Application`} 
        message={
          <div>
            <p>Are you sure you want to {actionType.toLowerCase()} this application for {currentApp?.studentName}?</p>
            {actionType === 'REJECT' && (
              <textarea className="input-field mt-3 w-full" placeholder="Rejection Remarks..." value={remarks} onChange={(e) => setRemarks(e.target.value)} required />
            )}
          </div>
        }
      />
    </div>
  );
};

export default ApplicationManagementPage;
