import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import PassCard from '../../components/passes/PassCard';
import { useToast } from '../../components/common/Toast';
import { Download, Printer } from 'lucide-react';

const BusPassPage = () => {
  const [pass, setPass] = useState(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);
  const [photoUrl, setPhotoUrl] = useState('');
  const { addToast } = useToast();

  useEffect(() => {
    const fetchPass = async () => {
      try {
        const res = await api.get('/student/pass');
        if (res.data) {
          setPass(res.data);
          if (res.data.photoAvailable) {
            const photoResponse = await api.get('/student/pass/photo', { responseType: 'blob' });
            setPhotoUrl(URL.createObjectURL(photoResponse.data));
          }
        }
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchPass();
  }, []);

  useEffect(() => () => { if (photoUrl) URL.revokeObjectURL(photoUrl); }, [photoUrl]);

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

      <PassCard pass={pass} photoUrl={photoUrl}/>
    </div>
  );
};

export default BusPassPage;
