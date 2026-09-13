import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';
import { Download, Printer } from 'lucide-react';

const BusPassPage = () => {
  const [pass, setPass] = useState(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);
  const { addToast } = useToast();

  useEffect(() => {
    const fetchPass = async () => {
      try {
        const res = await api.get('/student/pass');
        if (res.data) setPass(res.data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchPass();
  }, []);

  const downloadPass = async () => {
    setDownloading(true);
    try {
      const response = await api.get('/student/pass/download', { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = `bus-pass-${pass.rollNumber}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
      addToast('Bus pass downloaded successfully', 'success');
    } catch (error) {
      addToast(error.response?.data?.message || 'Unable to download bus pass', 'error');
    } finally {
      setDownloading(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  if (!pass) {
    return (
      <div className="card text-center py-12">
        <h3 className="text-xl font-medium text-gray-800 mb-2">No Active Bus Pass</h3>
        <p className="text-gray-500 max-w-md mx-auto">
          You currently do not have an active bus pass. To generate a pass, your bus application must be approved and you must pay at least 50% of the transport fee.
        </p>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="grid grid-cols-2 sm:flex sm:justify-end gap-3 mb-4">
        <button disabled={downloading} onClick={downloadPass} className="btn-primary flex items-center justify-center gap-2 disabled:opacity-60">
          <Download size={16} /> {downloading ? 'Downloading...' : 'Download PDF'}
        </button>
        <button onClick={() => window.print()} className="btn-secondary flex items-center gap-2">
          <Printer size={16} /> Print
        </button>
      </div>

      {/* Bus Pass Card */}
      <div id="bus-pass" className="bg-white rounded-xl shadow-lg border border-gray-200 overflow-hidden">
        <div className="bg-blue-800 p-4 text-white text-center flex flex-col items-center">
          <img src="/logo.jpg" alt="LBRCE Logo" className="h-16 w-16 bg-white rounded-full p-1 mb-2" />
          <h2 className="font-bold text-lg">LAKIREDDY BALI REDDY COLLEGE OF ENGINEERING</h2>
          <p className="text-sm text-blue-200">DIGITAL BUS PASS</p>
        </div>
        
        <div className="p-4 sm:p-6">
          <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-4 mb-6">
            <div>
              <p className="text-sm text-gray-500">Pass Number</p>
              <p className="font-bold text-lg">{pass.passNumber}</p>
            </div>
            <StatusBadge status="ACTIVE" />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-4 gap-x-8">
            <div>
              <p className="text-xs text-gray-500 uppercase">Student Name</p>
              <p className="font-medium text-gray-900">{pass.studentName}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500 uppercase">Roll Number</p>
              <p className="font-medium text-gray-900">{pass.rollNumber}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500 uppercase">Branch</p>
              <p className="font-medium text-gray-900">{pass.branch || '-'}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500 uppercase">Year / Semester</p>
              <p className="font-medium text-gray-900">{pass.year || '-'} / {pass.semester || '-'}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500 uppercase">Phone Number</p>
              <p className="font-medium text-gray-900">{pass.phoneNumber || '-'}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500 uppercase">Bus Number</p>
              <p className="font-medium text-gray-900">{pass.busNumber}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500 uppercase">Route</p>
              <p className="font-medium text-gray-900">{pass.routeName}</p>
            </div>
            <div className="sm:col-span-2">
              <p className="text-xs text-gray-500 uppercase">Boarding Point</p>
              <p className="font-medium text-gray-900">{pass.boardingPoint}</p>
            </div>
          </div>
        </div>
        <div className="bg-gray-50 px-6 py-3 border-t text-center text-xs text-gray-500">
          This is a system generated digital pass. Valid for the current academic year.
        </div>
      </div>
    </div>
  );
};

export default BusPassPage;
