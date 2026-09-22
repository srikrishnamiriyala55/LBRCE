import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2 } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';

const BusManagementPage = () => {
  const [buses, setBuses] = useState([]);
  const [incharges, setIncharges] = useState([]);
  const [loading, setLoading] = useState(true);

  // Add/Edit Bus Modal
  const [busModalOpen, setBusModalOpen] = useState(false);
  const [editingBus, setEditingBus] = useState(null);
  const [busForm, setBusForm] = useState({
    busNumber: '',
    totalSeats: 50,
    startingPoint: '',
    endingPoint: ''
  });

  // Deactivate Confirm
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [busToDelete, setBusToDelete] = useState(null);
  const [assignmentBus, setAssignmentBus] = useState(null);
  const [selectedInchargeId, setSelectedInchargeId] = useState('');

  const { addToast } = useToast();

  useEffect(() => {
    fetchBuses();
    api.get('/admin/incharges').then(r => setIncharges(r.data)).catch(() => {});
  }, []);

  const fetchBuses = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/buses');
      setBuses(res.data);
    } catch (err) {
      addToast('Failed to load buses', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenBusModal = (bus = null) => {
    if (bus) {
      setEditingBus(bus);
      setBusForm({
        busNumber: bus.busNumber,
        totalSeats: bus.totalSeats || 50,
        startingPoint: bus.startingPoint || '',
        endingPoint: bus.endingPoint || ''
      });
    } else {
      setEditingBus(null);
      setBusForm({
        busNumber: '',
        totalSeats: 50,
        startingPoint: '',
        endingPoint: 'LBRCE Campus'
      });
    }
    setBusModalOpen(true);
  };

  const handleSaveBus = async (e) => {
    e.preventDefault();
    try {
      if (editingBus) {
        await api.put(`/admin/buses/${editingBus.id}`, busForm);
        addToast('Bus updated successfully', 'success');
      } else {
        await api.post('/admin/buses', busForm);
        addToast('Bus created successfully', 'success');
      }
      setBusModalOpen(false);
      fetchBuses();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to save bus', 'error');
    }
  };

  const handleDeleteBus = async () => {
    try {
      await api.delete(`/admin/buses/${busToDelete.id}`);
      addToast('Bus deactivated', 'success');
      setConfirmOpen(false);
      fetchBuses();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to deactivate bus', 'error');
    }
  };

  const assignedBusCount = (inchargeId) => buses.filter(bus => bus.inchargeId === inchargeId).length;

  const openAssignment = (bus) => {
    setAssignmentBus(bus);
    setSelectedInchargeId(bus.inchargeId ? String(bus.inchargeId) : '');
  };

  const assignIncharge = async (event) => {
    event.preventDefault();
    if (!assignmentBus || !selectedInchargeId) return;
    try { await api.put(`/admin/buses/${assignmentBus.id}/incharge`, { inchargeId: Number(selectedInchargeId) }); addToast('In-Charge assigned', 'success'); setAssignmentBus(null); await fetchBuses(); }
    catch (err) { addToast(err.response?.data?.message || 'Assignment failed', 'error'); }
  };

  const unassignIncharge = async (bus) => {
    if (!window.confirm(`Remove the current In-Charge from ${bus.busNumber}?`)) return;
    try { await api.delete(`/admin/buses/${bus.id}/incharge`); addToast('In-Charge unassigned', 'success'); fetchBuses(); }
    catch (err) { addToast(err.response?.data?.message || 'Unassignment failed', 'error'); }
  };

  const columns = [
    { key: 'busNumber', label: 'Bus Number', render: (row) => <span className="font-bold text-blue-900">{row.busNumber}</span> },
    { key: 'startingPoint', label: 'Starting Point', render: (row) => row.startingPoint || 'N/A' },
    { key: 'endingPoint', label: 'Ending Point', render: (row) => row.endingPoint || 'N/A' },
    { key: 'capacity', label: 'Capacity / Occupied', render: (row) => `${row.occupiedSeats || 0} / ${row.totalSeats || 0} seats` },
    { key: 'inchargeName', label: 'In-Charge', render: row => row.inchargeName || 'Unassigned' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    {
      key: 'actions',
      label: 'Actions',
      render: (row) => (
        <div className="flex items-center gap-2">
          <button onClick={() => handleOpenBusModal(row)} className="p-1.5 text-gray-700 hover:bg-gray-100 rounded" title="Edit Bus">
            <Edit2 size={16} />
          </button>
          <button onClick={() => openAssignment(row)} className="text-xs text-blue-700 hover:underline">{row.inchargeName ? 'Change' : 'Assign'}</button>
          {row.inchargeName && <button onClick={() => unassignIncharge(row)} className="text-xs text-red-600 hover:underline">Unassign</button>}
          <button onClick={() => { setBusToDelete(row); setConfirmOpen(true); }} className="p-1.5 text-red-600 hover:bg-red-50 rounded" title="Deactivate Bus">
            <Trash2 size={16} />
          </button>
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Bus Fleet Management</h2>
          <p className="text-gray-500 text-sm">Create and oversee institutional college buses and capacity</p>
        </div>
        <button onClick={() => handleOpenBusModal()} className="btn-primary flex items-center gap-2">
          <Plus size={18} /> Add New Bus
        </button>
      </div>

      <div className="card">
        <DataTable columns={columns} data={buses} loading={loading} />
      </div>

      {/* Add/Edit Bus Modal */}
      <Modal isOpen={busModalOpen} onClose={() => setBusModalOpen(false)} title={editingBus ? 'Edit Bus' : 'Add New Bus'}>
        <form onSubmit={handleSaveBus} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Bus Registration / Number *</label>
            <input
              type="text"
              required
              placeholder="e.g. AP 16 TX 1234 / Bus 12"
              className="input-field"
              value={busForm.busNumber}
              onChange={(e) => setBusForm({ ...busForm, busNumber: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Total Seats *</label>
              <input
                type="number"
                min="10"
                max="100"
                required
                className="input-field"
                value={busForm.totalSeats}
                onChange={(e) => setBusForm({ ...busForm, totalSeats: parseInt(e.target.value) || 0 })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Starting Point *</label>
              <input
                type="text"
                placeholder="e.g. Vijayawada Bus Stand"
                className="input-field"
                required
                value={busForm.startingPoint}
                onChange={(e) => setBusForm({ ...busForm, startingPoint: e.target.value })}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Destination / Ending Point *</label>
            <input
              type="text"
              placeholder="e.g. LBRCE Campus, Mylavaram"
              className="input-field"
              required
              value={busForm.endingPoint}
              onChange={(e) => setBusForm({ ...busForm, endingPoint: e.target.value })}
            />
          </div>

          <div className="flex justify-end gap-3 mt-6">
            <button type="button" onClick={() => setBusModalOpen(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-primary">{editingBus ? 'Save Changes' : 'Create Bus'}</button>
          </div>
        </form>
      </Modal>

      {/* Confirm Dialog */}
      <ConfirmDialog
        isOpen={confirmOpen}
        title="Deactivate Bus"
        message={`Are you sure you want to deactivate bus ${busToDelete?.busNumber}?`}
        onConfirm={handleDeleteBus}
        onCancel={() => setConfirmOpen(false)}
      />

      <Modal isOpen={Boolean(assignmentBus)} onClose={() => setAssignmentBus(null)} title="Assign In-Charge">
        <form onSubmit={assignIncharge} className="space-y-4">
          <p className="text-sm text-gray-600">Select an active In-charge for <strong>{assignmentBus?.busNumber}</strong>.</p>
          <div>
            <label htmlFor="inchargeAssignment" className="block text-sm font-medium text-gray-700 mb-1">In-Charge *</label>
            <select id="inchargeAssignment" required className="input-field" value={selectedInchargeId} onChange={(event) => setSelectedInchargeId(event.target.value)}>
              <option value="">Select In-Charge</option>
              {incharges.map((incharge) => {
                const count = assignedBusCount(incharge.id);
                const isCurrent = assignmentBus?.inchargeId === incharge.id;
                const unavailable = incharge.status !== 'ACTIVE' || (count > 0 && !isCurrent);
                return <option key={incharge.id} value={incharge.id} disabled={unavailable}>
                  {incharge.teacherId} — {incharge.name} ({count} {count === 1 ? 'bus' : 'buses'} assigned){incharge.status !== 'ACTIVE' ? ' — Inactive' : unavailable ? ' — Unavailable' : ''}
                </option>;
              })}
            </select>
            <p className="mt-1 text-xs text-gray-500">An In-charge already assigned to another bus is shown but cannot be selected.</p>
          </div>
          <div className="flex justify-end gap-3">
            <button type="button" onClick={() => setAssignmentBus(null)} className="btn-secondary">Cancel</button>
            <button type="submit" disabled={!selectedInchargeId} className="btn-primary disabled:opacity-50">Assign In-Charge</button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default BusManagementPage;
