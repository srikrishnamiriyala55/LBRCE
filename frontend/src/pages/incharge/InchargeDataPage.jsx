import React, { useEffect, useMemo, useState } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import PassViewerModal from '../../components/passes/PassViewerModal';
import { useToast } from '../../components/common/Toast';
import { formatINR } from '../../utils/currency';

const feeFilters = { search: '', academicYear: '', payment: '', passEligible: '' };
const paymentState = (fee) => {
  const paid = Number(fee.paidAmount || 0), total = Number(fee.totalAmount || 0);
  if (total > 0 && paid >= total) return 'PAID';
  if (paid > 0) return 'PARTIALLY_PAID';
  return 'PENDING';
};

export default function InchargeDataPage({ type }) {
  const [data, setData] = useState([]), [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState(feeFilters);
  const [passSearch, setPassSearch] = useState('');
  const [passAcademicYear, setPassAcademicYear] = useState('');
  const [academicYears, setAcademicYears] = useState([]);
  const [selectedPass, setSelectedPass] = useState(null), [photoUrl, setPhotoUrl] = useState(''), [downloading, setDownloading] = useState(false);
  const { addToast } = useToast();

  useEffect(() => {
    api.get('/incharge/academic-years').then((r) => {
      const years = r.data || [];
      setAcademicYears(years);
      const activeYear = years.find((year) => year.active);
      if (activeYear) {
        setFilters((current) => ({ ...current, academicYear: activeYear.yearName }));
        setPassAcademicYear(activeYear.yearName);
      }
    }).catch(() => {});
  }, []);

  const load = () => { setLoading(true); api.get(`/incharge/${type}`).then((response) => setData(Array.isArray(response.data) ? response.data : (response.data.content || []))).catch((error) => addToast(error.response?.data?.message || `Failed to load ${type}`, 'error')).finally(() => setLoading(false)); };
  useEffect(() => {
    setFilters((current) => ({ ...feeFilters, academicYear: current.academicYear }));
    setPassSearch('');
    load();
  }, [type]);
  const markRead = async (row) => { if (!row.read) { await api.put(`/incharge/notifications/${row.id}/read`); load(); } };
  const closePass = () => { if (photoUrl) URL.revokeObjectURL(photoUrl); setPhotoUrl(''); setSelectedPass(null); };
  const viewPass = async (row) => { try { const response = await api.get(`/incharge/passes/${row.id}`); let nextPhotoUrl = ''; if (response.data.photoAvailable) { const photo = await api.get(`/incharge/passes/${row.id}/photo`, { responseType: 'blob' }); nextPhotoUrl = URL.createObjectURL(photo.data); } setPhotoUrl(nextPhotoUrl); setSelectedPass(response.data); } catch (error) { addToast(error.response?.data?.message || 'Unable to open this bus pass', 'error'); } };
  const downloadPass = async () => { setDownloading(true); try { const response = await api.get(`/incharge/passes/${selectedPass.id}/download`, { responseType: 'blob' }); const url = URL.createObjectURL(response.data), link = document.createElement('a'); link.href = url; link.download = `bus-pass-${selectedPass.rollNumber}.pdf`; link.click(); URL.revokeObjectURL(url); } catch (error) { addToast(error.response?.data?.message || 'Unable to download this bus pass', 'error'); } finally { setDownloading(false); } };
  const fees = useMemo(() => data.filter((fee) => { const text = [fee.studentName, fee.rollNumber, fee.academicYear, fee.totalAmount, fee.paidAmount, fee.remainingAmount, fee.status].join(' ').toLowerCase(); return (!filters.search || text.includes(filters.search.toLowerCase())) && (!filters.academicYear || fee.academicYear === filters.academicYear) && (!filters.payment || paymentState(fee) === filters.payment) && (!filters.passEligible || String(Boolean(fee.passEligible)) === filters.passEligible); }), [data, filters]);
  const filteredPasses = useMemo(() => {
    const term = passSearch.trim().toLowerCase();
    return data.filter((p) => {
      const matchesSearch = !term ||
        (p.studentName || '').toLowerCase().includes(term) ||
        (p.rollNumber || '').toLowerCase().includes(term) ||
        (p.passNumber || '').toLowerCase().includes(term) ||
        (p.busNumber || '').toLowerCase().includes(term) ||
        (p.boardingPoint || '').toLowerCase().includes(term) ||
        (p.status || '').toLowerCase().includes(term);
      const matchesYear = !passAcademicYear || p.academicYear === passAcademicYear;
      return matchesSearch && matchesYear;
    });
  }, [data, passSearch, passAcademicYear]);
  const filteredPayments = useMemo(() => {
    const term = passSearch.trim().toLowerCase();
    return data.filter(payment => {
      const matchesText = !term || [payment.studentName, payment.rollNumber, payment.orderId, payment.amount, payment.status]
        .filter(value => value !== null && value !== undefined).join(' ').toLowerCase().includes(term);
      return matchesText && (!passAcademicYear || payment.academicYear === passAcademicYear);
    });
  }, [data, passSearch, passAcademicYear]);
  const feeCounts = useMemo(() => fees.reduce((counts, fee) => ({ ...counts, [paymentState(fee)]: counts[paymentState(fee)] + 1 }), { PAID: 0, PARTIALLY_PAID: 0, PENDING: 0 }), [fees]);
  const configs = { fees: [{ key: 'studentName', label: 'Student' }, { key: 'rollNumber', label: 'Roll Number' }, { key: 'academicYear', label: 'Academic Year' }, { key: 'totalAmount', label: 'Required Fee', render: row => formatINR(row.totalAmount) }, { key: 'paidAmount', label: 'Paid', render: row => formatINR(row.paidAmount) }, { key: 'remainingAmount', label: 'Remaining', render: row => formatINR(row.remainingAmount) }, { key: 'payment', label: 'Payment', render: row => <StatusBadge status={paymentState(row)} /> }, { key: 'passEligible', label: 'Pass Eligible', render: row => row.passEligible ? 'Yes' : 'No' }], payments: [{ key: 'createdAt', label: 'Date', render: row => new Date(row.paidAt || row.createdAt).toLocaleString() }, { key: 'studentName', label: 'Student' }, { key: 'rollNumber', label: 'Roll Number' }, { key: 'academicYear', label: 'Academic Year' }, { key: 'orderId', label: 'Receipt / Order ID', render: row => row.orderId || '—' }, { key: 'amount', label: 'Amount', render: row => formatINR(row.amount) }, { key: 'status', label: 'Status', render: row => <StatusBadge status={row.status} /> }], passes: [{ key: 'studentName', label: 'Student' }, { key: 'rollNumber', label: 'Roll Number' }, { key: 'academicYear', label: 'Academic Year', render: r => r.academicYear || '—' }, { key: 'passNumber', label: 'Pass ID' }, { key: 'busNumber', label: 'Bus' }, { key: 'boardingPoint', label: 'Boarding Point' }, { key: 'validUntil', label: 'Valid Until' }, { key: 'status', label: 'Status', render: row => <StatusBadge status={row.status} /> }, { key: 'actions', label: 'Action', render: row => <button type="button" className="text-sm font-medium text-blue-700 hover:underline" onClick={() => viewPass(row)}>View</button> }], notifications: [{ key: 'title', label: 'Title' }, { key: 'message', label: 'Message' }, { key: 'createdAt', label: 'Date', render: row => new Date(row.createdAt).toLocaleString() }, { key: 'read', label: 'Action', render: row => row.read ? 'Read' : <button className="text-blue-700 text-sm" onClick={() => markRead(row)}>Mark read</button> }] };
  return <><div className="card space-y-5"><div className="flex items-center justify-between"><h2 className="text-xl font-bold capitalize">{type}</h2><button type="button" className="btn-secondary" onClick={load}>Refresh</button></div>{type === 'fees' && <><div className="grid grid-cols-1 gap-3 sm:grid-cols-3"><div className="rounded-lg border p-3"><p className="text-xs text-gray-500">Paid</p><p className="text-xl font-bold text-green-700">{feeCounts.PAID}</p></div><div className="rounded-lg border p-3"><p className="text-xs text-gray-500">Partially paid</p><p className="text-xl font-bold text-amber-700">{feeCounts.PARTIALLY_PAID}</p></div><div className="rounded-lg border p-3"><p className="text-xs text-gray-500">Pending</p><p className="text-xl font-bold text-red-700">{feeCounts.PENDING}</p></div></div><div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4"><input className="input-field" placeholder="Search any fee attribute" value={filters.search} onChange={event => setFilters({ ...filters, search: event.target.value })}/><select className="input-field" value={filters.academicYear} onChange={event => setFilters({ ...filters, academicYear: event.target.value })}><option value="">All academic years</option>{(academicYears.length ? academicYears.map(y => y.yearName) : [...new Set(data.map(fee => fee.academicYear).filter(Boolean))]).map(year => <option key={year} value={year}>{year}</option>)}</select><select className="input-field" value={filters.payment} onChange={event => setFilters({ ...filters, payment: event.target.value })}><option value="">All payment states</option><option value="PAID">Paid</option><option value="PARTIALLY_PAID">Partially paid</option><option value="PENDING">Pending</option></select><select className="input-field" value={filters.passEligible} onChange={event => setFilters({ ...filters, passEligible: event.target.value })}><option value="">All pass eligibility</option><option value="true">Pass eligible</option><option value="false">Not pass eligible</option></select></div></>}{['passes','payments'].includes(type) && <div className="flex flex-col gap-3 sm:flex-row"><input className="input-field flex-1" placeholder={type === 'payments' ? 'Search student, roll number, receipt, amount or status...' : 'Search student, roll, pass ID, bus or boarding point...'} value={passSearch} onChange={e => setPassSearch(e.target.value)} /><select className="input-field sm:w-56" value={passAcademicYear} onChange={e => setPassAcademicYear(e.target.value)}><option value="">All academic years</option>{(academicYears.length ? academicYears.map(y => y.yearName) : [...new Set(data.map(p => p.academicYear).filter(Boolean))]).map(year => <option key={year} value={year}>{year}</option>)}</select></div>}<DataTable columns={configs[type]} data={type === 'fees' ? fees : type === 'passes' ? filteredPasses : type === 'payments' ? filteredPayments : data} loading={loading} searchable={false}/></div><PassViewerModal pass={selectedPass} photoUrl={photoUrl} onClose={closePass} onDownload={downloadPass} downloading={downloading}/></>;
}
