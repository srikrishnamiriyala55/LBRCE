import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, MapPin } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';

const BusManagementPage = () => {
  const [buses, setBuses] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [incharges, setIncharges] = useState([]);
  const [loading, setLoading] = useState(true);

  // Add/Edit Bus Modal
  const [busModalOpen, setBusModalOpen] = useState(false);
  const [editingBus, setEditingBus] = useState(null);
  const [busForm, setBusForm] = useState({
    busNumber: '',
    routeId: '',
    totalSeats: 50,
    startingPoint: '',
    endingPoint: ''
  });

  // Boarding Points Modal
  const [bpModalOpen, setBpModalOpen] = useState(false);
  const [selectedBusForBp, setSelectedBusForBp] = useState(null);
  const [boardingPoints, setBoardingPoints] = useState([]);
  const [bpForm, setBpForm] = useState({
    stationName: '',
    feeAmount: 5000,
    orderIndex: 0
  });

  // Deactivate Confirm
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [busToDelete, setBusToDelete] = useState(null);

  const { addToast } = useToast();

  useEffect(() => {
    fetchBuses();
    fetchRoutes();
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

  const fetchRoutes = async () => {
    try {
      const res = await api.get('/admin/routes');
      setRoutes(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleOpenBusModal = (bus = null) => {
    if (bus) {
      setEditingBus(bus);
      setBusForm({
        busNumber: bus.busNumber,
        routeId: bus.routeId || '',
        totalSeats: bus.totalSeats || 50,
        startingPoint: bus.startingPoint || '',
        endingPoint: bus.endingPoint || ''
      });
    } else {
      setEditingBus(null);
      setBusForm({
        busNumber: '',
        routeId: routes.length > 0 ? routes[0].id : '',
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

  const assignIncharge = async (bus) => {
    const choices = incharges.map(i => `${i.id}: ${i.teacherId} — ${i.name}`).join('\n');
    const selected = window.prompt(`Enter In-Charge database ID:\n${choices}`);
    if (!selected) return;
    try { await api.put(`/admin/buses/${bus.id}/incharge`, { inchargeId: Number(selected) }); addToast('In-Charge assigned', 'success'); fetchBuses(); }
    catch (err) { addToast(err.response?.data?.message || 'Assignment failed', 'error'); }
  };

  const unassignIncharge = async (bus) => {
    if (!window.confirm(`Remove the current In-Charge from ${bus.busNumber}?`)) return;
    try { await api.delete(`/admin/buses/${bus.id}/incharge`); addToast('In-Charge unassigned', 'success'); fetchBuses(); }
    catch (err) { addToast(err.response?.data?.message || 'Unassignment failed', 'error'); }
  };

  const toggleBoardingPoint = async (point) => {
    const next = point.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try { await api.put(`/admin/boarding-points/${point.id}/status`, { status: next }); const res=await api.get(`/admin/buses/${selectedBusForBp.id}/boarding-points`);setBoardingPoints(res.data);addToast(`Boarding point ${next.toLowerCase()}`,'success'); }
    catch(err){addToast(err.response?.data?.message||'Status update failed','error');}
  };

  const handleOpenBoardingPoints = async (bus) => {
    setSelectedBusForBp(bus);
    try {
      const res = await api.get(`/admin/buses/${bus.id}/boarding-points`);
      setBoardingPoints(res.data);
      setBpModalOpen(true);
    } catch (err) {
      addToast('Failed to load boarding points', 'error');
    }
  };

  const handleAddBoardingPoint = async (e) => {
    e.preventDefault();
    try {
      await api.post('/admin/boarding-points', {
        busId: selectedBusForBp.id,
        ...bpForm
      });
      addToast('Boarding point added', 'success');
      setBpForm({ stationName: '', feeAmount: 5000, orderIndex: boardingPoints.length + 1 });
      const res = await api.get(`/admin/buses/${selectedBusForBp.id}/boarding-points`);
      setBoardingPoints(res.data);
    } catch (err) {
      addToast('Failed to add boarding point', 'error');
    }
  };

  const columns = [
    { key: 'busNumber', label: 'Bus Number', render: (row) => <span className="font-bold text-blue-900">{row.busNumber}</span> },
    { key: 'routeName', label: 'Route', render: (row) => row.routeName || 'Unassigned' },
    { key: 'routeEndpoints', label: 'Start -> Destination', render: (row) => `${row.startingPoint || 'N/A'} → ${row.endingPoint || 'LBRCE'}` },
    { key: 'capacity', label: 'Capacity / Occupied', render: (row) => `${row.occupiedSeats || 0} / ${row.totalSeats || 0} seats` },
    { key: 'inchargeName', label: 'In-Charge', render: row => row.inchargeName || 'Unassigned' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    {
      key: 'actions',
      label: 'Actions',
      render: (row) => (
        <div className="flex items-center gap-2">
          <button onClick={() => handleOpenBoardingPoints(row)} className="p-1.5 text-blue-700 hover:bg-blue-50 rounded" title="Manage Boarding Points">
            <MapPin size={16} />
          </button>
          <button onClick={() => handleOpenBusModal(row)} className="p-1.5 text-gray-700 hover:bg-gray-100 rounded" title="Edit Bus">
            <Edit2 size={16} />
          </button>
          <button onClick={() => assignIncharge(row)} className="text-xs text-blue-700 hover:underline">{row.inchargeName ? 'Change' : 'Assign'}</button>
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

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Assigned Route</label>
            <select
              className="input-field"
              value={busForm.routeId}
              onChange={(e) => setBusForm({ ...busForm, routeId: e.target.value })}
            >
              <option value="">-- Select Route --</option>
              {routes.filter(r => r.status === 'ACTIVE').map((r) => (
                <option key={r.id} value={r.id}>{r.routeName} ({r.startingPoint} - {r.endingPoint})</option>
              ))}
            </select>
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
              <label className="block text-sm font-medium text-gray-700 mb-1">Starting Point</label>
              <input
                type="text"
                placeholder="e.g. Vijayawada Bus Stand"
                className="input-field"
                value={busForm.startingPoint}
                onChange={(e) => setBusForm({ ...busForm, startingPoint: e.target.value })}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Destination / Ending Point</label>
            <input
              type="text"
              placeholder="e.g. LBRCE Campus, Mylavaram"
              className="input-field"
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

      {/* Boarding Points Modal */}
      <Modal isOpen={bpModalOpen} onClose={() => setBpModalOpen(false)} title={`Boarding Points — Bus ${selectedBusForBp?.busNumber}`}>
        <div className="space-y-6">
          <div className="max-h-60 overflow-y-auto divide-y border rounded-lg">
            {boardingPoints.length === 0 ? (
              <p className="p-4 text-center text-gray-500 text-sm">No boarding points added yet.</p>
            ) : (
              boardingPoints.map((bp) => (
                <div key={bp.id} className="p-3 flex justify-between items-center text-sm">
                  <div>
                    <span className="font-semibold text-gray-800">{bp.stationName}</span>
                    <span className="text-xs text-gray-500 ml-2">Order: #{bp.orderIndex}</span>
                  </div>
                  <div className="flex items-center gap-3"><StatusBadge status={bp.status}/><span className="font-medium text-blue-700">₹{bp.feeAmount}</span><button type="button" onClick={()=>toggleBoardingPoint(bp)} className={bp.status==='ACTIVE'?'text-xs text-red-600':'text-xs text-green-700'}>{bp.status==='ACTIVE'?'Deactivate':'Activate'}</button></div>
                </div>
              ))
            )}
          </div>

          <form onSubmit={handleAddBoardingPoint} className="border-t pt-4 space-y-3">
            <h4 className="font-semibold text-sm text-gray-700">Add New Boarding Point</h4>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <input
                type="text"
                placeholder="Station Name *"
                required
                className="input-field col-span-2 text-sm"
                value={bpForm.stationName}
                onChange={(e) => setBpForm({ ...bpForm, stationName: e.target.value })}
              />
              <input
                type="number"
                placeholder="Fee (₹) *"
                required
                className="input-field text-sm"
                value={bpForm.feeAmount}
                onChange={(e) => setBpForm({ ...bpForm, feeAmount: parseInt(e.target.value) || 0 })}
              />
            </div>
            <button type="submit" className="btn-primary w-full text-sm">Add Station</button>
          </form>
        </div>
      </Modal>

      {/* Confirm Dialog */}
      <ConfirmDialog
        isOpen={confirmOpen}
        title="Deactivate Bus"
        message={`Are you sure you want to deactivate bus ${busToDelete?.busNumber}?`}
        onConfirm={handleDeleteBus}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
};

export default BusManagementPage;
