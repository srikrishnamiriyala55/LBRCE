import React,{useEffect,useState} from 'react';
import {Download,Filter,X} from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../common/DataTable';
import StatusBadge from '../common/StatusBadge';
import {useToast} from '../common/Toast';
import {DEPARTMENTS} from '../../utils/academicOptions';

const empty = { academicYearId: '', busNumber: '', name: '', rollNumber: '', year: '', semester: '', paymentStatus: '', accountStatus: '', applicationStatus: '', transferStatus: '', department: '', minimumRemaining: '' };

export default function TransportReportTable({ role }) {
  const [filters, setFilters] = useState(empty);
  const [applied, setApplied] = useState(empty);
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState('');
  const [buses, setBuses] = useState([]);
  const [academicYears, setAcademicYears] = useState([]);
  const [yearsLoaded, setYearsLoaded] = useState(false);
  const { addToast } = useToast();

  useEffect(() => {
    api.get(`/${role}/buses`).then(r => setBuses(r.data || [])).catch(() => {});
  }, [role]);

  useEffect(() => {
    let active = true;
    api.get(`/${role}/academic-years`).then(r => {
      if (!active) return;
      const years = r.data || [];
      setAcademicYears(years);
      const activeYear = years.find(y => y.active) || years[0];
      if (activeYear) {
        const yearId = String(activeYear.id);
        setFilters(f => ({ ...f, academicYearId: yearId }));
        setApplied(f => ({ ...f, academicYearId: yearId }));
      }
      setYearsLoaded(true);
    }).catch(() => {
      if (!active) return;
      addToast('Failed to load academic years', 'error');
      setYearsLoaded(true);
      setLoading(false);
    });
    return () => { active = false; };
  }, [role, addToast]);

  const params = (source) => {
    const p = new URLSearchParams({ unpaged: 'true' });
    Object.entries(source).forEach(([k, v]) => {
      if (v !== '' && v != null) p.set(k, v);
    });
    return p.toString();
  };

  const load = () => {
    if (!applied.academicYearId) {
      setRows([]);
      setTotal(0);
      setLoading(false);
      return;
    }
    setLoading(true);
    api.get(`/${role}/reports/students?${params(applied)}`)
      .then(r => {
        const list = Array.isArray(r.data) ? r.data : (r.data?.content || []);
        setRows(list);
        setTotal(Array.isArray(r.data) ? r.data.length : (r.data?.totalElements ?? list.length));
      })
      .catch(e => addToast(e.response?.data?.message || 'Failed to load report', 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (!yearsLoaded) return;
    load();
  }, [applied, role, yearsLoaded]);

  const apply = (e) => {
    e.preventDefault();
    if (!filters.academicYearId) {
      addToast('Academic Year is compulsory to generate report. Please select an academic year.', 'error');
      return;
    }
    setApplied({ ...filters });
  };

  const clear = () => {
    const active = academicYears.find(y => y.active) || academicYears[0];
    const next = { ...empty, academicYearId: active ? String(active.id) : '' };
    setFilters(next);
    setApplied(next);
  };

  const download = async (format) => {
    if (!applied.academicYearId) {
      addToast('Academic Year is compulsory to download report. Please select an academic year.', 'error');
      return;
    }
    setDownloading(format);
    try {
      const r = await api.get(`/${role}/reports/students/export/${format}?${params(applied)}`, { responseType: 'blob' });
      const url = URL.createObjectURL(r.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = `transport-report.${format === 'excel' ? 'xlsx' : 'pdf'}`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
      addToast(`${format === 'excel' ? 'Excel' : 'PDF'} report downloaded`, 'success');
    } catch (e) {
      addToast('Report download failed. Apply narrower filters if the report is too large.', 'error');
    } finally {
      setDownloading('');
    }
  };

  const set = (k, v) => setFilters(f => ({ ...f, [k]: v }));

  const columns = [
    { key: 'rollNumber', label: 'Roll Number' },
    { key: 'studentName', label: 'Name' },
    { key: 'department', label: 'Department' },
    { key: 'academic', label: 'Year / Sem', render: r => `${r.studyYear || '-'} / ${r.semester || '-'}` },
    ...(role === 'admin' ? [{ key: 'accountStatus', label: 'Account', render: r => <StatusBadge status={r.accountStatus} /> }] : []),
    { key: 'busNumber', label: 'Bus', render: r => r.busNumber || 'Not allocated' },
    { key: 'boardingPoint', label: 'Boarding Point', render: r => r.boardingPoint || '-' },
    { key: 'paymentStatus', label: 'Payment', render: r => <StatusBadge status={r.paymentStatus} /> },
    { key: 'paidAmount', label: 'Paid', render: r => `₹${r.paidAmount || 0}` },
    { key: 'remainingAmount', label: 'Remaining', render: r => `₹${r.remainingAmount || 0}` },
    { key: 'applicationStatus', label: 'Application', render: r => r.applicationStatus ? <StatusBadge status={r.applicationStatus} /> : <span>-</span> },
    { key: 'transferStatus', label: 'Transfer', render: r => r.transferStatus ? <StatusBadge status={r.transferStatus} /> : <span>-</span> }
  ];

  return (
    <div className="card min-w-0 space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
        <div className="min-w-0">
          <h3 className="text-lg font-bold">Filtered Student Transport Report</h3>
          <p className="text-sm text-gray-500">{total} matching record(s). Downloads use the same filters.</p>
        </div>
        <div className="grid grid-cols-2 sm:flex gap-2">
          <button disabled={!!downloading || !applied.academicYearId} onClick={() => download('excel')} className="btn-secondary flex justify-center gap-2 items-center disabled:opacity-50">
            <Download size={16} />{downloading === 'excel' ? 'Preparing...' : 'Excel'}
          </button>
          <button disabled={!!downloading || !applied.academicYearId} onClick={() => download('pdf')} className="btn-secondary flex justify-center gap-2 items-center disabled:opacity-50">
            <Download size={16} />{downloading === 'pdf' ? 'Preparing...' : 'PDF'}
          </button>
        </div>
      </div>

      <form onSubmit={apply} className="grid sm:grid-cols-2 lg:grid-cols-4 gap-3">
        <div className="space-y-1">
          <label className="block text-xs font-semibold text-gray-700">Academic Year <span className="text-red-500 font-bold">* Compulsory</span></label>
          <select required className="input-field border-blue-400 font-medium" value={filters.academicYearId} onChange={e => set('academicYearId', e.target.value)}>
            <option value="">Select academic year *</option>
            {academicYears.map(y => <option key={y.id} value={y.id}>{y.yearName}{y.active ? ' (Active)' : ''}</option>)}
          </select>
        </div>

        {role === 'admin' && (
          <div className="space-y-1">
            <label className="block text-xs font-medium text-gray-600">Bus</label>
            <select className="input-field" value={filters.busNumber} onChange={e => set('busNumber', e.target.value)}>
              <option value="">All buses</option>
              {buses.map(b => <option key={b.id} value={b.busNumber}>{b.busNumber}{b.startingPoint ? ` — ${b.startingPoint}` : ''}{b.endingPoint ? ` → ${b.endingPoint}` : ''}</option>)}
            </select>
          </div>
        )}

        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-600">Student Name</label>
          <input className="input-field" placeholder="Search name" value={filters.name} onChange={e => set('name', e.target.value)} />
        </div>

        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-600">Roll Number</label>
          <input className="input-field" placeholder="Search roll" value={filters.rollNumber} onChange={e => set('rollNumber', e.target.value)} />
        </div>

        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-600">Department</label>
          <select className="input-field" value={filters.department} onChange={e => set('department', e.target.value)}>
            <option value="">All departments</option>
            {DEPARTMENTS.map(v => <option key={v} value={v}>{v}</option>)}
          </select>
        </div>

        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-600">Study Year</label>
          <select className="input-field" value={filters.year} onChange={e => set('year', e.target.value)}>
            <option value="">All years</option>
            {[1, 2, 3, 4].map(v => <option key={v}>{v}</option>)}
          </select>
        </div>

        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-600">Semester</label>
          <select className="input-field" value={filters.semester} onChange={e => set('semester', e.target.value)}>
            <option value="">All semesters</option>
            {[1, 2, 3, 4, 5, 6, 7, 8].map(v => <option key={v}>{v}</option>)}
          </select>
        </div>

        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-600">Payment Status</label>
          <select className="input-field" value={filters.paymentStatus} onChange={e => set('paymentStatus', e.target.value)}>
            <option value="">All payment states</option>
            <option value="PAID">Paid</option>
            <option value="UNPAID">Unpaid</option>
            <option value="PARTIALLY_PAID">Partially paid</option>
            <option value="PENDING">Any balance pending</option>
          </select>
        </div>

        {role === 'admin' && (
          <div className="space-y-1">
            <label className="block text-xs font-medium text-gray-600">Account Status</label>
            <select className="input-field" value={filters.accountStatus} onChange={e => set('accountStatus', e.target.value)}>
              <option value="">All accounts</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </div>
        )}

        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-600">Application Status</label>
          <select className="input-field" value={filters.applicationStatus} onChange={e => set('applicationStatus', e.target.value)}>
            <option value="">All applications</option>
            {['PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED'].map(v => <option key={v} value={v}>{v.replaceAll('_', ' ')}</option>)}
          </select>
        </div>

        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-600">Transfer Status</label>
          <select className="input-field" value={filters.transferStatus} onChange={e => set('transferStatus', e.target.value)}>
            <option value="">All transfers</option>
            {['TRANSFER_REQUESTED', 'OLD_INCHARGE_APPROVED', 'OLD_INCHARGE_REJECTED', 'NEW_INCHARGE_APPROVED', 'NEW_INCHARGE_REJECTED', 'TRANSFER_COMPLETED', 'CANCELLED'].map(v => <option key={v} value={v}>{v.replaceAll('_', ' ')}</option>)}
          </select>
        </div>

        <div className="space-y-1">
          <label className="block text-xs font-medium text-gray-600">Min. Remaining Fee</label>
          <input type="number" min="0" className="input-field" placeholder="e.g. 5000" value={filters.minimumRemaining} onChange={e => set('minimumRemaining', e.target.value)} />
        </div>

        <div className="flex items-end gap-2 lg:col-span-4 justify-end pt-2">
          <button type="button" onClick={clear} className="btn-secondary px-4 flex items-center gap-1.5" title="Reset all filters">
            <X size={16} /> Reset Filters
          </button>
          <button type="submit" className="btn-primary px-6 flex items-center gap-2">
            <Filter size={16} /> Apply Filters & Generate
          </button>
        </div>
      </form>

      {!applied.academicYearId && !loading ? (
        <div className="p-8 text-center bg-blue-50/50 rounded-lg border border-blue-200">
          <p className="text-blue-900 font-medium">Please select an Academic Year above to display transport reports.</p>
        </div>
      ) : (
        <DataTable columns={columns} data={rows} loading={loading} searchable={false} />
      )}
    </div>
  );
}
