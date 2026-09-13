import React, { useState, useEffect } from 'react';
import { Plus, Calendar, CheckCircle } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';

const AcademicYearPage = () => {
  const [years, setYears] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({
    yearName: '',
    startDate: '',
    endDate: ''
  });

  const { addToast } = useToast();

  useEffect(() => {
    fetchYears();
  }, []);

  const fetchYears = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/academic-years');
      setYears(res.data);
    } catch (err) {
      addToast('Failed to load academic years', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      await api.post('/admin/academic-years', form);
      addToast('Academic year created and set active', 'success');
      setModalOpen(false);
      fetchYears();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to create academic year', 'error');
    }
  };

  const columns = [
    { key: 'yearName', label: 'Academic Year', render: (row) => <span className="font-bold text-blue-900">{row.yearName}</span> },
    { key: 'startDate', label: 'Start Date', render: (row) => row.startDate || '—' },
    { key: 'endDate', label: 'End Date', render: (row) => row.endDate || '—' },
    {
      key: 'active',
      label: 'Status',
      render: (row) => (
        row.active ? (
          <span className="inline-flex items-center gap-1 text-xs font-semibold bg-green-100 text-green-800 px-2.5 py-0.5 rounded-full">
            <CheckCircle size={12} /> Active Term
          </span>
        ) : (
          <span className="text-xs font-medium bg-gray-100 text-gray-600 px-2.5 py-0.5 rounded-full">
            Archived
          </span>
        )
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Academic Years</h2>
          <p className="text-gray-500 text-sm">Configure institutional academic sessions and transit cycles</p>
        </div>
        <button onClick={() => setModalOpen(true)} className="btn-primary flex items-center gap-2">
          <Plus size={18} /> New Academic Year
        </button>
      </div>

      <div className="card">
        <DataTable columns={columns} data={years} loading={loading} />
      </div>

      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title="Create New Academic Year">
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Session / Year Name *</label>
            <input
              type="text"
              required
              placeholder="e.g. 2025-26"
              className="input-field"
              value={form.yearName}
              onChange={(e) => setForm({ ...form, yearName: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
              <input
                type="date"
                className="input-field"
                value={form.startDate}
                onChange={(e) => setForm({ ...form, startDate: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">End Date</label>
              <input
                type="date"
                className="input-field"
                value={form.endDate}
                onChange={(e) => setForm({ ...form, endDate: e.target.value })}
              />
            </div>
          </div>

          <p className="text-xs text-blue-600 bg-blue-50 p-2 rounded">
            Creating a new academic year sets it as the active session for transport allocations and pass validations.
          </p>

          <div className="flex justify-end gap-3 mt-6">
            <button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-primary">Create & Activate</button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default AcademicYearPage;
