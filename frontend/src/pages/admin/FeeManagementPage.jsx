import React, { useState, useEffect, useRef, useCallback } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';
import { formatINR } from '../../utils/currency';
import Modal from '../../components/common/Modal';
import { CreditCard } from 'lucide-react';
import { apiMessage, openRazorpayCheckout } from '../../utils/razorpay';

const FeeManagementPage = () => {
  const [fees, setFees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedFee, setSelectedFee] = useState(null);
  const [payment, setPayment] = useState({ amount: '', reference: '', remarks: '' });
  const [saving, setSaving] = useState(false);
  const [onlineFee, setOnlineFee] = useState(null);
  const [onlineAmount, setOnlineAmount] = useState('');
  const [onlineProcessing, setOnlineProcessing] = useState(false);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [academicYears, setAcademicYears] = useState([]);
  const [academicYearId, setAcademicYearId] = useState('');
  const [yearsLoaded, setYearsLoaded] = useState(false);
  const debounceTimer = useRef(null);
  const requestVersion = useRef(0);

  const { addToast } = useToast();

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

  const fetchFees = useCallback(async (signal) => {
    const version = ++requestVersion.current;
    setLoading(true);
    try {
      const params = { unpaged: true };
      if (debouncedSearch.trim()) params.search = debouncedSearch.trim();
      if (academicYearId) params.academicYearId = academicYearId;
      const res = await api.get('/admin/fees', { signal, params });
      if (signal?.aborted || version !== requestVersion.current) return;
      setFees(Array.isArray(res.data) ? res.data : (res.data?.content || []));
      setLoading(false);
    } catch (err) {
      if (signal?.aborted || err.name === 'CanceledError' || err.code === 'ERR_CANCELED') {
        return;
      }
      addToast('Failed to load fees records', 'error');
      if (version === requestVersion.current) setLoading(false);
    }
  }, [debouncedSearch, academicYearId, addToast]);

  useEffect(() => {
    if (!yearsLoaded) return;
    const controller = new AbortController();
    fetchFees(controller.signal);
    return () => controller.abort();
  }, [yearsLoaded, fetchFees]);

  const openPayment = (fee) => {
    setSelectedFee(fee);
    setPayment({ amount: '', reference: '', remarks: '' });
  };

  const submitPayment = async (event) => {
    event.preventDefault();
    const amount = Number(payment.amount);
    if (!Number.isInteger(amount) || amount <= 0 || amount > selectedFee.remainingAmount) {
      addToast(`Enter a whole-number amount between ₹1 and ${formatINR(selectedFee.remainingAmount)}`, 'error');
      return;
    }
    setSaving(true);
    try {
      await api.post(`/admin/fees/${selectedFee.id}/manual-payment`, { ...payment, amount });
      addToast('Manual fee payment recorded successfully', 'success');
      setSelectedFee(null);
      fetchFees(new AbortController().signal);
    } catch (error) {
      addToast(error.response?.data?.message || 'Unable to record the payment', 'error');
    } finally {
      setSaving(false);
    }
  };

  const openOnlinePayment = (fee) => {
    setOnlineFee(fee);
    setOnlineAmount(String(fee.remainingAmount));
  };

  const closeOnlinePayment = () => {
    if (onlineProcessing) return;
    setOnlineFee(null);
    setOnlineAmount('');
  };

  const submitOnlinePayment = async (event) => {
    event.preventDefault();
    const amount = Number(onlineAmount);
    if (!onlineFee || !Number.isInteger(amount) || amount < 1 || amount > onlineFee.remainingAmount) {
      addToast(`Enter a whole-number amount between ₹1 and ${formatINR(onlineFee?.remainingAmount || 0)}`, 'error');
      return;
    }
    setOnlineProcessing(true);
    try {
      const { data: order } = await api.post('/admin/payments/order', { feeId: onlineFee.id, amount });
      await openRazorpayCheckout({
        order,
        onDismiss: () => setOnlineProcessing(false),
        onFailure: message => { addToast(message, 'error'); setOnlineProcessing(false); },
        onSuccess: async response => {
          try {
            await api.post('/admin/payments/verify', {
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature
            });
            addToast('Online payment verified and the student fee was updated', 'success');
            setOnlineFee(null);
            setOnlineAmount('');
            await fetchFees(new AbortController().signal);
          } catch (error) {
            addToast(apiMessage(error, 'Payment verification failed'), 'error');
          } finally {
            setOnlineProcessing(false);
          }
        }
      });
    } catch (error) {
      addToast(apiMessage(error, 'Unable to start secure payment checkout'), 'error');
      setOnlineProcessing(false);
    }
  };

  const columns = [
    { key: 'student', label: 'Student', render: (row) => (
      <div>
        <span className="font-bold text-blue-900">{row.rollNumber || 'N/A'}</span>
        {row.studentName && <span className="text-xs text-gray-500 block">{row.studentName}</span>}
      </div>
    )},
    { key: 'academicYear', label: 'Academic Year', render: (row) => <span className="font-semibold">{row.academicYear || '—'}</span> },
    { key: 'totalAmount', label: 'Total Fee', render: (row) => formatINR(row.totalAmount) },
    { key: 'paidAmount', label: 'Paid Amount', render: (row) => <span className="text-green-700 font-medium">{formatINR(row.paidAmount)}</span> },
    { key: 'remainingAmount', label: 'Remaining', render: (row) => <span className="text-red-600 font-medium">{formatINR(row.remainingAmount)}</span> },
    {
      key: 'percentage',
      label: 'Progress',
      render: (row) => (
        <div className="w-36">
          <div className="flex justify-between text-xs text-gray-500 mb-1">
            <span>{row.paidPercentage?.toFixed(0) || 0}%</span>
            <span>{row.passEligible ? 'Pass Eligible' : '< 50%'}</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div
              className={`h-2 rounded-full ${row.paidPercentage >= 50 ? 'bg-green-600' : 'bg-blue-600'}`}
              style={{ width: `${Math.min(100, row.paidPercentage || 0)}%` }}
            ></div>
          </div>
        </div>
      )
    },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    { key: 'actions', label: 'Actions', render: (row) => row.remainingAmount > 0 ? (
      <div className="flex flex-wrap gap-3">
        <button type="button" className="whitespace-nowrap text-sm font-medium text-blue-700 hover:underline" onClick={() => openOnlinePayment(row)}>Pay Online</button>
        <button type="button" className="whitespace-nowrap text-sm font-medium text-gray-700 hover:underline" onClick={() => openPayment(row)}>Record Manual</button>
      </div>
    ) : <span className="text-sm text-gray-400">Fully paid</span> }
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-800">Fee Collection & Audit</h2>
        <p className="text-gray-500 text-sm">Monitor student transportation fees, payment percentages, and pass eligibility</p>
      </div>

      <div className="card">
        <div className="mb-4">
          <div className="grid gap-3 sm:grid-cols-2"><input
            className="input-field"
            value={search}
            onChange={handleSearchChange}
            placeholder="Search student name, roll number, academic year or status..."
          /><select className="input-field" value={academicYearId} onChange={(e)=>setAcademicYearId(e.target.value)}><option value="">All academic years</option>{academicYears.map((year)=><option key={year.id} value={year.id}>{year.yearName}{year.active?' (Active)':''}</option>)}</select></div>
        </div>
        <DataTable columns={columns} data={fees} loading={loading} searchable={false} />
      </div>
      <Modal isOpen={Boolean(selectedFee)} onClose={() => !saving && setSelectedFee(null)} title="Record Manual Fee Payment">
        {selectedFee && <form className="space-y-4" onSubmit={submitPayment}>
          <div className="rounded-lg bg-blue-50 p-3 text-sm">
            <div className="font-semibold text-blue-950">{selectedFee.studentName} ({selectedFee.rollNumber})</div>
            <div className="mt-1 text-blue-800">Remaining balance: {formatINR(selectedFee.remainingAmount)}</div>
          </div>
          <label className="block text-sm font-medium text-gray-700">Amount paid (₹) *
            <input className="input-field mt-1" type="number" min="1" max={selectedFee.remainingAmount} step="1" required value={payment.amount} onChange={(e) => setPayment({ ...payment, amount: e.target.value })}/>
          </label>
          <label className="block text-sm font-medium text-gray-700">Receipt / bank reference
            <input className="input-field mt-1" maxLength="100" value={payment.reference} onChange={(e) => setPayment({ ...payment, reference: e.target.value })} placeholder="Optional external receipt reference"/>
          </label>
          <label className="block text-sm font-medium text-gray-700">Remarks
            <textarea className="input-field mt-1" rows="3" maxLength="500" value={payment.remarks} onChange={(e) => setPayment({ ...payment, remarks: e.target.value })} placeholder="Optional payment note"/>
          </label>
          <div className="flex justify-end gap-2">
            <button type="button" className="btn-secondary" disabled={saving} onClick={() => setSelectedFee(null)}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Recording...' : 'Record Payment'}</button>
          </div>
        </form>}
      </Modal>
      <Modal isOpen={Boolean(onlineFee)} onClose={closeOnlinePayment} title="Pay Student Fee Online">
        {onlineFee && <form className="space-y-4" onSubmit={submitOnlinePayment}>
          <div className="rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-950">
            <p className="font-semibold">{onlineFee.studentName} ({onlineFee.rollNumber})</p>
            <p className="mt-1">Academic year: {onlineFee.academicYear}</p>
            <p>Outstanding balance: {formatINR(onlineFee.remainingAmount)}</p>
          </div>
          <label className="block text-sm font-medium text-gray-700">Amount to pay (₹) *
            <input className="input-field mt-1" type="number" min="1" max={onlineFee.remainingAmount} step="1" required value={onlineAmount} disabled={onlineProcessing} onChange={(e) => setOnlineAmount(e.target.value)}/>
          </label>
          <p className="text-xs text-gray-500">The transaction is credited only after server-side Razorpay verification. The student, assigned In-Charge, and administrators will be notified.</p>
          <div className="flex justify-end gap-2">
            <button type="button" className="btn-secondary" disabled={onlineProcessing} onClick={closeOnlinePayment}>Cancel</button>
            <button type="submit" className="btn-primary flex items-center gap-2" disabled={onlineProcessing}><CreditCard size={17}/>{onlineProcessing ? 'Opening checkout…' : `Pay ${formatINR(Number(onlineAmount) || 0)}`}</button>
          </div>
        </form>}
      </Modal>
    </div>
  );
};

export default FeeManagementPage;
