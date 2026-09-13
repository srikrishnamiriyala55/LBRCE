import React, { useState, useEffect } from 'react';
import { Plus, Edit2 } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';

const RouteManagementPage = () => {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRoute, setEditingRoute] = useState(null);
  const [form, setForm] = useState({
    routeName: '',
    startingPoint: '',
    endingPoint: '',
    description: ''
  });

  const { addToast } = useToast();

  useEffect(() => {
    fetchRoutes();
  }, []);

  const fetchRoutes = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/routes');
      setRoutes(res.data);
    } catch (err) {
      addToast('Failed to load routes', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (route = null) => {
    if (route) {
      setEditingRoute(route);
      setForm({
        routeName: route.routeName,
        startingPoint: route.startingPoint,
        endingPoint: route.endingPoint,
        description: route.description || ''
      });
    } else {
      setEditingRoute(null);
      setForm({
        routeName: '',
        startingPoint: '',
        endingPoint: 'LBRCE Campus',
        description: ''
      });
    }
    setModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingRoute) {
        await api.put(`/admin/routes/${editingRoute.id}`, form);
        addToast('Route updated successfully', 'success');
      } else {
        await api.post('/admin/routes', form);
        addToast('Route created successfully', 'success');
      }
      setModalOpen(false);
      fetchRoutes();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to save route', 'error');
    }
  };

  const toggleStatus = async (route) => {
    const next = route.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try { await api.put(`/admin/routes/${route.id}/status`, { status: next }); addToast(`Route ${next.toLowerCase()}`, 'success'); fetchRoutes(); }
    catch (err) { addToast(err.response?.data?.message || 'Failed to update route status', 'error'); }
  };

  const columns = [
    { key: 'routeName', label: 'Route Name', render: (row) => <span className="font-bold text-blue-900">{row.routeName}</span> },
    { key: 'startingPoint', label: 'Origin' },
    { key: 'endingPoint', label: 'Destination' },
    { key: 'description', label: 'Description', render: (row) => row.description || '—' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    {
      key: 'actions',
      label: 'Actions',
      render: (row) => (
        <div className="flex items-center gap-2"><button onClick={() => handleOpenModal(row)} className="p-1.5 text-gray-700 hover:bg-gray-100 rounded" title="Edit Route"><Edit2 size={16} /></button><button type="button" onClick={() => toggleStatus(row)} className={row.status === 'ACTIVE' ? 'text-xs text-red-600' : 'text-xs text-green-700'}>{row.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button></div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Transport Routes</h2>
          <p className="text-gray-500 text-sm">Define transit routes and coverage paths</p>
        </div>
        <button onClick={() => handleOpenModal()} className="btn-primary flex items-center gap-2">
          <Plus size={18} /> Add New Route
        </button>
      </div>

      <div className="card">
        <DataTable columns={columns} data={routes} loading={loading} />
      </div>

      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editingRoute ? 'Edit Route' : 'Create Route'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Route Name *</label>
            <input
              type="text"
              required
              placeholder="e.g. Vijayawada - Mylavaram Line 1"
              className="input-field"
              value={form.routeName}
              onChange={(e) => setForm({ ...form, routeName: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Origin *</label>
              <input
                type="text"
                required
                placeholder="e.g. PNBS Vijayawada"
                className="input-field"
                value={form.startingPoint}
                onChange={(e) => setForm({ ...form, startingPoint: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Destination *</label>
              <input
                type="text"
                required
                placeholder="e.g. LBRCE Campus"
                className="input-field"
                value={form.endingPoint}
                onChange={(e) => setForm({ ...form, endingPoint: e.target.value })}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <textarea
              rows="3"
              placeholder="Route path details and notable highways..."
              className="input-field"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            ></textarea>
          </div>

          <div className="flex justify-end gap-3 mt-6">
            <button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-primary">{editingRoute ? 'Save Changes' : 'Create Route'}</button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default RouteManagementPage;
