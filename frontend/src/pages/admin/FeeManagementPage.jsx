import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';
import { formatINR } from '../../utils/currency';
import Modal from '../../components/common/Modal';

const FeeManagementPage = () => {
  const [fees, setFees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedFee, setSelectedFee] = useState(null);
  const [payment, setPayment] = useState({ amount: '', reference: '', remarks: '' });
  const [saving, setSaving] = useState(false);

  const { addToast } = useToast();

  useEffect(() => {
    fetchFees();
  }, [page]);

  const fetchFees = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/admin/fees?page=${page}&size=10`);
      setFees(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch (err) {
      addToast('Failed to load fees records', 'error');
    } finally {
      setLoading(false);
    }
  };

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
      await fetchFees();
    } catch (error) {
      addToast(error.response?.data?.message || 'Unable to record the payment', 'error');
    } finally {
      setSaving(false);
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
    { key: 'actions', label: 'Action', render: (row) => row.remainingAmount > 0 ? (
      <button type="button" className="whitespace-nowrap text-sm font-medium text-blue-700 hover:underline" onClick={() => openPayment(row)}>
        Record Payment
      </button>
    ) : <span className="text-sm text-gray-400">Fully paid</span> }
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-800">Fee Collection & Audit</h2>
        <p className="text-gray-500 text-sm">Monitor student transportation fees, payment percentages, and pass eligibility</p>
      </div>

      <div className="card">
        <DataTable columns={columns} data={fees} loading={loading} searchPlaceholder="Search student, roll number, year, fee or status..." />
        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
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
    </div>
  );
};

export default FeeManagementPage;
