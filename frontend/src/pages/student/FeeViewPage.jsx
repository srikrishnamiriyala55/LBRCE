import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import { useToast } from '../../components/common/Toast';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Modal from '../../components/common/Modal';
import { CreditCard, ShieldCheck } from 'lucide-react';
import DataTable from '../../components/common/DataTable';
import { formatINR } from '../../utils/currency';
import { apiMessage, openRazorpayCheckout } from '../../utils/razorpay';

const FeeViewPage = () => {
  const [fees, setFees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [payModalOpen, setPayModalOpen] = useState(false);
  const [selectedFee, setSelectedFee] = useState(null);
  const [amountToPay, setAmountToPay] = useState('');
  const [processing, setProcessing] = useState(false);
  const [payments, setPayments] = useState([]);
  const [academicYear, setAcademicYear] = useState('');

  const { addToast } = useToast();

  useEffect(() => {
    fetchFees();
  }, []);

  const fetchFees = async () => {
    try {
      const [feeResult, paymentResult] = await Promise.all([
        api.get('/student/fees'),
        api.get('/student/payments')
      ]);
      setFees(feeResult.data || []);
      setPayments(paymentResult.data || []);
    } catch (err) {
      addToast('Failed to load fee details', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenPayment = (fee) => {
    setSelectedFee(fee);
    setAmountToPay(String(fee.remainingAmount));
    setPayModalOpen(true);
  };

  const closePaymentModal = () => {
    if (processing) return;
    setPayModalOpen(false);
    setSelectedFee(null);
    setAmountToPay('');
  };

  const handleProcessPayment = async (e) => {
    e.preventDefault();
    const amount = Number(amountToPay);
    if (!selectedFee || !Number.isInteger(amount) || amount < 1 || amount > selectedFee.remainingAmount) {
      addToast(`Enter a whole-rupee amount between ₹1 and ${formatINR(selectedFee?.remainingAmount || 0)}`, 'error');
      return;
    }

    setProcessing(true);
    try {
      const { data: order } = await api.post('/student/payments/order', {
        feeId: selectedFee.id,
        amount
      });

      await openRazorpayCheckout({
        order,
        onDismiss: () => setProcessing(false),
        onSuccess: async response => {
          try {
            await api.post('/student/payments/verify', {
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature
            });
            addToast('Payment verified successfully. Your fee balance has been updated.', 'success');
            setPayModalOpen(false);
            setSelectedFee(null);
            setAmountToPay('');
            await fetchFees();
          } catch (error) {
            addToast(apiMessage(error, 'Payment verification failed. Please contact the transport office with the payment ID.'), 'error');
          } finally {
            setProcessing(false);
          }
        },
        onFailure: message => {
          addToast(message, 'error');
          setProcessing(false);
        }
      });
    } catch (error) {
      addToast(apiMessage(error, 'Unable to start the secure payment checkout'), 'error');
      setProcessing(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  const newestFirst = (left, right) => String(right || '').localeCompare(String(left || ''), undefined, { numeric: true });
  const years = [...new Set([...fees.map(f => f.academicYear), ...payments.map(p => p.academicYear)].filter(Boolean))].sort(newestFirst);
  const visibleFees = (academicYear ? fees.filter(f => f.academicYear === academicYear) : fees)
    .slice().sort((left, right) => newestFirst(left.academicYear, right.academicYear));
  const visiblePayments = academicYear ? payments.filter(p => p.academicYear === academicYear) : payments;
  const paymentColumns = [
    { key: 'createdAt', label: 'Date', render: row => new Date(row.paidAt || row.createdAt).toLocaleString() },
    { key: 'academicYear', label: 'Academic Year' },
    { key: 'orderId', label: 'Receipt / Order ID', render: row => row.orderId || '—' },
    { key: 'transactionId', label: 'Payment ID', render: row => row.transactionId || '—' },
    { key: 'gatewayName', label: 'Mode', render: row => row.gatewayName === 'RAZORPAY' ? 'Online' : (row.gatewayName || '—') },
    { key: 'amount', label: 'Amount', render: row => formatINR(row.amount) },
    { key: 'status', label: 'Status', render: row => <StatusBadge status={row.status} /> }
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-800">Transportation Fee & Payment</h2>
        <p className="text-gray-500 text-sm">Review your allocated transit fee, payment history, and bus pass eligibility status</p>
      </div>

      <div className="card max-w-sm">
        <label className="mb-1 block text-sm font-medium text-gray-700">Academic Year</label>
        <select className="input-field" value={academicYear} onChange={event => setAcademicYear(event.target.value)}>
          <option value="">All academic years</option>
          {years.map(year => <option key={year} value={year}>{year}</option>)}
        </select>
      </div>

      {visibleFees.length === 0 ? (
        <div className="card text-center text-gray-500 py-12">
          <p className="text-lg font-medium text-gray-700">No active transportation fee records found.</p>
          <p className="text-sm text-gray-500 mt-1">Once your bus application is approved by the incharge, your semester fee will be assigned here.</p>
        </div>
      ) : (
        visibleFees.map((fee, index) => (
          <div key={fee.id} className="card space-y-4">
            <div className="flex flex-wrap justify-between items-start gap-2">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-lg font-bold text-blue-900">Academic Year: {fee.academicYear}</h3>
                  {!academicYear && index === 0
                    ? <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-semibold text-blue-800">Newest fee</span>
                    : <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-semibold text-gray-700">Previous-year fee</span>}
                </div>
                <p className="text-sm text-gray-600">Total Approved Fee: ₹{fee.totalAmount}</p>
              </div>
              <div className="flex items-center gap-2">
                {fee.passEligible && (
                  <span className="inline-flex items-center gap-1 text-xs font-semibold bg-green-100 text-green-800 px-2.5 py-1 rounded-full">
                    <ShieldCheck size={14} /> Pass Eligible
                  </span>
                )}
                <StatusBadge status={fee.status} />
              </div>
            </div>

            <div className="bg-gray-50 p-4 rounded-lg space-y-2 border">
              <div className="flex justify-between text-sm">
                <span className="font-medium text-gray-700">Amount Paid: ₹{fee.paidAmount || 0}</span>
                <span className="font-semibold text-red-600">Remaining Balance: ₹{fee.remainingAmount}</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
                <div 
                  className={`h-3 rounded-full transition-all ${fee.paidPercentage >= 50 ? 'bg-green-600' : 'bg-blue-600'}`} 
                  style={{ width: `${Math.min(100, fee.paidPercentage || 0)}%` }}
                ></div>
              </div>
              <div className="flex justify-between text-xs text-gray-500">
                <span>{fee.paidPercentage?.toFixed(1) || 0}% Paid</span>
                <span>(Pass auto-generates at 50% paid: ₹{fee.totalAmount * 0.5})</span>
              </div>
            </div>

            {fee.remainingAmount > 0 && fee.status !== 'PAID' ? (
              <div className="border-t pt-4 flex justify-end">
                <button
                  onClick={() => handleOpenPayment(fee)}
                  className="btn-primary flex items-center gap-2"
                >
                  <CreditCard size={18} /> Pay {(!academicYear && index > 0) ? 'Previous-Year' : 'Transport'} Fee (₹{fee.remainingAmount})
                </button>
              </div>
            ) : (
              <p className="text-sm text-green-700 font-semibold text-right pt-2">✓ Full transportation fee paid for this term.</p>
            )}
          </div>
        ))
      )}

      <div className="card">
        <h3 className="mb-4 text-lg font-bold text-gray-800">Payment Transaction History</h3>
        <DataTable columns={paymentColumns} data={visiblePayments} loading={false} searchable={false} />
      </div>

      <Modal isOpen={payModalOpen} onClose={closePaymentModal} title="Secure Transportation Fee Payment">
        <form className="space-y-5" onSubmit={handleProcessPayment}>
          <div className="rounded-lg border border-blue-200 bg-blue-50 p-4 text-sm text-blue-900">
            <p className="font-semibold">Academic Year: {selectedFee?.academicYear}</p>
            <p className="mt-1">Outstanding balance: <strong>{formatINR(selectedFee?.remainingAmount || 0)}</strong></p>
            <p className="mt-2 text-xs text-blue-700">Payment is processed securely by Razorpay. Your balance changes only after server-side signature and payment verification.</p>
          </div>

          <div>
            <label htmlFor="payment-amount" className="mb-1 block text-sm font-medium text-gray-700">Amount to pay (₹)</label>
            <input
              id="payment-amount"
              type="number"
              min="1"
              max={selectedFee?.remainingAmount || 1}
              step="1"
              required
              className="input-field"
              value={amountToPay}
              onChange={event => setAmountToPay(event.target.value)}
              disabled={processing}
            />
            <p className="mt-1 text-xs text-gray-500">Amounts are shown and recorded in Indian rupees.</p>
          </div>

          <div className="flex justify-end gap-3">
            <button type="button" className="btn-secondary" onClick={closePaymentModal} disabled={processing}>Cancel</button>
            <button type="submit" className="btn-primary flex items-center gap-2" disabled={processing}>
              <CreditCard size={18} /> {processing ? 'Opening secure checkout…' : `Pay ${formatINR(Number(amountToPay) || 0)}`}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default FeeViewPage;
