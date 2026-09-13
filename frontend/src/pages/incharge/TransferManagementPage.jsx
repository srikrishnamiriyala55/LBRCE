import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { useToast } from '../../components/common/Toast';

const TransferManagementPage = () => {
  const [transfers, setTransfers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [currentTransfer, setCurrentTransfer] = useState(null);
  const [actionType, setActionType] = useState('');
  const [remarks, setRemarks] = useState('');
  const { addToast } = useToast();

  useEffect(() => {
    fetchTransfers();
  }, [page]);

  const fetchTransfers = async () => {
    setLoading(true);
    try {
      const [release, acceptance] = await Promise.all([
        api.get(`/incharge/transfers/pending-release?page=${page}&size=10`),
        api.get(`/incharge/transfers/pending-acceptance?page=${page}&size=10`)
      ]);
      const releaseRows = (release.data.content || []).map(t => ({...t, approvalStage: 'RELEASE'}));
      const acceptanceRows = (acceptance.data.content || []).map(t => ({...t, approvalStage: 'ACCEPTANCE'}));
      setTransfers([...releaseRows, ...acceptanceRows]);
      setTotalPages(Math.max(release.data.totalPages || 1, acceptance.data.totalPages || 1));
    } catch (err) {
      addToast('Failed to load transfers', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleAction = async () => {
    if(actionType==='REJECT'&&!remarks.trim()){addToast('A rejection reason is required','error');return;}
    try {
      if (actionType === 'APPROVE') {
        const action = currentTransfer.approvalStage === 'RELEASE' ? 'approve-release' : 'accept';
        await api.post(`/incharge/transfers/${currentTransfer.id}/${action}`, { remarks });
      } else {
        const action = currentTransfer.approvalStage === 'RELEASE' ? 'reject-release' : 'reject';
        await api.post(`/incharge/transfers/${currentTransfer.id}/${action}`);
      }
      addToast(`Transfer ${actionType.toLowerCase()}d`, 'success');
      setDialogOpen(false);
      fetchTransfers();
    } catch (err) {
      addToast(err.response?.data?.message || 'Action failed', 'error');
    }
  };

  const columns = [
    { key: 'studentName', label: 'Student', render: row => `${row.studentName} (${row.studentRollNumber})` },
    { key: 'currentBus', label: 'Current Bus', render: row => row.currentBusNumber },
    { key: 'requestedBus', label: 'Requested Bus', render: row => row.requestedBusNumber },
    { key: 'approvalStage', label: 'Your Decision', render: row => row.approvalStage === 'RELEASE' ? 'Approve release' : 'Accept into bus' },
    { key: 'reason', label: 'Reason' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    { key: 'actions', label: 'Actions', render: (row) => (
      ['TRANSFER_REQUESTED', 'OLD_INCHARGE_APPROVED'].includes(row.status) && (
        <div className="flex gap-2">
          <button onClick={() => { setCurrentTransfer(row); setActionType('APPROVE'); setRemarks(''); setDialogOpen(true); }} className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded">Approve</button>
          <button onClick={() => { setCurrentTransfer(row); setActionType('REJECT'); setRemarks(''); setDialogOpen(true); }} className="text-xs bg-red-100 text-red-700 px-2 py-1 rounded">Reject</button>
        </div>
      )
    )}
  ];

  return (
    <div className="card">
      <h2 className="text-xl font-bold mb-6">Transfer Requests</h2>
      <DataTable columns={columns} data={transfers} loading={loading} />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />

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
