import React, { useState, useEffect } from 'react';
import { Plus, Search } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';

const StudentManagementPage = () => {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [branchFilter, setBranchFilter] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({
    rollNumber: '',
    name: '',
    email: '',
    phoneNumber: '',
    password: '',
    branch: 'CSE',
    year: 1,
    semester: 1,
    dob: '',
    gender: 'Male',
    bloodGroup: 'O+',
    address: '',
    parentName: '',
    parentPhoneNumber: ''
  });

  const { addToast } = useToast();

  useEffect(() => {
    fetchStudents();
  }, [page, branchFilter]);

  const fetchStudents = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/admin/students?search=${encodeURIComponent(search)}&branch=${encodeURIComponent(branchFilter)}&page=${page}&size=10`);
      setStudents(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch (err) {
      addToast('Failed to load students', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchStudents();
  };

  const handleCreateStudent = async (e) => {
    e.preventDefault();
    try {
      await api.post('/admin/students', form);
      addToast('Student created successfully', 'success');
      setModalOpen(false);
      fetchStudents();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to create student', 'error');
    }
  };

  const changeStatus = async (student) => {
    const next = student.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await api.put(`/admin/students/${student.id}/status`, { status: next });
      addToast(`Student account ${next === 'ACTIVE' ? 'activated' : 'deactivated'}`, 'success');
      fetchStudents();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to update student status', 'error');
    }
  };

  const columns = [
    { key: 'rollNumber', label: 'Roll Number', render: (row) => <span className="font-bold text-blue-900">{row.rollNumber}</span> },
    { key: 'name', label: 'Student Name' },
    { key: 'academic', label: 'Branch / Year', render: (row) => `${row.branch} — Year ${row.year}` },
    { key: 'contact', label: 'Phone / Email', render: (row) => `${row.phoneNumber || '—'} / ${row.email || '—'}` },
    { key: 'parent', label: 'Parent Phone', render: (row) => row.parentPhoneNumber || '—' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status || 'ACTIVE'} /> },
    { key: 'actions', label: 'Account Action', render: (row) => <button type="button" className={row.status === 'ACTIVE' ? 'text-red-600 text-sm font-medium' : 'text-green-700 text-sm font-medium'} onClick={() => changeStatus(row)}>{row.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button> }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Student Directory</h2>
          <p className="text-gray-500 text-sm">Register and manage college student records</p>
        </div>
        <button onClick={() => setModalOpen(true)} className="btn-primary flex items-center gap-2">
          <Plus size={18} /> Register Student
        </button>
      </div>

      <div className="card space-y-4">
        <form onSubmit={handleSearchSubmit} className="flex flex-wrap gap-4">
          <div className="flex-1 min-w-[240px] relative">
            <input
              type="text"
              placeholder="Search by Roll Number or Name..."
              className="input-field pl-10"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <Search className="absolute left-3 top-2.5 text-gray-400" size={18} />
          </div>
          <select
            className="input-field w-auto min-w-[150px]"
            value={branchFilter}
            onChange={(e) => { setBranchFilter(e.target.value); setPage(0); }}
          >
            <option value="">All Branches</option>
            <option value="CSE">CSE</option>
            <option value="ECE">ECE</option>
            <option value="EEE">EEE</option>
            <option value="MECH">MECH</option>
            <option value="CIVIL">CIVIL</option>
            <option value="IT">IT</option>
            <option value="AI&DS">AI & DS</option>
          </select>
          <button type="submit" className="btn-secondary">Search</button>
        </form>

        <DataTable columns={columns} data={students} loading={loading} />
        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
      </div>

      {/* Register Student Modal */}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title="Register Student">
        <form onSubmit={handleCreateStudent} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Roll Number *</label>
              <input
                type="text"
                required
                placeholder="e.g. 21761A0501"
                className="input-field uppercase"
                value={form.rollNumber}
                onChange={(e) => setForm({ ...form, rollNumber: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Student Full Name *</label>
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

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Branch *</label>
              <select
                className="input-field"
                value={form.branch}
                onChange={(e) => setForm({ ...form, branch: e.target.value })}
              >
                <option value="CSE">CSE</option>
                <option value="ECE">ECE</option>
                <option value="EEE">EEE</option>
                <option value="MECH">MECH</option>
                <option value="CIVIL">CIVIL</option>
                <option value="IT">IT</option>
                <option value="AI&DS">AI & DS</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Year *</label>
              <select
                className="input-field"
                value={form.year}
                onChange={(e) => setForm({ ...form, year: parseInt(e.target.value) })}
              >
                <option value={1}>1st Year</option>
                <option value={2}>2nd Year</option>
                <option value={3}>3rd Year</option>
                <option value={4}>4th Year</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Semester</label>
              <select
                className="input-field"
                value={form.semester}
                onChange={(e) => setForm({ ...form, semester: parseInt(e.target.value) })}
              >
                <option value={1}>1st Sem</option>
                <option value={2}>2nd Sem</option>
              </select>
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

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Parent Name</label>
              <input
                type="text"
                className="input-field"
                value={form.parentName}
                onChange={(e) => setForm({ ...form, parentName: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Parent Phone</label>
              <input
                type="text"
                className="input-field"
                value={form.parentPhoneNumber}
                onChange={(e) => setForm({ ...form, parentPhoneNumber: e.target.value })}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Initial Password *</label>
            <input
              type="password"
              required
              className="input-field"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
          </div>

          <div className="flex justify-end gap-3 mt-6">
            <button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-primary">Register Student</button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default StudentManagementPage;
