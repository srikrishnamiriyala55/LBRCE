import React, { useEffect, useState } from 'react';
import { Edit2, Eye } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import StudentFormModal, { studentFormFromProfile } from '../../components/students/StudentFormModal';
import InchargeStudentDetailModal from '../../components/students/InchargeStudentDetailModal';
import { useToast } from '../../components/common/Toast';
import { DEPARTMENTS } from '../../utils/academicOptions';

export default function StudentListPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({ search: '', branch: '', year: '', paymentStatus: '', passStatus: '' });
  const [editing, setEditing] = useState(null);
  const [viewingStudent, setViewingStudent] = useState(null);
  const { addToast } = useToast();

  const load = async () => {
    setLoading(true);
    try {
      const q = new URLSearchParams({
        unpaged: 'true',
        ...Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== ''))
      });
      const r = await api.get(`/incharge/students?${q}`);
      setRows(r.data.content || []);
    } catch (e) {
      addToast(e.response?.data?.message || 'Failed to load students', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [filters]);

  const save = async (form, photo) => {
    try {
      const { rollNumber, password, ...payload } = form;
      const data = new FormData();
      data.append('student', new Blob([JSON.stringify({ ...payload, year: Number(payload.year), semester: Number(payload.semester) })], { type: 'application/json' }));
      if (photo) data.append('photo', photo);
      await api.put(`/incharge/students/${editing.studentId}`, data);
      addToast('Student details updated', 'success');
      setEditing(null);
      load();
    } catch (e) {
      addToast(e.response?.data?.message || 'Student update failed', 'error');
      throw e;
    }
  };

  const columns = [
    { key: 'name', label: 'Student' },
    { key: 'rollNumber', label: 'Roll Number' },
    { key: 'branch', label: 'Department / Year', render: (r) => `${r.branch || '—'} / ${r.year || '—'}` },
    { key: 'boardingPoint', label: 'Boarding Point' },
    { key: 'fee', label: 'Fee', render: (r) => `₹${r.paidAmount} / ₹${r.requiredFee}` },
    { key: 'feeStatus', label: 'Payment', render: (r) => <StatusBadge status={r.feeStatus} /> },
    { key: 'passStatus', label: 'Pass', render: (r) => <StatusBadge status={r.passStatus} /> },
    {
      key: 'actions',
      label: 'Actions',
      render: (r) => (
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            title="View student profile"
            className="p-1.5 text-blue-700 hover:text-blue-900 transition-colors"
            onClick={() => setViewingStudent(r)}
          >
            <Eye size={16} />
          </button>
          <button
            type="button"
            title="Edit student details"
            className="p-1.5 text-indigo-700 hover:text-indigo-900 transition-colors"
            onClick={() => setEditing(r)}
          >
            <Edit2 size={16} />
          </button>
        </div>
      )
    }
  ];

  return (
    <>
      <div className="card space-y-4">
        <h2 className="text-xl font-bold">Assigned Bus Students</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-2">
          <input
            className="input-field"
            placeholder="Search name / roll"
            value={filters.search}
            onChange={(e) => setFilters({ ...filters, search: e.target.value })}
          />
          <select
            className="input-field"
            value={filters.branch}
            onChange={(e) => setFilters({ ...filters, branch: e.target.value })}
          >
            <option value="">All departments</option>
            {DEPARTMENTS.map((v) => <option key={v} value={v}>{v}</option>)}
          </select>
          <select
            className="input-field"
            value={filters.year}
            onChange={(e) => setFilters({ ...filters, year: e.target.value })}
          >
            <option value="">All years</option>
            {[1, 2, 3, 4].map((v) => <option key={v} value={v}>{v}</option>)}
          </select>
          <select
            className="input-field"
            value={filters.paymentStatus}
            onChange={(e) => setFilters({ ...filters, paymentStatus: e.target.value })}
          >
            <option value="">All payment states</option>
            <option value="PAID">Paid</option>
            <option value="PARTIALLY_PAID">Partially paid</option>
            <option value="PENDING">Pending</option>
          </select>
          <select
            className="input-field"
            value={filters.passStatus}
            onChange={(e) => setFilters({ ...filters, passStatus: e.target.value })}
          >
            <option value="">All passes</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="INACTIVE">INACTIVE</option>
          </select>
        </div>
        <DataTable columns={columns} data={rows} loading={loading} searchable={false} />
      </div>

      <StudentFormModal
        isOpen={Boolean(editing)}
        onClose={() => setEditing(null)}
        title="Edit Assigned Student"
        initial={editing ? studentFormFromProfile(editing) : undefined}
        mode="inchargeEdit"
        onSubmit={save}
      />

      <InchargeStudentDetailModal
        isOpen={Boolean(viewingStudent)}
        onClose={() => setViewingStudent(null)}
        student={viewingStudent}
      />
    </>
  );
}
