import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import { useToast } from '../../components/common/Toast';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmDialog from '../../components/common/ConfirmDialog';

const TransferPage = () => {
  const [transfers, setTransfers] = useState([]);
  const [buses, setBuses] = useState([]);
  const [boardingPoints, setBoardingPoints] = useState([]);
  const [formData, setFormData] = useState({ requestedBusId: '', requestedBoardingPointId: '', reason: '' });
  const [loading, setLoading] = useState(true);
  const [dashboard, setDashboard] = useState(null);
  const [confirmCancelOpen, setConfirmCancelOpen] = useState(false);
  const [cancellingTransfer, setCancellingTransfer] = useState(null);
  const { addToast } = useToast();

  useEffect(() => {
    fetchTransfers();
    fetchBuses();
    refreshDashboard();
  }, []);

  const refreshDashboard = () => {
    api.get('/student/dashboard')
      .then(r => setDashboard(r.data.data || r.data))
      .catch(() => setDashboard({ transportationStatus: 'NOT_ALLOCATED', canTransfer: false }));
  };

  const fetchTransfers = async () => {
    try {
      const res = await api.get('/student/transfers');
      setTransfers(res.data);
    } catch (err) {
      addToast('Failed to load transfers', 'error');
    } finally {
      setLoading(false);
    }
  };

  const fetchBuses = async () => {
    try {
      const res = await api.get('/student/buses');
      setBuses(res.data);
    } catch (err) {}
  };

  const handleBusChange = async (e) => {
    const busId = e.target.value;
    setFormData({ ...formData, requestedBusId: busId, requestedBoardingPointId: '' });
    if (busId) {
      try {
        const res = await api.get(`/student/buses/${busId}/boarding-points`);
        setBoardingPoints(res.data);
      } catch (err) {}
    } else {
      setBoardingPoints([]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.post('/student/transfers', formData);
      addToast('Transfer request submitted', 'success');
      setFormData({ requestedBusId: '', requestedBoardingPointId: '', reason: '' });
      fetchTransfers();
      refreshDashboard();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to submit request', 'error');
    }
  };

  const handleCancelTransfer = async () => {
    if (!cancellingTransfer) return;
    try {
      await api.delete(`/student/transfers/${cancellingTransfer.id}`);
      addToast('Transfer request cancelled successfully', 'success');
      setConfirmCancelOpen(false);
      setCancellingTransfer(null);
      fetchTransfers();
      refreshDashboard();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to cancel transfer request', 'error');
    }
  };

  const usedCount = dashboard?.usedTransfersCount ?? transfers.filter(t => t.status !== 'CANCELLED').length;
  const limitReached = dashboard?.transferLimitReached ?? (usedCount >= 2);
  const allocated = dashboard?.transportationStatus === 'ALLOCATED';
  const eligible = dashboard?.canTransfer === true && !limitReached;

  const columns = [
    { key: 'date', label: 'Date', render: (row) => row.requestedAt ? new Date(row.requestedAt).toLocaleDateString() : '—' },
    { key: 'currentBus', label: 'Current Bus', render: (row) => row.currentBusNumber },
    { key: 'newBus', label: 'Requested Bus', render: (row) => row.requestedBusNumber },
    { key: 'reason', label: 'Student Reason', render: (row) => row.reason || '—' },
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
      render: (row) => {
        const canCancel = ['TRANSFER_REQUESTED', 'OLD_INCHARGE_APPROVED'].includes(row.status);
        if (canCancel) {
          return (
            <button
              type="button"
              onClick={() => {
                setCancellingTransfer(row);
                setConfirmCancelOpen(true);
              }}
              className="text-xs bg-red-50 text-red-700 hover:bg-red-100 border border-red-200 px-2.5 py-1 rounded font-medium transition-colors"
            >
              Cancel
            </button>
          );
        }
        return '—';
      }
    }
  ];

  return (
    <div className="space-y-6">
      <div className="card">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-4">
          <h2 className="text-xl font-bold">Request Bus Transfer</h2>
          <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${usedCount >= 2 ? 'bg-red-50 text-red-700 border-red-200' : 'bg-blue-50 text-blue-700 border-blue-200'}`}>
            Transfers this year: {usedCount} / 2 used
          </span>
        </div>

        {limitReached && (
          <div className="mb-4 rounded-lg bg-red-50 border border-red-200 p-3 text-sm text-red-800">
            You have reached the maximum limit of 2 bus transfer requests allowed per academic year.
          </div>
        )}

        {eligible ? (
          <div className="mb-4 rounded bg-blue-50 p-3 text-sm text-blue-900">
            Current Bus: <b>{dashboard.busNumber}</b> · {dashboard.startingPoint || 'N/A'} → {dashboard.endingPoint || 'N/A'} · Boarding Point: {dashboard.boardingPoint || 'N/A'}
          </div>
        ) : !limitReached && <p className="mb-4 text-sm text-amber-700">Transfer is available only after an approved active bus assignment.</p>}
        {allocated && dashboard?.hasActiveTransfer && <p className="mb-4 text-sm text-amber-700">You already have a transfer request in progress. Wait for a decision or cancel it before creating another.</p>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <fieldset disabled={!eligible || !!dashboard?.hasActiveTransfer} className="space-y-4 disabled:opacity-60">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">New Bus</label>
                <select className="input-field" required value={formData.requestedBusId} onChange={handleBusChange}>
                  <option value="">Select Bus</option>
                  {buses.filter(b => b.busNumber !== dashboard?.busNumber).map(b => <option key={b.id} value={b.id}>{b.busNumber} — {b.startingPoint} to {b.endingPoint}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">New Boarding Point</label>
                <select className="input-field" required value={formData.requestedBoardingPointId} onChange={(e) => setFormData({...formData, requestedBoardingPointId: e.target.value})}>
                  <option value="">Select Point</option>
                  {boardingPoints.map(p => <option key={p.id} value={p.id}>{p.stationName}</option>)}
                </select>
              </div>
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">Reason for Transfer</label>
                <textarea required rows="3" className="input-field" value={formData.reason} onChange={(e) => setFormData({...formData, reason: e.target.value})}></textarea>
              </div>
            </div>
            <button type="submit" disabled={!eligible || !!dashboard?.hasActiveTransfer} className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed">Submit Request</button>
          </fieldset>
        </form>
      </div>

      <div className="card">
        <h3 className="text-lg font-bold mb-4">Transfer History</h3>
        <DataTable columns={columns} data={transfers} loading={loading} searchable={false} />
      </div>

      <ConfirmDialog
        isOpen={confirmCancelOpen}
        title="Cancel Transfer Request"
        message={
          <div>
            <p>Are you sure you want to cancel your transfer request from <b>{cancellingTransfer?.currentBusNumber}</b> to <b>{cancellingTransfer?.requestedBusNumber}</b>?</p>
            <p className="mt-2 text-xs text-gray-500">Once cancelled, this request will be invalidated and you can submit a new transfer request if eligible.</p>
          </div>
        }
        confirmText="Yes, Cancel Transfer"
        confirmVariant="danger"
        onConfirm={handleCancelTransfer}
        onCancel={() => {
          setConfirmCancelOpen(false);
          setCancellingTransfer(null);
        }}
      />
    </div>
  );
};

export default TransferPage;
