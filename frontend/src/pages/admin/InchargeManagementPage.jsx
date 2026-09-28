import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Eye, EyeOff } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';
import { DEPARTMENTS, PASSWORD_HELP, PASSWORD_PATTERN, normalizePhone } from '../../utils/academicOptions';

const InchargeManagementPage = () => {
  const [incharges, setIncharges] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingIncharge, setEditingIncharge] = useState(null);
  const [showPassword, setShowPassword] = useState(false);
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
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingIncharge) {
        await api.put(`/admin/incharges/${editingIncharge.id}`, form);
        addToast('Incharge updated successfully', 'success');
      } else {
        await api.post('/admin/incharges', form);
        addToast('Incharge created successfully', 'success');
      }
      setModalOpen(false);
      fetchIncharges();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to save incharge', 'error');
    }
  };

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
        <button onClick={() => handleOpenModal(row)} className="p-1.5 text-gray-700 hover:bg-gray-100 rounded" title="Edit Faculty">
          <Edit2 size={16} />
        </button>
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
        <DataTable columns={columns} data={incharges} loading={loading} />
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
                {['Assistant Professor', 'Associate Professor', 'Professor', 'Lecturer', 'Transport Officer', 'Administrative Staff'].map((designation) => <option key={designation} value={designation}>{designation}</option>)}
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
    </div>
  );
};

export default InchargeManagementPage;
