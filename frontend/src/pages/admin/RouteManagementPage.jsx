import React, { useEffect, useState } from 'react';
import { Edit2, Plus } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';

export default function RouteManagementPage() {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ routeName: '', startingPoint: '', endingPoint: '', description: '' });
  const { addToast } = useToast();

  const load = async () => {
    setLoading(true);
    try { setRoutes((await api.get('/admin/routes')).data || []); }
    catch { addToast('Failed to load routes', 'error'); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  const open = (route = null) => {
    setEditing(route);
    setForm(route ? {
      routeName: route.routeName,
      startingPoint: route.startingPoint,
      endingPoint: route.endingPoint,
      description: route.description || ''
    } : { routeName: '', startingPoint: '', endingPoint: 'LBRCE', description: '' });
    setModalOpen(true);
  };

  const save = async event => {
    event.preventDefault();
    try {
      if (editing) await api.put(`/admin/routes/${editing.id}`, form);
      else await api.post('/admin/routes', form);
      addToast(`Route ${editing ? 'updated' : 'created'} successfully`, 'success');
      setModalOpen(false);
      load();
    } catch (error) { addToast(error.response?.data?.message || 'Failed to save route', 'error'); }
  };

  const toggleStatus = async route => {
    const status = route.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await api.put(`/admin/routes/${route.id}/status`, { status });
      addToast(`Route ${status.toLowerCase()}`, 'success');
      load();
    } catch (error) { addToast(error.response?.data?.message || 'Failed to update route status', 'error'); }
  };

  const columns = [
    { key: 'routeName', label: 'Route Name', render: row => <span className="font-bold text-blue-900">{row.routeName}</span> },
    { key: 'startingPoint', label: 'Origin' },
    { key: 'endingPoint', label: 'Destination' },
    { key: 'description', label: 'Description', render: row => row.description || '—' },
    { key: 'status', label: 'Status', render: row => <StatusBadge status={row.status}/> },
    { key: 'actions', label: 'Actions', render: row => <div className="flex items-center gap-3"><button onClick={() => open(row)} className="p-1.5 text-blue-700 hover:bg-blue-50 rounded" title="Edit route"><Edit2 size={16}/></button><button onClick={() => toggleStatus(row)} className={row.status === 'ACTIVE' ? 'text-xs text-red-600' : 'text-xs text-green-700'}>{row.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button></div> }
  ];

  return <div className="space-y-6">
    <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
      <div><h2 className="text-2xl font-bold text-gray-800">Transport Routes</h2><p className="text-gray-500 text-sm">Define route origins, destinations and coverage</p></div>
      <button onClick={() => open()} className="btn-primary flex items-center justify-center gap-2"><Plus size={18}/> Add New Route</button>
    </div>
    <div className="card"><DataTable columns={columns} data={routes} loading={loading}/></div>
    <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit Route' : 'Create Route'}>
      <form onSubmit={save} className="space-y-4">
        <label className="block text-sm font-medium">Route Name *<input required className="input-field mt-1" value={form.routeName} onChange={e => setForm({...form, routeName:e.target.value})}/></label>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <label className="block text-sm font-medium">Origin *<input required className="input-field mt-1" value={form.startingPoint} onChange={e => setForm({...form, startingPoint:e.target.value})}/></label>
          <label className="block text-sm font-medium">Destination *<input required className="input-field mt-1" value={form.endingPoint} onChange={e => setForm({...form, endingPoint:e.target.value})}/></label>
        </div>
        <label className="block text-sm font-medium">Description<textarea rows="3" className="input-field mt-1" value={form.description} onChange={e => setForm({...form, description:e.target.value})}/></label>
        <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-3"><button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button><button className="btn-primary">{editing ? 'Save Changes' : 'Create Route'}</button></div>
      </form>
    </Modal>
  </div>;
}
