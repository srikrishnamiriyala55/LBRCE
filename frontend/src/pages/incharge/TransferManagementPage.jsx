import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { useToast } from '../../components/common/Toast';

const TransferManagementPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [transfers, setTransfers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState('');
  // Initialize from URL (e.g. ?status=TRANSFER_REQUESTED from dashboard card click)
  const [statusFilter, setStatusFilter] = useState(() => searchParams.get('status') || '');

  const [dialogOpen, setDialogOpen] = useState(false);
  const [currentTransfer, setCurrentTransfer] = useState(null);
  const [actionType, setActionType] = useState('');
  const [remarks, setRemarks] = useState('');
  const { addToast } = useToast();

  useEffect(() => {
    fetchTransfers();
  }, []);

  const fetchTransfers = async () => {
    setLoading(true);
    try {
      const [release, acceptance, all] = await Promise.all([
        api.get('/incharge/transfers/pending-release?unpaged=true'),
        api.get('/incharge/transfers/pending-acceptance?unpaged=true'),
        api.get('/incharge/transfers?unpaged=true')
      ]);
      const releaseIds = new Set((release.data.content || []).map(t => t.id));
      const acceptanceIds = new Set((acceptance.data.content || []).map(t => t.id));
      const allRows = (all.data.content || []).map(t => ({
        ...t,
        approvalStage: releaseIds.has(t.id) ? 'RELEASE' : acceptanceIds.has(t.id) ? 'ACCEPTANCE' : null
      }));
      setTransfers(allRows);
    } catch (err) {
      addToast('Failed to load transfers', 'error');
    } finally {
      setLoading(false);
    }
  };

  const filteredTransfers = useMemo(() => {
    const term = searchText.trim().toLowerCase();
    return transfers.filter((t) => {
      const matchesSearch = !term ||
        (t.studentName || '').toLowerCase().includes(term) ||
        (t.studentRollNumber || '').toLowerCase().includes(term) ||
        (t.currentBusNumber || '').toLowerCase().includes(term) ||
        (t.requestedBusNumber || '').toLowerCase().includes(term) ||
        (t.reason || '').toLowerCase().includes(term) ||
        (t.remarks || '').toLowerCase().includes(term);
      const matchesStatus = !statusFilter || (t.status || '') === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [transfers, searchText, statusFilter]);

  const handleAction = async () => {
    if(actionType==='REJECT'&&!remarks.trim()){addToast('A rejection reason is required','error');return;}
    try {
      if (actionType === 'APPROVE') {
        const action = currentTransfer.approvalStage === 'RELEASE' ? 'approve-release' : 'accept';
        await api.post(`/incharge/transfers/${currentTransfer.id}/${action}`, { remarks });
      } else {
        const action = currentTransfer.approvalStage === 'RELEASE' ? 'reject-release' : 'reject';
        await api.post(`/incharge/transfers/${currentTransfer.id}/${action}`, { remarks: remarks.trim() });
      }
      addToast(`Transfer ${actionType.toLowerCase()}d`, 'success');
      setDialogOpen(false);
      fetchTransfers();
    } catch (err) {
      addToast(err.response?.data?.message || 'Action failed', 'error');
    }
  };

  const columns = [
    { key: 'studentName', label: 'Student Name', render: row => row.studentName || '—' },
    { key: 'studentRollNumber', label: 'Roll Number', render: row => row.studentRollNumber || '—' },
    { key: 'currentBus', label: 'Current Bus', render: row => row.currentBusNumber },
    { key: 'requestedBus', label: 'Requested Bus', render: row => row.requestedBusNumber },
    { key: 'reason', label: 'Reason' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    {
      key: 'remarks',
      label: 'Decision Remarks',
      render: (row) => {
        const isRejected = (row.status || '').includes('REJECTED');
        if (isRejected || row.remarks) {
          return row.remarks ? (
            <span className={`text-xs px-2 py-0.5 rounded border ${isRejected ? 'text-red-700 bg-red-50 border-red-200' : 'text-gray-700 bg-gray-50 border-gray-200'}`}>
              {row.remarks}
            </span>
          ) : (
            <span className="text-xs text-gray-400 italic">No remarks provided</span>
          );
        }
        return '—';
      }
    },
    {
      key: 'actions',
      label: 'Actions',
      render: (row) => (
        row.approvalStage ? (
          <div className="flex gap-2">
            <button
              onClick={() => { setCurrentTransfer(row); setActionType('APPROVE'); setRemarks(''); setDialogOpen(true); }}
              className="text-xs bg-green-100 hover:bg-green-200 text-green-700 px-2 py-1 rounded font-medium transition-colors"
            >
              {row.approvalStage === 'RELEASE' ? 'Release' : 'Accept'}
            </button>
            <button
              onClick={() => { setCurrentTransfer(row); setActionType('REJECT'); setRemarks(''); setDialogOpen(true); }}
              className="text-xs bg-red-100 hover:bg-red-200 text-red-700 px-2 py-1 rounded font-medium transition-colors"
            >
              Reject
            </button>
          </div>
        ) : (
          <span className="text-xs text-gray-400">No action</span>
        )
      )
    }
  ];

  return (
    <div className="card">
      <h2 className="text-xl font-bold mb-4">Transfer Requests</h2>
      <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <input
          type="text"
          className="input-field sm:col-span-2"
          placeholder="Search student, roll number, bus or reason..."
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          aria-label="Search transfers"
        />
        <select
          className="input-field"
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); if (e.target.value) setSearchParams({ status: e.target.value }); else setSearchParams({}); }}
          aria-label="Filter by status"
        >
          <option value="">All Statuses</option>
          <option value="TRANSFER_REQUESTED">Transfer Requested</option>
          <option value="OLD_INCHARGE_APPROVED">Old Incharge Approved</option>
          <option value="OLD_INCHARGE_REJECTED">Old Incharge Rejected</option>
          <option value="NEW_INCHARGE_APPROVED">New Incharge Approved</option>
          <option value="NEW_INCHARGE_REJECTED">New Incharge Rejected</option>
          <option value="TRANSFER_COMPLETED">Completed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </div>
      {!loading && <p className="mb-3 text-xs text-gray-500">{filteredTransfers.length} of {transfers.length} transfer(s){searchText || statusFilter ? ' matching filters' : ''}</p>}
      <DataTable columns={columns} data={filteredTransfers} loading={loading} searchable={false} />

      <ConfirmDialog 
        isOpen={dialogOpen} 
        onCancel={() => setDialogOpen(false)} 
        onConfirm={handleAction} 
        title={`${actionType === 'APPROVE' ? 'Approve' : 'Reject'} Transfer`} 
        message={<div><p>Are you sure you want to {actionType.toLowerCase()} this transfer request?</p>{actionType==='REJECT'&&<textarea className="input-field mt-3" maxLength="500" required placeholder="Rejection reason" value={remarks} onChange={e=>setRemarks(e.target.value)}/>}</div>}
      />
    </div>
  );
};

export default TransferManagementPage;
