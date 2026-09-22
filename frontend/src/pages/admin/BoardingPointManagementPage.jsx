import React, { useEffect, useMemo, useState } from 'react';
import { ArrowDown, ArrowUp, Edit2, Filter, MapPin, Plus, Trash2, X } from 'lucide-react';
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
  const [manageBusId, setManageBusId] = useState('');
  const [reordering, setReordering] = useState(false);
  const [form, setForm] = useState({ busId: '', pointSelection: '', stationName: '', feeAmount: '', orderIndex: 0 });
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
  useEffect(() => {
    if (!manageBusId && buses.length) setManageBusId(String(buses[0].id));
  }, [buses, manageBusId]);

  const filteredPoints = useMemo(() => points.filter(point => {
    const busMatch = !filters.busNumber || point.busNumber?.toLowerCase().includes(filters.busNumber.toLowerCase());
    const pointMatch = !filters.stationName || point.stationName?.toLowerCase().includes(filters.stationName.toLowerCase());
    const statusMatch = !filters.status || point.status === filters.status;
    return busMatch && pointMatch && statusMatch;
  }), [points, filters]);

  const availablePoints = useMemo(() => {
    const selectedBusId = Number(form.busId);
    const assignedNames = new Set(points.filter(point => point.busId === selectedBusId)
      .map(point => point.stationName.toLowerCase()));
    const distinct = new Map();
    points.forEach(point => {
      const key = point.stationName.toLowerCase();
      if (!assignedNames.has(key) && !distinct.has(key)) distinct.set(key, point);
    });
    return [...distinct.values()].sort((a, b) => a.stationName.localeCompare(b.stationName));
  }, [points, form.busId]);

  const orderedPoints = useMemo(() => points
    .filter(point => point.busId === Number(manageBusId))
    .sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0) || a.stationName.localeCompare(b.stationName)),
  [points, manageBusId]);

  const open = (point = null, busId = null) => {
    const targetBusId = busId || buses[0]?.id || '';
    const nextOrder = points.filter(existing => existing.busId === Number(targetBusId)).length;
    setEditing(point);
    setForm(point ? { busId:point.busId, pointSelection:'', stationName:point.stationName, feeAmount:point.feeAmount, orderIndex:point.orderIndex ?? 0 }
      : { busId:targetBusId, pointSelection:'', stationName:'', feeAmount:'', orderIndex:nextOrder });
    setModalOpen(true);
  };

  const movePoint = async (index, direction) => {
    const destination = index + direction;
    if (destination < 0 || destination >= orderedPoints.length || reordering) return;
    const reordered = [...orderedPoints];
    [reordered[index], reordered[destination]] = [reordered[destination], reordered[index]];
    setReordering(true);
    try {
      const response = await api.put(`/admin/buses/${manageBusId}/boarding-points/order`, {
        boardingPointIds: reordered.map(point => point.id)
      });
      const updated = new Map(response.data.map(point => [point.id, point]));
      setPoints(current => current.map(point => updated.get(point.id) || point));
      addToast('Boarding-point order updated', 'success');
    } catch (error) {
      addToast(error.response?.data?.message || 'Failed to update boarding-point order', 'error');
      load();
    } finally { setReordering(false); }
  };

  const save = async event => {
    event.preventDefault();
    const payload = { busId:Number(form.busId), feeAmount:Number(form.feeAmount), orderIndex:Number(form.orderIndex || 0) };
    if (editing || form.pointSelection === 'NEW') payload.stationName = form.stationName.trim();
    else payload.existingPointId = Number(form.pointSelection);
    try {
      if (editing) await api.put(`/admin/boarding-points/${editing.id}`, { stationName:payload.stationName, feeAmount:payload.feeAmount, orderIndex:payload.orderIndex });
      else await api.post('/admin/boarding-points', payload);
      addToast(`Boarding point ${editing ? 'updated' : 'added'} successfully`, 'success');
      setModalOpen(false);
      load();
    } catch (error) { addToast(error.response?.data?.message || 'Failed to save boarding point', 'error'); }
  };

  const selectBoardingPoint = (value) => {
    if (value === 'NEW' || !value) {
      setForm({ ...form, pointSelection:value, stationName:'', feeAmount:'' });
      return;
    }
    const selected = availablePoints.find(point => point.id === Number(value));
    setForm({ ...form, pointSelection:value, stationName:'', feeAmount:selected?.feeAmount ?? '' });
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
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <label className="block flex-1 text-sm font-medium text-gray-700">Manage Boarding Points for Bus
          <select className="input-field mt-1" value={manageBusId} onChange={event => setManageBusId(event.target.value)}>
            {buses.map(bus => <option key={bus.id} value={bus.id}>{bus.busNumber} — {bus.startingPoint} to {bus.endingPoint}</option>)}
          </select>
        </label>
        <button disabled={!manageBusId} onClick={() => open(null, Number(manageBusId))} className="btn-primary flex items-center justify-center gap-2 disabled:opacity-50"><Plus size={18}/> Add Point to This Bus</button>
      </div>
      <p className="text-sm text-gray-500">The top point is order 0. Use the arrows to change the travel sequence; all order numbers are saved automatically.</p>
      <div className="divide-y rounded-lg border">
        {orderedPoints.map((point, index) => <div key={point.id} className="flex flex-col gap-3 p-3 sm:flex-row sm:items-center">
          <div className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-50 text-sm font-bold text-blue-800">{index}</span>
            <div><p className="font-medium text-gray-900">{point.stationName}</p><p className="text-xs text-gray-500">₹{Number(point.feeAmount || 0).toLocaleString('en-IN')} · {point.status}</p></div>
          </div>
          <div className="flex flex-wrap items-center gap-2 sm:ml-auto">
            <button type="button" disabled={index === 0 || reordering} onClick={() => movePoint(index, -1)} className="btn-secondary p-2 disabled:opacity-40" title="Move up"><ArrowUp size={16}/></button>
            <button type="button" disabled={index === orderedPoints.length - 1 || reordering} onClick={() => movePoint(index, 1)} className="btn-secondary p-2 disabled:opacity-40" title="Move down"><ArrowDown size={16}/></button>
            <button type="button" onClick={() => open(point)} className="p-2 text-blue-700" title="Edit point and fee"><Edit2 size={16}/></button>
            <button type="button" onClick={() => toggleStatus(point)} className={point.status === 'ACTIVE' ? 'text-xs text-amber-700' : 'text-xs text-green-700'}>{point.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button>
            <button type="button" onClick={() => remove(point)} className="p-2 text-red-600" title="Delete unused point"><Trash2 size={16}/></button>
          </div>
        </div>)}
        {!loading && !orderedPoints.length && <p className="p-6 text-center text-sm text-gray-500">No boarding points are assigned to this bus.</p>}
      </div>
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
        <label className="block text-sm font-medium">Bus *<select required disabled={!!editing} className="input-field mt-1 disabled:bg-gray-100" value={form.busId} onChange={e => { const busId=e.target.value; setForm({...form,busId,pointSelection:'',stationName:'',feeAmount:'',orderIndex:points.filter(point => point.busId === Number(busId)).length}); }}><option value="">Select bus</option>{buses.map(bus => <option key={bus.id} value={bus.id}>{bus.busNumber} - {bus.startingPoint} to {bus.endingPoint}</option>)}</select></label>
        {editing ? <label className="block text-sm font-medium">Boarding Point *<input required maxLength="255" className="input-field mt-1" value={form.stationName} onChange={e => setForm({...form,stationName:e.target.value})}/></label> : <>
          <label className="block text-sm font-medium">Boarding Point *<select required disabled={!form.busId} className="input-field mt-1 disabled:bg-gray-100" value={form.pointSelection} onChange={e => selectBoardingPoint(e.target.value)}><option value="">Select available point</option>{availablePoints.map(point => <option key={point.id} value={point.id}>{point.stationName} — ₹{Number(point.feeAmount || 0).toLocaleString('en-IN')}</option>)}<option value="NEW">＋ Add new boarding point</option></select></label>
          {form.pointSelection === 'NEW' && <label className="block text-sm font-medium">New Boarding Point Name *<input required maxLength="255" className="input-field mt-1" placeholder="Enter a unique point name" value={form.stationName} onChange={e => setForm({...form,stationName:e.target.value})}/><span className="mt-1 block text-xs text-gray-500">If this name already exists, select it from the dropdown instead.</span></label>}
        </>}
        <label className="block text-sm font-medium">Annual Fee (₹) *<input required type="number" min="1" className="input-field mt-1" value={form.feeAmount} onChange={e => setForm({...form,feeAmount:e.target.value})}/><span className="mt-1 block text-xs text-gray-500">Existing-point fees are filled automatically and can be changed for this bus.</span></label>
        <p className="text-xs text-gray-500">Fee changes apply to new applications. Existing approved fee records retain their recorded amount.</p>
        <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-3"><button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button><button className="btn-primary">{editing ? 'Save Changes' : 'Add Point'}</button></div>
      </form>
    </Modal>
  </div>;
}
