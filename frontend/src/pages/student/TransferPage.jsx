import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import { useToast } from '../../components/common/Toast';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';

const TransferPage = () => {
  const [transfers, setTransfers] = useState([]);
  const [buses, setBuses] = useState([]);
  const [boardingPoints, setBoardingPoints] = useState([]);
  const [formData, setFormData] = useState({ requestedBusId: '', requestedBoardingPointId: '', reason: '' });
  const [loading, setLoading] = useState(true);
  const [dashboard, setDashboard] = useState(null);
  const { addToast } = useToast();

  useEffect(() => {
    fetchTransfers();
    fetchBuses();
    api.get('/student/dashboard').then(r => setDashboard(r.data.data || r.data)).catch(() => setDashboard({ transportationStatus: 'NOT_ALLOCATED', canTransfer: false }));
  }, []);

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
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to submit request', 'error');
    }
  };

  const columns = [
    { key: 'date', label: 'Date', render: (row) => row.requestedAt ? new Date(row.requestedAt).toLocaleDateString() : '—' },
    { key: 'currentBus', label: 'Current Bus', render: (row) => row.currentBusNumber },
    { key: 'newBus', label: 'Requested Bus', render: (row) => row.requestedBusNumber },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> }
  ];

  const allocated = dashboard?.transportationStatus === 'ALLOCATED';
  const eligible = dashboard?.canTransfer === true;

  return (
    <div className="space-y-6">
      <div className="card">
        <h2 className="text-xl font-bold mb-4">Request Bus Transfer</h2>
        {eligible ? (
          <div className="mb-4 rounded bg-blue-50 p-3 text-sm text-blue-900">
            Current Bus: <b>{dashboard.busNumber}</b> · Route: {dashboard.routeName || `${dashboard.startingPoint || ''} – ${dashboard.endingPoint || ''}`} · Boarding Point: {dashboard.boardingPoint || 'N/A'}
          </div>
        ) : <p className="mb-4 text-sm text-amber-700">Transfer is available only after an approved active bus assignment.</p>}
        {allocated && dashboard?.hasActiveTransfer && <p className="mb-4 text-sm text-amber-700">You already have a transfer request in progress. Wait for a decision or cancel it before creating another.</p>}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">New Bus</label>
              <select className="input-field" required value={formData.requestedBusId} onChange={handleBusChange}>
                <option value="">Select Bus</option>
                {buses.filter(b => b.busNumber !== dashboard?.busNumber).map(b => <option key={b.id} value={b.id}>{b.busNumber} — {b.routeName || `${b.startingPoint} to ${b.endingPoint}`}</option>)}
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
          <button type="submit" disabled={!eligible} className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed">Submit Request</button>
        </form>
      </div>

      <div className="card">
        <h3 className="text-lg font-bold mb-4">Transfer History</h3>
        <DataTable columns={columns} data={transfers} loading={loading} />
      </div>
    </div>
  );
};

export default TransferPage;
