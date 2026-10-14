import React, { useEffect, useMemo, useState } from 'react';
import { CreditCard, Download, FileSpreadsheet } from 'lucide-react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import PassViewerModal from '../../components/passes/PassViewerModal';
import Modal from '../../components/common/Modal';
import { useToast } from '../../components/common/Toast';
import { formatINR } from '../../utils/currency';
import { apiMessage, openRazorpayCheckout } from '../../utils/razorpay';
import { downloadPaymentsCsv, downloadPaymentsPdf } from '../../utils/paymentRecordsExport';

const feeFilters = { search: '', academicYear: '', payment: '', passEligible: '' };
const paymentState = fee => {
  const paid = Number(fee.paidAmount || 0), total = Number(fee.totalAmount || 0);
  if (total > 0 && paid >= total) return 'PAID';
  if (paid > 0) return 'PARTIALLY_PAID';
  return 'PENDING';
};

export default function InchargeDataPage({ type }) {
  const [data, setData] = useState([]), [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState(feeFilters);
  const [passSearch, setPassSearch] = useState(''), [passAcademicYear, setPassAcademicYear] = useState('');
  const [paymentFilters, setPaymentFilters] = useState({ fromDate: '', toDate: '', status: '', mode: '' });
  const [exportingPayments, setExportingPayments] = useState(false);
  const [academicYears, setAcademicYears] = useState([]);
  const [selectedPass, setSelectedPass] = useState(null), [photoUrl, setPhotoUrl] = useState(''), [downloading, setDownloading] = useState(false);
  const [onlineFee, setOnlineFee] = useState(null), [onlineAmount, setOnlineAmount] = useState(''), [onlineProcessing, setOnlineProcessing] = useState(false);
  const { addToast } = useToast();

  useEffect(() => {
    api.get('/incharge/academic-years').then(({ data: years = [] }) => {
      setAcademicYears(years);
      const activeYear = years.find(year => year.active);
      if (activeYear) {
        setFilters(current => ({ ...current, academicYear: activeYear.yearName }));
        setPassAcademicYear(activeYear.yearName);
      }
    }).catch(() => {});
  }, []);

  const load = () => {
    setLoading(true);
    api.get(`/incharge/${type}`)
      .then(response => setData(Array.isArray(response.data) ? response.data : (response.data.content || [])))
      .catch(error => addToast(error.response?.data?.message || `Failed to load ${type}`, 'error'))
      .finally(() => setLoading(false));
  };
  useEffect(() => { setFilters(current => ({ ...feeFilters, academicYear: current.academicYear })); setPassSearch(''); load(); }, [type]);

  const markRead = async row => { if (!row.read) { await api.put(`/incharge/notifications/${row.id}/read`); load(); } };
  const closePass = () => { if (photoUrl) URL.revokeObjectURL(photoUrl); setPhotoUrl(''); setSelectedPass(null); };
  const viewPass = async row => {
    try {
      const response = await api.get(`/incharge/passes/${row.id}`);
      let nextPhotoUrl = '';
      if (response.data.photoAvailable) {
        const photo = await api.get(`/incharge/passes/${row.id}/photo`, { responseType: 'blob' });
        nextPhotoUrl = URL.createObjectURL(photo.data);
      }
      setPhotoUrl(nextPhotoUrl); setSelectedPass(response.data);
    } catch (error) { addToast(error.response?.data?.message || 'Unable to open this bus pass', 'error'); }
  };
  const downloadPass = async () => {
    setDownloading(true);
    try {
      const response = await api.get(`/incharge/passes/${selectedPass.id}/download`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data), link = document.createElement('a');
      link.href = url; link.download = `bus-pass-${selectedPass.rollNumber}.pdf`; link.click(); URL.revokeObjectURL(url);
    } catch (error) { addToast(error.response?.data?.message || 'Unable to download this bus pass', 'error'); }
    finally { setDownloading(false); }
  };

  const closeOnlinePayment = () => { if (!onlineProcessing) { setOnlineFee(null); setOnlineAmount(''); } };
  const submitOnlinePayment = async event => {
    event.preventDefault();
    const amount = Number(onlineAmount);
    if (!onlineFee || !Number.isInteger(amount) || amount < 1 || amount > onlineFee.remainingAmount) {
      addToast(`Enter a whole-number amount between ₹1 and ${formatINR(onlineFee?.remainingAmount || 0)}`, 'error'); return;
    }
    setOnlineProcessing(true);
    try {
      const { data: order } = await api.post('/incharge/payments/order', { feeId: onlineFee.id, amount });
      await openRazorpayCheckout({
        order,
        onDismiss: () => setOnlineProcessing(false),
        onFailure: message => { addToast(message, 'error'); setOnlineProcessing(false); },
        onSuccess: async response => {
          try {
            await api.post('/incharge/payments/verify', { razorpayOrderId: response.razorpay_order_id, razorpayPaymentId: response.razorpay_payment_id, razorpaySignature: response.razorpay_signature });
            addToast('Online payment verified and the student fee was updated', 'success');
            setOnlineFee(null); setOnlineAmount(''); load();
          } catch (error) { addToast(apiMessage(error, 'Payment verification failed'), 'error'); }
          finally { setOnlineProcessing(false); }
        }
      });
    } catch (error) { addToast(apiMessage(error, 'Unable to start secure payment checkout'), 'error'); setOnlineProcessing(false); }
  };

  const fees = useMemo(() => data.filter(fee => {
    const text = [fee.studentName, fee.rollNumber, fee.academicYear, fee.totalAmount, fee.paidAmount, fee.remainingAmount, fee.status].join(' ').toLowerCase();
    return (!filters.search || text.includes(filters.search.toLowerCase())) && (!filters.academicYear || fee.academicYear === filters.academicYear)
      && (!filters.payment || paymentState(fee) === filters.payment) && (!filters.passEligible || String(Boolean(fee.passEligible)) === filters.passEligible);
  }), [data, filters]);
  const filteredPasses = useMemo(() => { const term = passSearch.trim().toLowerCase(); return data.filter(row => (!term || [row.studentName, row.rollNumber, row.passNumber, row.busNumber, row.boardingPoint, row.status].filter(Boolean).join(' ').toLowerCase().includes(term)) && (!passAcademicYear || row.academicYear === passAcademicYear)); }, [data, passSearch, passAcademicYear]);
  const filteredPayments = useMemo(() => {
    const term = passSearch.trim().toLowerCase();
    const from = paymentFilters.fromDate ? new Date(`${paymentFilters.fromDate}T00:00:00`) : null;
    const to = paymentFilters.toDate ? new Date(`${paymentFilters.toDate}T23:59:59.999`) : null;
    return data.filter(row => {
      const paidDate = row.paidAt || row.createdAt ? new Date(row.paidAt || row.createdAt) : null;
      const searchable = [row.studentName, row.rollNumber, row.email, row.phoneNumber, row.branch, row.year,
        row.semester, row.academicYear, row.orderId, row.transactionId, row.gatewayName, row.amount,
        row.totalFee, row.paidTillNow, row.pendingAmount, row.status].filter(value => value !== null && value !== undefined).join(' ').toLowerCase();
      return (!term || searchable.includes(term))
        && (!passAcademicYear || row.academicYear === passAcademicYear)
        && (!from || (paidDate && paidDate >= from))
        && (!to || (paidDate && paidDate <= to))
        && (!paymentFilters.status || row.status === paymentFilters.status)
        && (!paymentFilters.mode || row.gatewayName === paymentFilters.mode);
    });
  }, [data, passSearch, passAcademicYear, paymentFilters]);
  const feeCounts = useMemo(() => fees.reduce((counts, fee) => ({ ...counts, [paymentState(fee)]: counts[paymentState(fee)] + 1 }), { PAID: 0, PARTIALLY_PAID: 0, PENDING: 0 }), [fees]);
  const yearOptions = academicYears.length ? academicYears.map(year => year.yearName) : [...new Set(data.map(row => row.academicYear).filter(Boolean))];

  const configs = {
    fees: [
      { key: 'studentName', label: 'Student' }, { key: 'rollNumber', label: 'Roll Number' }, { key: 'academicYear', label: 'Academic Year' },
      { key: 'totalAmount', label: 'Required Fee', render: row => formatINR(row.totalAmount) }, { key: 'paidAmount', label: 'Paid', render: row => formatINR(row.paidAmount) },
      { key: 'remainingAmount', label: 'Remaining', render: row => formatINR(row.remainingAmount) }, { key: 'payment', label: 'Payment', render: row => <StatusBadge status={paymentState(row)} /> },
      { key: 'passEligible', label: 'Pass Eligible', render: row => row.passEligible ? 'Yes' : 'No' },
      { key: 'actions', label: 'Action', render: row => row.remainingAmount > 0 ? <button type="button" className="text-sm font-medium text-blue-700 hover:underline" onClick={() => { setOnlineFee(row); setOnlineAmount(String(row.remainingAmount)); }}>Pay Online</button> : <span className="text-sm text-gray-400">Fully paid</span> }
    ],
    payments: [
      { key: 'rollNumber', label: 'Roll Number' },
      { key: 'studentName', label: 'Student Details', render: row => <div><div className="font-medium">{row.studentName || '—'}</div><div className="text-xs text-gray-500">{row.branch || '—'} / {row.year || '—'} / {row.semester || '—'}</div><div className="text-xs text-gray-500">{row.phoneNumber || '—'} · {row.email || '—'}</div></div> },
      { key: 'academicYear', label: 'Academic Year' },
      { key: 'createdAt', label: 'Payment Date', render: row => row.paidAt || row.createdAt ? new Date(row.paidAt || row.createdAt).toLocaleString('en-IN') : '—' },
      { key: 'amount', label: 'Transaction Amount', render: row => formatINR(row.amount) },
      { key: 'transactionId', label: 'Transaction ID', render: row => row.transactionId || '—' },
      { key: 'gatewayName', label: 'Mode', render: row => row.gatewayName === 'ADMIN_MANUAL' ? 'Admin Manual' : (row.gatewayName || '—') },
      { key: 'totalFee', label: 'Total Fee', render: row => formatINR(row.totalFee) },
      { key: 'paidTillNow', label: 'Paid Till Now', render: row => formatINR(row.paidTillNow) },
      { key: 'pendingAmount', label: 'Pending', render: row => formatINR(row.pendingAmount) },
      { key: 'status', label: 'Status', render: row => <StatusBadge status={row.status} /> }
    ],
    passes: [
      { key: 'studentName', label: 'Student' }, { key: 'rollNumber', label: 'Roll Number' }, { key: 'academicYear', label: 'Academic Year', render: row => row.academicYear || '—' },
      { key: 'passNumber', label: 'Pass ID' }, { key: 'busNumber', label: 'Bus' }, { key: 'boardingPoint', label: 'Boarding Point' }, { key: 'validUntil', label: 'Valid Until' },
      { key: 'status', label: 'Status', render: row => <StatusBadge status={row.status} /> }, { key: 'actions', label: 'Action', render: row => <button type="button" className="text-sm font-medium text-blue-700 hover:underline" onClick={() => viewPass(row)}>View</button> }
    ],
    notifications: [
      { key: 'title', label: 'Title' }, { key: 'message', label: 'Message' }, { key: 'createdAt', label: 'Date', render: row => new Date(row.createdAt).toLocaleString() },
      { key: 'read', label: 'Action', render: row => row.read ? 'Read' : <button className="text-sm text-blue-700" onClick={() => markRead(row)}>Mark read</button> }
    ]
  };

  const displayedData = type === 'fees' ? fees : type === 'passes' ? filteredPasses : type === 'payments' ? filteredPayments : data;
  return <>
    <div className="card space-y-5">
      <div className="flex items-center justify-between"><h2 className="text-xl font-bold capitalize">{type}</h2><button type="button" className="btn-secondary" onClick={load}>Refresh</button></div>
      {type === 'fees' && <><div className="grid grid-cols-1 gap-3 sm:grid-cols-3"><div className="rounded-lg border p-3"><p className="text-xs text-gray-500">Paid</p><p className="text-xl font-bold text-green-700">{feeCounts.PAID}</p></div><div className="rounded-lg border p-3"><p className="text-xs text-gray-500">Partially paid</p><p className="text-xl font-bold text-amber-700">{feeCounts.PARTIALLY_PAID}</p></div><div className="rounded-lg border p-3"><p className="text-xs text-gray-500">Pending</p><p className="text-xl font-bold text-red-700">{feeCounts.PENDING}</p></div></div><div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4"><input className="input-field" placeholder="Search any fee attribute" value={filters.search} onChange={event => setFilters({ ...filters, search: event.target.value })}/><select className="input-field" value={filters.academicYear} onChange={event => setFilters({ ...filters, academicYear: event.target.value })}><option value="">All academic years</option>{yearOptions.map(year => <option key={year} value={year}>{year}</option>)}</select><select className="input-field" value={filters.payment} onChange={event => setFilters({ ...filters, payment: event.target.value })}><option value="">All payment states</option><option value="PAID">Paid</option><option value="PARTIALLY_PAID">Partially paid</option><option value="PENDING">Pending</option></select><select className="input-field" value={filters.passEligible} onChange={event => setFilters({ ...filters, passEligible: event.target.value })}><option value="">All pass eligibility</option><option value="true">Pass eligible</option><option value="false">Not pass eligible</option></select></div></>}
      {type === 'passes' && <div className="flex flex-col gap-3 sm:flex-row"><input className="input-field flex-1" placeholder="Search student, roll, pass ID, bus or boarding point..." value={passSearch} onChange={event => setPassSearch(event.target.value)}/><select className="input-field sm:w-56" value={passAcademicYear} onChange={event => setPassAcademicYear(event.target.value)}><option value="">All academic years</option>{yearOptions.map(year => <option key={year} value={year}>{year}</option>)}</select></div>}
      {type === 'payments' && <div className="space-y-3">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5">
          <input className="input-field xl:col-span-2" placeholder="Search student, roll number, transaction ID, contact or amount..." value={passSearch} onChange={event => setPassSearch(event.target.value)}/>
          <select className="input-field" value={passAcademicYear} onChange={event => setPassAcademicYear(event.target.value)}><option value="">All academic years</option>{yearOptions.map(year => <option key={year} value={year}>{year}</option>)}</select>
          <label className="text-xs font-medium text-gray-600">From date<input type="date" className="input-field mt-1" value={paymentFilters.fromDate} max={paymentFilters.toDate || undefined} onChange={event => setPaymentFilters(current => ({ ...current, fromDate: event.target.value }))}/></label>
          <label className="text-xs font-medium text-gray-600">To date<input type="date" className="input-field mt-1" value={paymentFilters.toDate} min={paymentFilters.fromDate || undefined} onChange={event => setPaymentFilters(current => ({ ...current, toDate: event.target.value }))}/></label>
          <select className="input-field" aria-label="Payment status" value={paymentFilters.status} onChange={event => setPaymentFilters(current => ({ ...current, status: event.target.value }))}><option value="">All statuses</option>{['SUCCESS','INITIATED','PENDING','FAILED','CANCELLED','REFUNDED'].map(status => <option key={status} value={status}>{status.replaceAll('_', ' ')}</option>)}</select>
          <select className="input-field" aria-label="Payment mode" value={paymentFilters.mode} onChange={event => setPaymentFilters(current => ({ ...current, mode: event.target.value }))}><option value="">All modes</option><option value="RAZORPAY">Razorpay</option><option value="ADMIN_MANUAL">Admin Manual</option></select>
          <button type="button" className="btn-secondary" onClick={() => { setPassSearch(''); setPassAcademicYear(''); setPaymentFilters({ fromDate: '', toDate: '', status: '', mode: '' }); }}>Clear filters</button>
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3"><p className="text-sm text-gray-600">Showing {filteredPayments.length} payment transaction{filteredPayments.length === 1 ? '' : 's'} for your assigned bus</p><div className="flex gap-2"><button type="button" className="btn-secondary flex items-center gap-2" disabled={exportingPayments || !filteredPayments.length} onClick={() => downloadPaymentsCsv(filteredPayments)}><FileSpreadsheet size={17}/>Download Excel</button><button type="button" className="btn-primary flex items-center gap-2" disabled={exportingPayments || !filteredPayments.length} onClick={async () => { setExportingPayments(true); try { await downloadPaymentsPdf(filteredPayments, paymentFilters); } finally { setExportingPayments(false); } }}><Download size={17}/>{exportingPayments ? 'Preparing…' : 'Download PDF'}</button></div></div>
      </div>}
      <DataTable columns={configs[type]} data={displayedData} loading={loading} searchable={false}/>
    </div>
    <PassViewerModal pass={selectedPass} photoUrl={photoUrl} onClose={closePass} onDownload={downloadPass} downloading={downloading}/>
    <Modal isOpen={Boolean(onlineFee)} onClose={closeOnlinePayment} title="Pay Student Fee Online">{onlineFee && <form className="space-y-4" onSubmit={submitOnlinePayment}><div className="rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-950"><p className="font-semibold">{onlineFee.studentName} ({onlineFee.rollNumber})</p><p className="mt-1">Academic year: {onlineFee.academicYear}</p><p>Outstanding balance: {formatINR(onlineFee.remainingAmount)}</p></div><label className="block text-sm font-medium text-gray-700">Amount to pay (₹) *<input className="input-field mt-1" type="number" min="1" max={onlineFee.remainingAmount} step="1" required value={onlineAmount} disabled={onlineProcessing} onChange={event => setOnlineAmount(event.target.value)}/></label><p className="text-xs text-gray-500">Payment is credited after server-side Razorpay verification. The student, assigned In-Charge, and administrators will be notified.</p><div className="flex justify-end gap-2"><button type="button" className="btn-secondary" disabled={onlineProcessing} onClick={closeOnlinePayment}>Cancel</button><button type="submit" className="btn-primary flex items-center gap-2" disabled={onlineProcessing}><CreditCard size={17}/>{onlineProcessing ? 'Opening checkout…' : `Pay ${formatINR(Number(onlineAmount) || 0)}`}</button></div></form>}</Modal>
  </>;
}
