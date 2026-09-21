import React, { useEffect, useState } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import StatusBadge from '../../components/common/StatusBadge';
import PassViewerModal from '../../components/passes/PassViewerModal';
import { useToast } from '../../components/common/Toast';

export default function AdminDataPage({ type }) {
  const [rows, setRows] = useState([]), [page, setPage] = useState(0), [pages, setPages] = useState(1), [loading, setLoading] = useState(true);
  const [selectedPass, setSelectedPass] = useState(null), [photoUrl, setPhotoUrl] = useState(''), [downloading, setDownloading] = useState(false);
  const { addToast } = useToast();

  useEffect(() => {
    setLoading(true);
    api.get(`/admin/${type}?page=${page}&size=20`).then((r) => { setRows(r.data.content || []); setPages(r.data.totalPages || 1); })
      .catch((e) => addToast(e.response?.data?.message || `Failed to load ${type}`, 'error')).finally(() => setLoading(false));
  }, [type, page]);

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
    transfers: [{ key: 'studentName', label: 'Student' }, { key: 'studentRollNumber', label: 'Roll Number' }, { key: 'currentBusNumber', label: 'Current Bus' }, { key: 'requestedBusNumber', label: 'Requested Bus' }, { key: 'oldInchargeName', label: 'Old In-Charge' }, { key: 'newInchargeName', label: 'New In-Charge' }, { key: 'status', label: 'State', render: (r) => <StatusBadge status={r.status} /> }],
    payments: [{ key: 'studentName', label: 'Student' }, { key: 'rollNumber', label: 'Roll Number' }, { key: 'orderId', label: 'Order ID' }, { key: 'amount', label: 'Amount', render: (r) => `₹${r.amount || 0}` }, { key: 'status', label: 'Verified Status', render: (r) => <StatusBadge status={r.status} /> }, { key: 'paidAt', label: 'Paid At' }],
    passes: [{ key: 'studentName', label: 'Student' }, { key: 'rollNumber', label: 'Roll Number' }, { key: 'passNumber', label: 'Pass ID' }, { key: 'busNumber', label: 'Bus' }, { key: 'boardingPoint', label: 'Boarding Point' }, { key: 'validUntil', label: 'Valid Until' }, { key: 'status', label: 'Status', render: (r) => <StatusBadge status={r.status} /> }, { key: 'actions', label: 'Action', render: (r) => <button type="button" className="text-sm font-medium text-blue-700 hover:underline" onClick={() => viewPass(r)}>View</button> }],
    notifications: [{ key: 'title', label: 'Title' }, { key: 'recipientRollNumber', label: 'Recipient' }, { key: 'message', label: 'Message' }, { key: 'createdAt', label: 'Date', render: (r) => r.createdAt ? new Date(r.createdAt).toLocaleString() : '—' }]
  };
  return <><div className="card"><h2 className="text-2xl font-bold mb-5 capitalize">{type}</h2><DataTable columns={configs[type]} data={rows} loading={loading} /><Pagination page={page} totalPages={pages} onPageChange={setPage} /></div><PassViewerModal pass={selectedPass} photoUrl={photoUrl} onClose={closePass} onDownload={downloadPass} downloading={downloading} /></>;
}
