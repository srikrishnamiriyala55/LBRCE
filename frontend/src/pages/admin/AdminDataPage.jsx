import React, { useEffect, useState } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import StatusBadge from '../../components/common/StatusBadge';
import PassViewerModal from '../../components/passes/PassViewerModal';
import { useToast } from '../../components/common/Toast';
import { RefreshCw } from 'lucide-react';

export default function AdminDataPage({ type }) {
  const [rows, setRows] = useState([]), [page, setPage] = useState(0), [pages, setPages] = useState(1), [loading, setLoading] = useState(true);
  const [selectedPass, setSelectedPass] = useState(null), [photoUrl, setPhotoUrl] = useState(''), [downloading, setDownloading] = useState(false);
  const [search, setSearch] = useState(''), [appliedSearch, setAppliedSearch] = useState('');
  const [refreshKey, setRefreshKey] = useState(0), [lastUpdated, setLastUpdated] = useState(null);
  const { addToast } = useToast();

  useEffect(() => {
    setLoading(true);
    const params = new URLSearchParams({ page: String(page), size: '20' });
    if (type === 'passes' && appliedSearch) params.set('search', appliedSearch);
    params.set('_ts', String(Date.now()));
    api.get(`/admin/${type}?${params}`, { headers: { 'Cache-Control': 'no-cache' } }).then((r) => { setRows(r.data.content || []); setPages(r.data.totalPages || 1); setLastUpdated(new Date()); })
      .catch((e) => addToast(e.response?.data?.message || `Failed to load ${type}`, 'error')).finally(() => setLoading(false));
  }, [type, page, appliedSearch, refreshKey]);

  useEffect(() => { setPage(0); setSearch(''); setAppliedSearch(''); }, [type]);
  useEffect(() => {
    const refreshVisiblePage = () => { if (document.visibilityState === 'visible') setRefreshKey((value) => value + 1); };
    window.addEventListener('focus', refreshVisiblePage);
    document.addEventListener('visibilitychange', refreshVisiblePage);
    return () => { window.removeEventListener('focus', refreshVisiblePage); document.removeEventListener('visibilitychange', refreshVisiblePage); };
  }, []);

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

  const configs = {
    transfers: [{ key: 'studentName', label: 'Student', render: (r) => r.studentName || '—' }, { key: 'studentRollNumber', label: 'Roll Number', render: (r) => r.studentRollNumber || '—' }, { key: 'currentBusNumber', label: 'From', render: (r) => <div><div className="font-medium">{r.currentBusNumber || '—'}</div><div className="text-xs text-gray-500">{r.currentBoardingPoint || '—'}</div></div> }, { key: 'requestedBusNumber', label: 'To', render: (r) => <div><div className="font-medium">{r.requestedBusNumber || '—'}</div><div className="text-xs text-gray-500">{r.requestedBoardingPoint || '—'}</div></div> }, { key: 'academicYear', label: 'Academic Year', render: (r) => r.academicYear || '—' }, { key: 'reason', label: 'Reason', render: (r) => r.reason || '—' }, { key: 'oldInchargeName', label: 'Old In-Charge', render: (r) => r.oldInchargeName || '—' }, { key: 'newInchargeName', label: 'New In-Charge', render: (r) => r.newInchargeName || '—' }, { key: 'status', label: 'State', render: (r) => <StatusBadge status={r.status} /> }],
    payments: [{ key: 'studentName', label: 'Student' }, { key: 'rollNumber', label: 'Roll Number' }, { key: 'orderId', label: 'Order ID' }, { key: 'amount', label: 'Amount', render: (r) => `₹${r.amount || 0}` }, { key: 'status', label: 'Verified Status', render: (r) => <StatusBadge status={r.status} /> }, { key: 'paidAt', label: 'Paid At' }],
    passes: [{ key: 'studentName', label: 'Student' }, { key: 'rollNumber', label: 'Roll Number' }, { key: 'passNumber', label: 'Pass ID' }, { key: 'busNumber', label: 'Bus' }, { key: 'boardingPoint', label: 'Boarding Point' }, { key: 'validUntil', label: 'Valid Until' }, { key: 'status', label: 'Status', render: (r) => <StatusBadge status={r.status} /> }, { key: 'actions', label: 'Action', render: (r) => <button type="button" className="text-sm font-medium text-blue-700 hover:underline" onClick={() => viewPass(r)}>View</button> }],
    notifications: [{ key: 'title', label: 'Title' }, { key: 'recipientRollNumber', label: 'Recipient' }, { key: 'message', label: 'Message' }, { key: 'createdAt', label: 'Date', render: (r) => r.createdAt ? new Date(r.createdAt).toLocaleString() : '—' }]
  };
  return <><div className="card"><div className="mb-5 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between"><div><h2 className="text-2xl font-bold capitalize">{type}</h2>{lastUpdated && <p className="text-xs text-gray-500">Updated {lastUpdated.toLocaleTimeString()}</p>}</div><button type="button" className="btn-secondary inline-flex items-center justify-center gap-2" disabled={loading} onClick={() => setRefreshKey((value) => value + 1)}><RefreshCw size={16} className={loading?'animate-spin':''}/>Refresh</button></div>{type === 'passes' && <form className="mb-5 flex flex-col gap-2 sm:flex-row" onSubmit={(event) => { event.preventDefault(); setPage(0); setAppliedSearch(search.trim()); }}><input className="input-field sm:max-w-md" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search pass ID, student, roll number or bus" aria-label="Search bus passes"/><button className="btn-primary" type="submit">Search</button>{appliedSearch && <button className="btn-secondary" type="button" onClick={() => { setSearch(''); setAppliedSearch(''); setPage(0); }}>Clear</button>}</form>}<DataTable columns={configs[type]} data={rows} loading={loading} /><Pagination page={page} totalPages={pages} onPageChange={setPage} /></div><PassViewerModal pass={selectedPass} photoUrl={photoUrl} onClose={closePass} onDownload={downloadPass} downloading={downloading} /></>;
}
