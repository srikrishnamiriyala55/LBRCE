import React, { useState, useEffect } from 'react';
import { Plus, Edit2 } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';

const InchargeManagementPage = () => {
  const [incharges, setIncharges] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingIncharge, setEditingIncharge] = useState(null);
  const [form, setForm] = useState({
    teacherId: '',
    name: '',
    email: '',
    phoneNumber: '',
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
        password: '',
        department: 'CSE',
        designation: 'Assistant Professor'
      });
    }
    setModalOpen(true);
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
                <option value="CSE">CSE</option>
                <option value="ECE">ECE</option>
                <option value="EEE">EEE</option>
                <option value="MECH">MECH</option>
                <option value="CIVIL">CIVIL</option>
                <option value="IT">IT</option>
                <option value="AI&DS">AI & DS</option>
                <option value="FED">FED</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Designation</label>
              <input
                type="text"
                className="input-field"
                value={form.designation}
                onChange={(e) => setForm({ ...form, designation: e.target.value })}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input
                type="email"
                className="input-field"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Phone Number</label>
              <input
                type="text"
                className="input-field"
                value={form.phoneNumber}
                onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {editingIncharge ? 'Password (leave blank to keep existing)' : 'Password *'}
            </label>
            <input
              type="password"
              required={!editingIncharge}
              className="input-field"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
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
