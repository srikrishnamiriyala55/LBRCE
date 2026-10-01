import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import { useToast } from '../../components/common/Toast';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Modal from '../../components/common/Modal';
import { CreditCard, ShieldCheck } from 'lucide-react';

const FeeViewPage = () => {
  const [fees, setFees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [payModalOpen, setPayModalOpen] = useState(false);
  const [selectedFee, setSelectedFee] = useState(null);
  const [amountToPay, setAmountToPay] = useState('');
  const [processing, setProcessing] = useState(false);

  const { addToast } = useToast();

  useEffect(() => {
    fetchFees();
  }, []);

  const fetchFees = async () => {
    try {
      const res = await api.get('/student/fees');
      setFees(res.data);
    } catch (err) {
      addToast('Failed to load fee details', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenPayment = (fee) => {
    setSelectedFee(fee);
    // Note: online payment not yet live — show Coming Soon modal
    setPayModalOpen(true);
  };

  // Dummy handler: actual payment gateway not yet integrated
  const handleProcessPayment = (e) => {
    e.preventDefault();
    setPayModalOpen(false);
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-800">Transportation Fee & Payment</h2>
        <p className="text-gray-500 text-sm">Review your allocated transit fee, payment history, and bus pass eligibility status</p>
      </div>

      {fees.length === 0 ? (
        <div className="card text-center text-gray-500 py-12">
          <p className="text-lg font-medium text-gray-700">No active transportation fee records found.</p>
          <p className="text-sm text-gray-500 mt-1">Once your bus application is approved by the incharge, your semester fee will be assigned here.</p>
        </div>
      ) : (
        fees.map(fee => (
          <div key={fee.id} className="card space-y-4">
            <div className="flex flex-wrap justify-between items-start gap-2">
              <div>
                <h3 className="text-lg font-bold text-blue-900">Academic Year: {fee.academicYear}</h3>
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

            {fee.remainingAmount > 0 ? (
              <div className="border-t pt-4 flex justify-end">
                <button
                  onClick={() => handleOpenPayment(fee)}
                  className="btn-primary flex items-center gap-2"
                >
                  <CreditCard size={18} /> Pay Transport Fee (₹{fee.remainingAmount})
                </button>
              </div>
            ) : (
              <p className="text-sm text-green-700 font-semibold text-right pt-2">✓ Full transportation fee paid for this term.</p>
            )}
          </div>
        ))
      )}

      {/* Dummy Payment Modal — Online payment coming soon */}
      <Modal isOpen={payModalOpen} onClose={() => setPayModalOpen(false)} title="Transportation Fee Payment">
        <div className="space-y-5">
          <div className="rounded-lg bg-amber-50 border border-amber-200 p-4">
            <div className="flex items-start gap-3">
              <CreditCard className="mt-0.5 shrink-0 text-amber-600" size={22} />
              <div>
                <p className="font-semibold text-amber-900">Online Payment — Coming Soon</p>
                <p className="mt-1 text-sm text-amber-800">
                  The online payment gateway is currently under development and will be available soon.
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-lg bg-blue-50 border border-blue-200 p-4">
            <p className="text-sm font-semibold text-blue-900 mb-2">Pay via Cash / DD at the Transport Office:</p>
            <ul className="text-sm text-blue-800 list-disc list-inside space-y-1">
              <li>Visit the LBRCE Transport Office with your student ID</li>
              <li>Pay the required fee amount: <strong>₹{selectedFee?.remainingAmount}</strong></li>
              <li>Your payment will be recorded and your pass status will be updated</li>
            </ul>
          </div>

          <div className="flex justify-end">
            <button type="button" className="btn-primary" onClick={() => setPayModalOpen(false)}>Close</button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default FeeViewPage;
