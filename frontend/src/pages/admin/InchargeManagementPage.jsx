import { Eye, EyeOff, Plus, Upload, Search } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';
import { DEPARTMENTS, PASSWORD_HELP, PASSWORD_PATTERN, normalizePhone } from '../../utils/academicOptions';
import api from '../../utils/axios';

const InchargeManagementPage = () => {
  const [incharges, setIncharges] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingIncharge, setEditingIncharge] = useState(null);
  const [showPassword, setShowPassword] = useState(false);
  const [photo, setPhoto] = useState(null);
  const [viewing, setViewing] = useState(null);
  const [photoUrl, setPhotoUrl] = useState('');
  const [form, setForm] = useState({
    teacherId: '',
    name: '',
    email: '',
    phoneNumber: '',
    address: '',
    password: '',
    department: 'CSE',
    designation: 'Assistant Professor'
  });

  const { addToast } = useToast();

  useEffect(() => {
    fetchIncharges();
  }, []);

  const fetchIncharges = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/incharges');
      setIncharges(res.data);
    } catch (err) {
      addToast('Failed to load incharges', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (incharge = null) => {
    if (incharge) {
      setEditingIncharge(incharge);
      setForm({
        teacherId: incharge.teacherId,
        name: incharge.name,
        email: incharge.email || '',
        phoneNumber: incharge.phoneNumber || '',
        address: incharge.address || '',
        password: '',
        department: incharge.department || 'CSE',
        designation: incharge.designation || 'Assistant Professor'
      });
    } else {
      setEditingIncharge(null);
      setForm({
        teacherId: '',
        name: '',
        email: '',
        phoneNumber: '',
        address: '',
        password: '',
        department: 'CSE',
        designation: 'Assistant Professor'
      });
    }
    setModalOpen(true);
    setShowPassword(false);
    setPhoto(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingIncharge) {
        await api.put(`/admin/incharges/${editingIncharge.id}`, form);
        addToast('Incharge updated successfully', 'success');
      } else {
        const response = await api.post('/admin/incharges', form);
        if (photo) { const data = new FormData(); data.append('photo', photo); await api.put(`/admin/incharges/${response.data.id}/photo`, data); }
        addToast('Incharge created successfully', 'success');
      }
      setModalOpen(false);
      fetchIncharges();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to save incharge', 'error');
    }
  };
  const viewIncharge = async (incharge) => { try { if (photoUrl) URL.revokeObjectURL(photoUrl); let nextPhoto = ''; try { const response = await api.get(`/admin/incharges/${incharge.id}/photo`, { responseType: 'blob' }); nextPhoto = URL.createObjectURL(response.data); } catch (error) { if (error.response?.status !== 404) throw error; } setPhotoUrl(nextPhoto); setViewing(incharge); } catch (error) { addToast(error.response?.data?.message || 'Unable to load In-Charge details', 'error'); } };
  const uploadPhoto = async (incharge, file) => { if (!file) return; if (!['image/jpeg','image/png'].includes(file.type) || file.size > 2 * 1024 * 1024) { addToast('Choose a JPEG or PNG photo up to 2 MB', 'error'); return; } try { const data = new FormData(); data.append('photo', file); await api.put(`/admin/incharges/${incharge.id}/photo`, data); addToast('In-Charge photo updated', 'success'); await viewIncharge(incharge); } catch (error) { addToast(error.response?.data?.message || 'Unable to update photo', 'error'); } };

  const filteredIncharges = useMemo(() => {
    const term = searchText.trim().toLowerCase();
    return incharges.filter((ic) => {
      const matchesSearch = !term ||
        (ic.teacherId || '').toLowerCase().includes(term) ||
        (ic.name || '').toLowerCase().includes(term) ||
        (ic.email || '').toLowerCase().includes(term) ||
        (ic.phoneNumber || '').toLowerCase().includes(term) ||
        (ic.department || '').toLowerCase().includes(term) ||
        (ic.designation || '').toLowerCase().includes(term);
      const matchesStatus = !statusFilter || (ic.status || 'ACTIVE') === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [incharges, searchText, statusFilter]);

  const columns = [
    { key: 'teacherId', label: 'Teacher ID', render: (row) => <span className="font-bold text-blue-900">{row.teacherId}</span> },
    { key: 'name', label: 'Faculty Name' },
    { key: 'department', label: 'Department / Designation', render: (row) => `${row.department} — ${row.designation}` },
    { key: 'email', label: 'Email' },
    { key: 'phoneNumber', label: 'Phone' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status || 'ACTIVE'} /> },
    {
      key: 'actions',
      label: 'Actions',
      render: (row) => (
        <button onClick={() => viewIncharge(row)} className="inline-flex items-center gap-1 rounded bg-blue-100 px-2 py-1 text-xs font-medium text-blue-800" title="View In-Charge details"><Eye size={14} /> View</button>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Bus Incharges & Faculty</h2>
          <p className="text-gray-500 text-sm">Oversee appointed faculty bus in-charges</p>
        </div>
        <button onClick={() => handleOpenModal()} className="btn-primary flex items-center gap-2">
          <Plus size={18} /> Add Incharge
        </button>
      </div>

      <div className="card">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} aria-hidden="true" />
            <input
              type="text"
              className="input-field pl-9"
              placeholder="Search by name, teacher ID, email, phone, department..."
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              aria-label="Search incharges"
            />
          </div>
          <select
            className="input-field w-auto"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            aria-label="Filter by status"
          >
            <option value="">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </div>
        {!loading && <p className="mb-2 text-xs text-gray-500">{filteredIncharges.length} incharge(s) found{searchText || statusFilter ? ' matching filters' : ''}</p>}
        <DataTable columns={columns} data={filteredIncharges} loading={loading} searchable={false} />
      </div>

      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editingIncharge ? 'Edit Faculty Incharge' : 'Create Faculty Incharge'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Teacher ID *</label>
              <input
                type="text"
                required
                disabled={!!editingIncharge}
                placeholder="e.g. T1042"
                className="input-field"
                value={form.teacherId}
                onChange={(e) => setForm({ ...form, teacherId: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Faculty Name *</label>
              <input
                type="text"
                required
                placeholder="Full Name"
                className="input-field"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Department *</label>
              <select
                className="input-field"
                value={form.department}
                onChange={(e) => setForm({ ...form, department: e.target.value })}
              >
                {DEPARTMENTS.map((department) => <option key={department} value={department}>{department}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Designation</label>
              <select
                className="input-field"
                value={form.designation}
                onChange={(e) => setForm({ ...form, designation: e.target.value })}
              >
                {['Professor', 'Associate Professor', 'Sr. Assistant Professors', 'Assistant Professors', 'Transport Officer', 'Administrative Staff'].map((designation) => <option key={designation} value={designation}>{designation}</option>)}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input
                type="email" required
                className="input-field"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Phone Number</label>
              <input
                type="tel" inputMode="numeric" required pattern="[0-9]{10}" minLength="10" maxLength="10"
                className="input-field"
                value={form.phoneNumber}
                onChange={(e) => setForm({ ...form, phoneNumber: normalizePhone(e.target.value) })}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Address *</label>
            <textarea required maxLength="500" rows="3" className="input-field" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
          </div>
          {!editingIncharge && <label className="block text-sm font-medium text-gray-700">Profile Photo *<input required type="file" accept="image/jpeg,image/png" className="input-field mt-1" onChange={(event) => setPhoto(event.target.files?.[0] || null)} /><span className="mt-1 block text-xs text-gray-500">JPEG or PNG, maximum 2 MB. Only an administrator can set this photo.</span></label>}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {editingIncharge ? 'Password (leave blank to keep existing)' : 'Password *'}
            </label>
            <div className="relative"><input
              type={showPassword ? 'text' : 'password'}
              required={!editingIncharge}
              minLength={form.password ? 6 : undefined}
              maxLength="15"
              pattern={form.password ? PASSWORD_PATTERN : undefined}
              title={PASSWORD_HELP}
              className="input-field"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            /><button type="button" aria-label={showPassword ? 'Hide password' : 'Show password'} onClick={() => setShowPassword((visible) => !visible)} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500">{showPassword ? <EyeOff size={18} /> : <Eye size={18} />}</button></div>
            <span className="mt-1 block text-xs text-gray-500">{PASSWORD_HELP}</span>
          </div>

          <div className="flex justify-end gap-3 mt-6">
            <button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-primary">{editingIncharge ? 'Save Changes' : 'Create Incharge'}</button>
          </div>
        </form>
      </Modal>
      <Modal isOpen={Boolean(viewing)} onClose={() => { if (photoUrl) URL.revokeObjectURL(photoUrl); setPhotoUrl(''); setViewing(null); }} title="In-Charge Details" size="lg">
        {viewing && <div className="space-y-5"><div className="flex flex-col gap-4 sm:flex-row sm:items-center"><div className="flex h-32 w-28 items-center justify-center overflow-hidden rounded-lg border bg-gray-50 text-center text-xs text-gray-400">{photoUrl ? <img src={photoUrl} alt={viewing.name} className="h-full w-full object-cover" /> : 'Photo unavailable'}</div><label className="btn-secondary inline-flex w-fit cursor-pointer items-center gap-2"><Upload size={16} /> Update photo<input type="file" accept="image/jpeg,image/png" className="hidden" onChange={(event) => { uploadPhoto(viewing, event.target.files?.[0]); event.target.value = ''; }} /></label></div><div className="grid grid-cols-1 gap-4 sm:grid-cols-2">{[['Teacher ID',viewing.teacherId],['Name',viewing.name],['Email',viewing.email],['Phone Number',viewing.phoneNumber],['Department',viewing.department],['Designation',viewing.designation],['Status',viewing.status],['Address',viewing.address]].map(([label,value])=><div key={label} className={label==='Address'?'sm:col-span-2':''}><p className="text-xs font-medium uppercase text-gray-500">{label}</p><p className="mt-1 break-words font-medium">{value || '—'}</p></div>)}</div></div>}
      </Modal>
    </div>
  );
};

export default InchargeManagementPage;
