import React, { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import PassViewerModal from '../../components/passes/PassViewerModal';
import { useToast } from '../../components/common/Toast';
import { Download, FileSpreadsheet, RefreshCw, Search } from 'lucide-react';
import { formatINR } from '../../utils/currency';
import { downloadPaymentsCsv, downloadPaymentsPdf } from '../../utils/paymentRecordsExport';

export default function AdminDataPage({ type }) {
  const [rows, setRows] = useState([]), [total, setTotal] = useState(0), [loading, setLoading] = useState(true);
  const [selectedPass, setSelectedPass] = useState(null), [photoUrl, setPhotoUrl] = useState(''), [downloading, setDownloading] = useState(false);
  const [searchParams, setSearchParams] = useSearchParams();
  const [search, setSearch] = useState(''), [appliedSearch, setAppliedSearch] = useState('');
  const [refreshKey, setRefreshKey] = useState(0), [lastUpdated, setLastUpdated] = useState(null);
  const [approvingTransfer, setApprovingTransfer] = useState(null);
  const [transferSummary, setTransferSummary] = useState({ total: 0, accepted: 0, rejected: 0 });
  const [inlineSearch, setInlineSearch] = useState('');
  const [paymentFilters, setPaymentFilters] = useState({ fromDate: '', toDate: '', status: '', mode: '' });
  const [exportingPayments, setExportingPayments] = useState(false);
  const [academicYears, setAcademicYears] = useState([]), [academicYearId, setAcademicYearId] = useState('');
  const [yearsLoaded, setYearsLoaded] = useState(false);
  // Initialize transfer status filter from URL search param (e.g. navigating from dashboard)
  const [transferStatusFilter, setTransferStatusFilter] = useState(() =>
    type === 'transfers' ? (searchParams.get('status') || '') : ''
  );
  const { addToast } = useToast();

  useEffect(() => {
    let active = true;
    api.get('/admin/academic-years').then((response) => {
      if (!active) return;
      const years = response.data || [];
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

  useEffect(() => {
    const requiresYears = ['passes', 'transfers', 'payments'].includes(type);
    if (requiresYears && !yearsLoaded) return;

    let active = true;
    setLoading(true);
    const params = new URLSearchParams({ unpaged: 'true' });
    if (type === 'passes' && appliedSearch) params.set('search', appliedSearch);
    if (requiresYears && academicYearId) params.set('academicYearId', academicYearId);
    if (type === 'payments') {
      if (paymentFilters.fromDate) params.set('fromDate', paymentFilters.fromDate);
      if (paymentFilters.toDate) params.set('toDate', paymentFilters.toDate);
      if (paymentFilters.status) params.set('status', paymentFilters.status);
      if (paymentFilters.mode) params.set('mode', paymentFilters.mode);
    }
    params.set('_ts', String(Date.now()));
    api.get(`/admin/${type}?${params}`, { headers: { 'Cache-Control': 'no-cache' } }).then((r) => {
      if (!active) return;
      const list = Array.isArray(r.data) ? r.data : (r.data?.content || []);
      setRows(list);
      setTotal(Array.isArray(r.data) ? r.data.length : (r.data?.totalElements ?? list.length));
      setLastUpdated(new Date());
      setLoading(false);
    }).catch((e) => {
      if (!active) return;
      addToast(e.response?.data?.message || `Failed to load ${type}`, 'error');
      setLoading(false);
    });
    return () => { active = false; };
  }, [type, appliedSearch, refreshKey, academicYearId, yearsLoaded, paymentFilters]);

  useEffect(() => {
    setSearch('');
    setAppliedSearch('');
    setInlineSearch('');
    setPaymentFilters({ fromDate: '', toDate: '', status: '', mode: '' });
    // When type changes, re-read status from URL (in case we navigated from dashboard)
    setTransferStatusFilter(type === 'transfers' ? (searchParams.get('status') || '') : '');
  }, [type]);
  useEffect(() => { if (type === 'transfers') api.get('/admin/transfers/summary').then((response) => setTransferSummary(response.data || {})).catch(() => {}); }, [type, refreshKey]);
  // Auto-refresh on tab focus is intentionally removed: for 6000+ users it causes
  // excessive DB load every time a user switches browser tabs.
  // Use the manual Refresh button instead.

  const filteredRows = useMemo(() => {
    if (type === 'passes') return rows;
    const term = inlineSearch.trim().toLowerCase();
    return rows.filter((r) => {
      const haystack = Object.values(r)
        .filter(v => v !== null && v !== undefined && typeof v !== 'object')
        .map(String).join(' ').toLowerCase();
      const matchesSearch = !term || haystack.includes(term);
      const matchesStatus = type !== 'transfers' || !transferStatusFilter || (r.status || '') === transferStatusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [rows, inlineSearch, transferStatusFilter, type]);

  const closePass = () => { if (photoUrl) URL.revokeObjectURL(photoUrl); setPhotoUrl(''); setSelectedPass(null); };
  const viewPass = async (row) => {
    try {
      const response = await api.get(`/admin/passes/${row.id}`);
      let nextPhotoUrl = '';
      if (response.data.photoAvailable) {
        const photo = await api.get(`/admin/passes/${row.id}/photo`, { responseType: 'blob' });
        nextPhotoUrl = URL.createObjectURL(photo.data);
      }
      setPhotoUrl(nextPhotoUrl); setSelectedPass(response.data);
    } catch (e) { addToast(e.response?.data?.message || 'Unable to open this bus pass', 'error'); }
  };
  const downloadPass = async () => {
    setDownloading(true);
    try {
      const response = await api.get(`/admin/passes/${selectedPass.id}/download`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data), link = document.createElement('a');
      link.href = url; link.download = `bus-pass-${selectedPass.rollNumber}.pdf`; link.click(); URL.revokeObjectURL(url);
    } catch (e) { addToast(e.response?.data?.message || 'Unable to download this bus pass', 'error'); }
    finally { setDownloading(false); }
  };
  const approveTransfer = async (row) => {
    if (!window.confirm(`Approve the transfer for ${row.studentRollNumber} from ${row.currentBusNumber} to ${row.requestedBusNumber}? This immediately changes the active allocation.`)) return;
    setApprovingTransfer(row.id);
    try { await api.post(`/admin/transfers/${row.id}/approve`); addToast('Transfer approved and allocation updated', 'success'); setRefreshKey((value) => value + 1); }
    catch (error) { addToast(error.response?.data?.message || 'Transfer approval failed', 'error'); }
    finally { setApprovingTransfer(null); }
  };
  const exportPayments = async (format) => {
    if (!filteredRows.length || exportingPayments) return;
    setExportingPayments(true);
    try {
      if (format === 'pdf') await downloadPaymentsPdf(filteredRows, paymentFilters);
      else downloadPaymentsCsv(filteredRows);
      addToast(`${filteredRows.length} filtered payment record(s) downloaded`, 'success');
    } catch (error) { addToast(error.message || 'Unable to download payment records', 'error'); }
    finally { setExportingPayments(false); }
  };

  const configs = {
    transfers: [
      { key: 'studentName', label: 'Student', render: (r) => r.studentName || '—' },
      { key: 'studentRollNumber', label: 'Roll Number', render: (r) => r.studentRollNumber || '—' },
      { key: 'currentBusNumber', label: 'From', render: (r) => <div><div className="font-medium">{r.currentBusNumber || '—'}</div><div className="text-xs text-gray-500">{r.currentBoardingPoint || '—'}</div></div> },
      { key: 'requestedBusNumber', label: 'To', render: (r) => <div><div className="font-medium">{r.requestedBusNumber || '—'}</div><div className="text-xs text-gray-500">{r.requestedBoardingPoint || '—'}</div><div className={`text-xs ${(r.requestedAvailableSeats||0)>0?'text-green-700':'text-red-600'}`}>{r.requestedAvailableSeats ?? 0} vacant</div></div> },
      { key: 'academicYear', label: 'Academic Year', render: (r) => r.academicYear || '—' },
      { key: 'reason', label: 'Reason', render: (r) => r.reason || '—' },
      { key: 'oldInchargeName', label: 'Old In-Charge', render: (r) => r.oldInchargeName || '—' },
      { key: 'newInchargeName', label: 'New In-Charge', render: (r) => r.newInchargeName || '—' },
      { key: 'status', label: 'State', render: (r) => <StatusBadge status={r.status} /> },
      {
        key: 'remarks',
        label: 'Decision Remarks',
        render: (r) => {
          const isRejected = (r.status || '').includes('REJECTED');
          if (isRejected || r.remarks) {
            return r.remarks ? (
              <span className={`text-xs px-2 py-0.5 rounded border ${isRejected ? 'text-red-700 bg-red-50 border-red-200' : 'text-gray-700 bg-gray-50 border-gray-200'}`}>
                {r.remarks}
              </span>
            ) : (
              <span className="text-xs text-gray-400 italic">No remarks</span>
            );
          }
          return '—';
        }
      },
      {
        key: 'actions',
        label: 'Action',
        render: (r) => ['TRANSFER_REQUESTED', 'OLD_INCHARGE_APPROVED'].includes(r.status) ? (
          <button
            type="button"
            disabled={approvingTransfer === r.id || (r.requestedAvailableSeats || 0) < 1}
            onClick={() => approveTransfer(r)}
            className="rounded bg-green-100 px-2 py-1 text-xs font-medium text-green-800 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {approvingTransfer === r.id ? 'Approving...' : (r.requestedAvailableSeats || 0) < 1 ? 'No Vacancy' : 'Approve Directly'}
          </button>
        ) : '—'
      }
    ],
    payments: [
      { key: 'rollNumber', label: 'Roll Number' },
      { key: 'studentName', label: 'Student Details', render: (r) => <div><div className="font-medium">{r.studentName || '—'}</div><div className="text-xs text-gray-500">{r.branch || '—'} / {r.year || '—'} / {r.semester || '—'}</div><div className="text-xs text-gray-500">{r.phoneNumber || '—'} · {r.email || '—'}</div></div> },
      { key: 'academicYear', label: 'Academic Year', render: (r) => r.academicYear || '—' },
      { key: 'paidAt', label: 'Payment Date', render: (r) => r.paidAt || r.createdAt ? new Date(r.paidAt || r.createdAt).toLocaleString('en-IN') : '—' },
      { key: 'amount', label: 'Amount', render: (r) => formatINR(r.amount) },
      { key: 'transactionId', label: 'Transaction ID', render: (r) => r.transactionId || '—' },
      { key: 'gatewayName', label: 'Mode', render: (r) => r.gatewayName === 'ADMIN_MANUAL' ? 'Admin Manual' : (r.gatewayName || '—') },
      { key: 'totalFee', label: 'Total Fee', render: (r) => formatINR(r.totalFee) },
      { key: 'paidTillNow', label: 'Paid Till Now', render: (r) => formatINR(r.paidTillNow) },
      { key: 'pendingAmount', label: 'Pending', render: (r) => formatINR(r.pendingAmount) },
      { key: 'status', label: 'Status', render: (r) => <StatusBadge status={r.status} /> }
    ],
    passes: [{ key: 'studentName', label: 'Student' }, { key: 'rollNumber', label: 'Roll Number' }, { key: 'passNumber', label: 'Pass ID' }, { key: 'busNumber', label: 'Bus' }, { key: 'boardingPoint', label: 'Boarding Point' }, { key: 'validUntil', label: 'Valid Until' }, { key: 'status', label: 'Status', render: (r) => <StatusBadge status={r.status} /> }, { key: 'actions', label: 'Action', render: (r) => <button type="button" className="text-sm font-medium text-blue-700 hover:underline" onClick={() => viewPass(r)}>View</button> }],
    notifications: [{ key: 'title', label: 'Title' }, { key: 'recipientRollNumber', label: 'Recipient' }, { key: 'message', label: 'Message' }, { key: 'createdAt', label: 'Date', render: (r) => r.createdAt ? new Date(r.createdAt).toLocaleString() : '—' }]
  };
  const transferCounts = transferSummary;
  return (
    <>
      <div className="card">
        {/* Header */}
        <div className="mb-5 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-2xl font-bold capitalize">{type}</h2>
            {type === 'passes' && <p className="text-sm text-gray-500">Passes generated: {total}</p>}
            {type === 'transfers' && <p className="text-sm text-gray-500">Total: {transferCounts.total || 0} · Accepted: {transferCounts.accepted || 0} · Rejected: {transferCounts.rejected || 0}</p>}
            {lastUpdated && <p className="text-xs text-gray-500">Updated {lastUpdated.toLocaleTimeString()}</p>}
          </div>
          <button type="button" className="btn-secondary inline-flex items-center justify-center gap-2" disabled={loading} onClick={() => setRefreshKey((value) => value + 1)}>
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} /> Refresh
          </button>
        </div>

        {/* Search + status filter for transfers */}
        {type === 'transfers' && (
          <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div className="relative sm:col-span-2">
              <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} aria-hidden="true" />
              <input type="text" className="input-field pl-9" placeholder="Search student, roll number, bus or reason..." value={inlineSearch} onChange={(e) => setInlineSearch(e.target.value)} aria-label="Search transfers" />
            </div>
            <select className="input-field" value={transferStatusFilter} onChange={(e) => { setTransferStatusFilter(e.target.value); if (e.target.value) setSearchParams({ status: e.target.value }); else setSearchParams({}); }} aria-label="Filter by transfer status">
              <option value="">All Statuses</option>
              <option value="TRANSFER_REQUESTED">Transfer Requested</option>
              <option value="OLD_INCHARGE_APPROVED">Old Incharge Approved</option>
              <option value="OLD_INCHARGE_REJECTED">Old Incharge Rejected</option>
              <option value="NEW_INCHARGE_APPROVED">New Incharge Approved</option>
              <option value="NEW_INCHARGE_REJECTED">New Incharge Rejected</option>
              <option value="TRANSFER_COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
        )}

        {(type === 'passes' || type === 'transfers' || type === 'payments') && (
          <div className="mb-4">
            <label className="block text-xs font-medium text-gray-600 mb-1">Academic year</label>
            <select className="input-field max-w-sm" value={academicYearId} onChange={(e) => setAcademicYearId(e.target.value)}>
              <option value="">All academic years</option>
              {academicYears.map((year) => <option key={year.id} value={year.id}>{year.yearName}{year.active ? ' (Active)' : ''}</option>)}
            </select>
          </div>
        )}

        {/* Search for payments */}
        {type === 'payments' && (
          <div className="mb-4 space-y-3">
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
              <label className="text-xs font-medium text-gray-600">From date<input type="date" className="input-field mt-1" value={paymentFilters.fromDate} max={paymentFilters.toDate || undefined} onChange={(e) => setPaymentFilters((current) => ({ ...current, fromDate: e.target.value }))}/></label>
              <label className="text-xs font-medium text-gray-600">To date<input type="date" className="input-field mt-1" value={paymentFilters.toDate} min={paymentFilters.fromDate || undefined} onChange={(e) => setPaymentFilters((current) => ({ ...current, toDate: e.target.value }))}/></label>
              <label className="text-xs font-medium text-gray-600">Payment status<select className="input-field mt-1" value={paymentFilters.status} onChange={(e) => setPaymentFilters((current) => ({ ...current, status: e.target.value }))}><option value="">All statuses</option>{['SUCCESS','INITIATED','PENDING','FAILED','CANCELLED','REFUNDED'].map((status) => <option key={status} value={status}>{status.replaceAll('_',' ')}</option>)}</select></label>
              <label className="text-xs font-medium text-gray-600">Payment mode<select className="input-field mt-1" value={paymentFilters.mode} onChange={(e) => setPaymentFilters((current) => ({ ...current, mode: e.target.value }))}><option value="">All modes</option><option value="RAZORPAY">Razorpay</option><option value="ADMIN_MANUAL">Admin Manual</option></select></label>
            </div>
            <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
              <div className="relative min-w-0 flex-1">
                <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} aria-hidden="true" />
                <input type="text" className="input-field pl-9" placeholder="Search all displayed fields: student, roll number, IDs, mode or status..." value={inlineSearch} onChange={(e) => setInlineSearch(e.target.value)} aria-label="Search payments" />
              </div>
              <div className="flex flex-wrap gap-2">
                <button type="button" className="btn-secondary" onClick={() => { setInlineSearch(''); setPaymentFilters({ fromDate: '', toDate: '', status: '', mode: '' }); }}>Clear filters</button>
                <button type="button" className="btn-secondary inline-flex items-center gap-2" disabled={!filteredRows.length || exportingPayments} onClick={() => exportPayments('csv')}><FileSpreadsheet size={16}/> Download Excel</button>
                <button type="button" className="btn-primary inline-flex items-center gap-2" disabled={!filteredRows.length || exportingPayments} onClick={() => exportPayments('pdf')}><Download size={16}/> Download PDF</button>
              </div>
            </div>
          </div>
        )}

        {/* Search for notifications */}
        {type === 'notifications' && (
          <div className="mb-4 relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} aria-hidden="true" />
            <input type="text" className="input-field pl-9" placeholder="Search by title, recipient or message..." value={inlineSearch} onChange={(e) => setInlineSearch(e.target.value)} aria-label="Search notifications" />
          </div>
        )}

        {/* Server-side search for passes */}
        {type === 'passes' && (
          <form className="mb-5 flex flex-col gap-2 sm:flex-row" onSubmit={(event) => { event.preventDefault(); setAppliedSearch(search.trim()); }}>
            <input className="input-field sm:max-w-md" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search pass ID, student, roll number or bus" aria-label="Search bus passes" />
            <button className="btn-primary" type="submit">Search</button>
            {appliedSearch && <button className="btn-secondary" type="button" onClick={() => { setSearch(''); setAppliedSearch(''); }}>Clear</button>}
          </form>
        )}

        {!loading && type !== 'passes' && (
          <p className="mb-3 text-xs text-gray-500">
            {filteredRows.length} of {rows.length} record(s){inlineSearch || transferStatusFilter ? ' matching filters' : ''}
          </p>
        )}

        <DataTable columns={configs[type]} data={type === 'passes' ? rows : filteredRows} loading={loading} searchable={false} />
      </div>
      <PassViewerModal pass={selectedPass} photoUrl={photoUrl} onClose={closePass} onDownload={downloadPass} downloading={downloading} />
    </>
  );
}
