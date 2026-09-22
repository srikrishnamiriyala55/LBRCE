import React, { useEffect, useMemo, useState } from 'react';
import { Edit2, Filter, MapPin, Plus, Trash2, X } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';

const emptyFilters = { busNumber: '', stationName: '', status: '' };

export default function BoardingPointManagementPage() {
  const [points, setPoints] = useState([]);
  const [buses, setBuses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState(emptyFilters);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ busId: '', stationName: '', feeAmount: '', orderIndex: 0 });
  const { addToast } = useToast();

  const load = async () => {
    setLoading(true);
    try {
      const [pointsResponse, busesResponse] = await Promise.all([
        api.get('/admin/boarding-points'), api.get('/admin/buses')
      ]);
      setPoints(pointsResponse.data || []);
      setBuses(busesResponse.data || []);
    } catch (error) { addToast('Failed to load boarding points', 'error'); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  const filteredPoints = useMemo(() => points.filter(point => {
    const busMatch = !filters.busNumber || point.busNumber?.toLowerCase().includes(filters.busNumber.toLowerCase());
    const pointMatch = !filters.stationName || point.stationName?.toLowerCase().includes(filters.stationName.toLowerCase());
    const statusMatch = !filters.status || point.status === filters.status;
    return busMatch && pointMatch && statusMatch;
  }), [points, filters]);

  const open = (point = null) => {
    setEditing(point);
    setForm(point ? { busId:point.busId, stationName:point.stationName, feeAmount:point.feeAmount, orderIndex:point.orderIndex ?? 0 }
      : { busId:buses[0]?.id || '', stationName:'', feeAmount:'', orderIndex:0 });
    setModalOpen(true);
  };

  const save = async event => {
    event.preventDefault();
    const payload = { ...form, busId:Number(form.busId), feeAmount:Number(form.feeAmount), orderIndex:Number(form.orderIndex || 0) };
    try {
      if (editing) await api.put(`/admin/boarding-points/${editing.id}`, { stationName:payload.stationName, feeAmount:payload.feeAmount, orderIndex:payload.orderIndex });
      else await api.post('/admin/boarding-points', payload);
      addToast(`Boarding point ${editing ? 'updated' : 'added'} successfully`, 'success');
      setModalOpen(false);
      load();
    } catch (error) { addToast(error.response?.data?.message || 'Failed to save boarding point', 'error'); }
  };

  const toggleStatus = async point => {
    const status = point.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await api.put(`/admin/boarding-points/${point.id}/status`, { status });
      addToast(`Boarding point ${status.toLowerCase()}`, 'success');
      load();
    } catch (error) { addToast(error.response?.data?.message || 'Failed to update status', 'error'); }
  };

  const remove = async point => {
    if (!window.confirm(`Delete ${point.stationName} from ${point.busNumber}?`)) return;
    try {
      await api.delete(`/admin/boarding-points/${point.id}`);
      addToast('Boarding point deleted', 'success');
      load();
    } catch (error) { addToast(error.response?.data?.message || 'Referenced points cannot be deleted; deactivate them instead', 'error'); }
  };

  const columns = [
    { key:'busNumber', label:'Bus Number', render:row => <span className="font-bold text-blue-900">{row.busNumber}</span> },
    { key:'stationName', label:'Boarding Point' },
    { key:'orderIndex', label:'Order', render:row => row.orderIndex ?? 0 },
    { key:'feeAmount', label:'Annual Fee', render:row => `₹${Number(row.feeAmount || 0).toLocaleString('en-IN')}` },
    { key:'status', label:'Status', render:row => <StatusBadge status={row.status}/> },
    { key:'actions', label:'Actions', render:row => <div className="flex items-center gap-2"><button onClick={() => open(row)} className="p-1.5 text-blue-700 hover:bg-blue-50 rounded" title="Edit point and fee"><Edit2 size={16}/></button><button onClick={() => toggleStatus(row)} className={row.status === 'ACTIVE' ? 'text-xs text-amber-700' : 'text-xs text-green-700'}>{row.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button><button onClick={() => remove(row)} className="p-1.5 text-red-600 hover:bg-red-50 rounded" title="Delete unused point"><Trash2 size={16}/></button></div> }
  ];

  return <div className="space-y-6">
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
      <div><h2 className="text-2xl font-bold text-gray-800 flex items-center gap-2"><MapPin size={24}/> Boarding Points & Fees</h2><p className="text-sm text-gray-500">Manage bus-wise pickup points and annual transport fees</p></div>
      <button disabled={!buses.length} onClick={() => open()} className="btn-primary flex items-center justify-center gap-2 disabled:opacity-50"><Plus size={18}/> Add Boarding Point</button>
    </div>

    <div className="card space-y-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        <label className="text-sm font-medium text-gray-700">Bus Number<input className="input-field mt-1" placeholder="e.g. BUS101" value={filters.busNumber} onChange={e => setFilters({...filters,busNumber:e.target.value})}/></label>
        <label className="text-sm font-medium text-gray-700">Boarding Point<input className="input-field mt-1" placeholder="Search point" value={filters.stationName} onChange={e => setFilters({...filters,stationName:e.target.value})}/></label>
        <label className="text-sm font-medium text-gray-700">Status<select className="input-field mt-1" value={filters.status} onChange={e => setFilters({...filters,status:e.target.value})}><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option><option value="RETIRED">Retired</option></select></label>
        <div className="flex items-end gap-2"><div className="flex-1 rounded-lg bg-blue-50 px-3 py-2.5 text-sm text-blue-800"><Filter size={15} className="inline mr-2"/>{filteredPoints.length} of {points.length} points</div><button onClick={() => setFilters(emptyFilters)} className="btn-secondary" title="Clear filters"><X size={17}/></button></div>
      </div>
      <DataTable columns={columns} data={filteredPoints} loading={loading} emptyMessage="No boarding points match these filters"/>
    </div>

    <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit Boarding Point & Fee' : 'Add Boarding Point'}>
      <form onSubmit={save} className="space-y-4">
        <label className="block text-sm font-medium">Bus *<select required disabled={!!editing} className="input-field mt-1 disabled:bg-gray-100" value={form.busId} onChange={e => setForm({...form,busId:e.target.value})}><option value="">Select bus</option>{buses.map(bus => <option key={bus.id} value={bus.id}>{bus.busNumber} - {bus.startingPoint} to {bus.endingPoint}</option>)}</select></label>
        <label className="block text-sm font-medium">Boarding Point *<input required maxLength="255" className="input-field mt-1" value={form.stationName} onChange={e => setForm({...form,stationName:e.target.value})}/></label>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4"><label className="block text-sm font-medium">Annual Fee (₹) *<input required type="number" min="1" className="input-field mt-1" value={form.feeAmount} onChange={e => setForm({...form,feeAmount:e.target.value})}/></label><label className="block text-sm font-medium">Display Order<input type="number" min="0" className="input-field mt-1" value={form.orderIndex} onChange={e => setForm({...form,orderIndex:e.target.value})}/></label></div>
        <p className="text-xs text-gray-500">Fee changes apply to new applications. Existing approved fee records retain their recorded amount.</p>
        <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-3"><button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button><button className="btn-primary">{editing ? 'Save Changes' : 'Add Point'}</button></div>
      </form>
    </Modal>
  </div>;
}
