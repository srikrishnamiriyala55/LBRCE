import React, { useEffect, useState, useRef, useCallback } from 'react';
import { Edit2, Eye, Plus, Search, Trash2 } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import StudentFormModal, { emptyStudentForm, studentFormFromProfile } from '../../components/students/StudentFormModal';
import StudentDetailModal from '../../components/students/StudentDetailModal';
import { useToast } from '../../components/common/Toast';
import { DEPARTMENTS } from '../../utils/academicOptions';

export default function StudentManagementPage() {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);
  const [search, setSearch] = useState('');
  const [branch, setBranch] = useState('');
  const [academicYears, setAcademicYears] = useState([]);
  const [academicYearId, setAcademicYearId] = useState('');
  const [yearsLoaded, setYearsLoaded] = useState(false);
  const [modal, setModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [viewingStudent, setViewingStudent] = useState(null);
  const { addToast } = useToast();

  // Debounced search: 300ms after last keystroke before hitting the server
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const debounceTimer = useRef(null);
  const requestVersion = useRef(0);

  useEffect(() => {
    let active = true;
    api.get('/admin/academic-years').then((r) => {
      if (!active) return;
      const years = r.data || [];
      setAcademicYears(years);
      const activeYear = years.find((year) => year.active);
      setAcademicYearId(String(activeYear?.id || (years[0]?.id ?? '')));
      setYearsLoaded(true);
    }).catch(() => {
      if (!active) return;
      addToast('Failed to load academic years', 'error');
      setYearsLoaded(true);
    });
    return () => { active = false; };
  }, [addToast]);

  const handleSearchChange = (e) => {
    const v = e.target.value;
    setSearch(v);
    clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => setDebouncedSearch(v), 300);
  };

  const load = useCallback(async (signal) => {
    const version = ++requestVersion.current;
    setLoading(true);
    try {
      const r = await api.get('/admin/students', {
        signal,
        params: {
          search: debouncedSearch.trim() || undefined,
          branch: branch || undefined,
          academicYearId: academicYearId || undefined
        }
      });
      if (signal?.aborted || version !== requestVersion.current) return;
      const dataList = Array.isArray(r.data) ? r.data : (r.data?.content || []);
      setStudents(dataList);
      setTotal(Array.isArray(r.data) ? r.data.length : (r.data?.totalElements ?? dataList.length));
      setLoading(false);
    } catch (e) {
      if (signal?.aborted || e.name === 'CanceledError' || e.code === 'ERR_CANCELED') {
        return;
      }
      addToast(e.response?.data?.message || 'Failed to load students', 'error');
      if (version === requestVersion.current) setLoading(false);
    }
  }, [debouncedSearch, branch, academicYearId, addToast]);

  useEffect(() => {
    if (!yearsLoaded) return;
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [yearsLoaded, load]);

  const open = (s) => {
    setEditing(s || null);
    setModal(true);
  };

  const save = async (form, photo) => {
    try {
      const data = new FormData();
      const payload = { ...form, year: Number(form.year), semester: Number(form.semester) };
      if (editing) delete payload.password;
      data.append('student', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
      if (photo) data.append('photo', photo);
      if (editing) await api.put(`/admin/students/${editing.id}`, data);
      else await api.post('/admin/students', data);
      addToast(`Student ${editing ? 'updated' : 'created'} successfully`, 'success');
      setModal(false);
      setEditing(null);
      load(new AbortController().signal);
    } catch (e) {
      addToast(e.response?.data?.message || 'Failed to save student', 'error');
      throw e;
    }
  };

  const changeStatus = async (s) => {
    const status = s.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await api.put(`/admin/students/${s.id}/status`, { status });
      addToast(`Student ${status.toLowerCase()}`, 'success');
      load(new AbortController().signal);
    } catch (e) {
      addToast(e.response?.data?.message || 'Status update failed', 'error');
    }
  };

  const remove = async (s) => {
    if (!window.confirm(`Archive ${s.rollNumber}? Login and active transport access will be disabled, while historical records are retained.`)) return;
    try {
      await api.delete(`/admin/students/${s.id}`);
      addToast('Student archived; historical records retained', 'success');
      load(new AbortController().signal);
    } catch (e) {
      addToast(e.response?.data?.message || 'Student archival failed', 'error');
    }
  };

  const columns = [
    { key: 'rollNumber', label: 'Roll Number', render: (r) => <span className="font-bold text-blue-900">{r.rollNumber}</span> },
    { key: 'name', label: 'Student Name' },
    { key: 'academic', label: 'Branch / Year / Sem', render: (r) => `${r.branch || '—'} / ${r.year || '—'} / ${r.semester || '—'}` },
    {
      key: 'contact',
      label: 'Phone / Email',
      render: (r) => (
        <div>
          <div>{r.phoneNumber || '—'}</div>
          <div className="text-xs text-gray-500">{r.email || '—'}</div>
        </div>
      )
    },
    { key: 'status', label: 'Status', render: (r) => <StatusBadge status={r.status || 'ACTIVE'} /> },
    {
      key: 'actions',
      label: 'Actions',
      render: (r) => (
        <div className="flex items-center gap-2">
          <button
            type="button"
            title="View student profile & transport records"
            className="p-1.5 text-blue-700 hover:text-blue-900 transition-colors"
            onClick={() => setViewingStudent(r)}
          >
            <Eye size={16} />
          </button>
          <button
            type="button"
            title="Edit student"
            className="p-1.5 text-indigo-700 hover:text-indigo-900 transition-colors"
            onClick={() => open(r)}
          >
            <Edit2 size={16} />
          </button>
          <button
            type="button"
            className={r.status === 'ACTIVE' ? 'text-xs text-amber-700 font-medium' : 'text-xs text-green-700 font-medium'}
            onClick={() => changeStatus(r)}
          >
            {r.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
          </button>
          <button
            type="button"
            title="Archive student"
            className="p-1.5 text-red-600 hover:text-red-800 transition-colors"
            onClick={() => remove(r)}
          >
            <Trash2 size={16} />
          </button>
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Student Directory</h2>
          <p className="text-sm text-gray-500">{total} student academic record(s) displayed for the selected year</p>
        </div>
        <button onClick={() => open(null)} className="btn-primary flex items-center gap-2">
          <Plus size={18} /> Register Student
        </button>
      </div>

      <div className="card space-y-4">
        <div className="flex flex-wrap gap-3">
          <div className="relative min-w-[240px] flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
            <input
              className="input-field !pl-10"
              placeholder="Search roll, name, email, phone, department, year, semester or status"
              value={search}
              onChange={handleSearchChange}
            />
          </div>
          <select className="input-field sm:w-48" value={branch} onChange={(e) => setBranch(e.target.value)}>
            <option value="">All departments</option>
            {DEPARTMENTS.map((v) => <option key={v} value={v}>{v}</option>)}
          </select>
          <select className="input-field sm:w-48" value={academicYearId} onChange={(e) => setAcademicYearId(e.target.value)}>
            <option value="">All academic years</option>
            {academicYears.map((year) => <option key={year.id} value={year.id}>{year.yearName}{year.active ? ' (Active)' : ''}</option>)}
          </select>
        </div>

        <DataTable columns={columns} data={students} loading={loading} searchable={false} />
      </div>

      <StudentFormModal
        isOpen={modal}
        onClose={() => setModal(false)}
        title={editing ? 'Edit Student' : 'Register Student'}
        initial={editing ? studentFormFromProfile(editing) : emptyStudentForm}
        mode={editing ? 'adminEdit' : 'create'}
        onSubmit={save}
      />

      <StudentDetailModal
        isOpen={Boolean(viewingStudent)}
        onClose={() => setViewingStudent(null)}
        studentId={viewingStudent?.id}
        initialStudent={viewingStudent}
      />
    </div>
  );
}
