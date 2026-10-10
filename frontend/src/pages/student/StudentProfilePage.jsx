import React, { useEffect, useState } from 'react';
import api from '../../utils/axios';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';

const StudentProfilePage = () => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [photoUrl, setPhotoUrl] = useState('');
  const [history, setHistory] = useState([]);
  const [selectedYearId, setSelectedYearId] = useState('');
  const { addToast } = useToast();

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const [res, historyResponse] = await Promise.all([api.get('/student/profile'), api.get('/student/history')]);
      setProfile(res.data);
      const years = historyResponse.data || [];
      setHistory(years);
      setSelectedYearId(String((years.find(year => year.active) || years[0])?.academicYearId || ''));
      if (res.data.photoAvailable) { const photo = await api.get('/student/profile/photo', { responseType: 'blob' }); setPhotoUrl(URL.createObjectURL(photo.data)); }
    } catch (err) {
      addToast('Failed to load profile', 'error');
    } finally {
      setLoading(false);
    }
  };
  const downloadHistoricalPass=async(pass)=>{try{const response=await api.get(`/student/passes/${pass.id}/download`,{responseType:'blob'});const url=URL.createObjectURL(response.data);const link=document.createElement('a');link.href=url;link.download=`bus-pass-${pass.academicYear || 'history'}-${profile?.rollNumber || 'student'}.pdf`;link.click();URL.revokeObjectURL(url);}catch(error){addToast(error.response?.data?.message||'Unable to download historical pass','error');}};

  if (loading) return <LoadingSpinner />;
  const selectedHistory = history.find(year => String(year.academicYearId) === selectedYearId);

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
    <div className="card">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3 mb-6">
        <h2 className="text-xl font-bold text-gray-800">Student Profile</h2>
        <p className="text-sm text-gray-500">Profile information is maintained by the transport office.</p>
      </div>
      <div className="mb-6 flex items-center gap-4"><div className="flex h-28 w-28 items-center justify-center overflow-hidden rounded-full border bg-gray-50 text-xs text-gray-400">{photoUrl ? <img src={photoUrl} alt="Student profile" className="h-full w-full object-cover"/> : 'Photo unavailable'}</div><p className="text-sm text-gray-500">Your registration photo is managed by the transport office.</p></div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label className="block text-sm font-medium text-gray-500">Name</label>
          <p className="mt-1 text-gray-900 font-medium">{profile?.name}</p>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-500">Roll Number</label>
          <p className="mt-1 text-gray-900 font-medium">{profile?.rollNumber}</p>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-500">Email</label>
          <p className="mt-1 text-gray-900 font-medium">{profile?.email}</p>
        </div>
        
        {/* Editable Fields */}
        <div>
          <label className="block text-sm font-medium text-gray-500 mb-1">Phone Number</label>
          <p className="mt-1 text-gray-900 font-medium">{profile?.phoneNumber || profile?.phone || 'N/A'}</p>
        </div>
        <div><label className="block text-sm font-medium text-gray-500">Department</label><p className="mt-1 text-gray-900 font-medium">{profile?.branch || '—'}</p></div>
        <div><label className="block text-sm font-medium text-gray-500">Year / Semester</label><p className="mt-1 text-gray-900 font-medium">{profile?.year || '—'} / {profile?.semester || '—'}</p></div>
        <div><label className="block text-sm font-medium text-gray-500">Date of Birth</label><p className="mt-1 text-gray-900 font-medium">{profile?.dob || '—'}</p></div>
        <div><label className="block text-sm font-medium text-gray-500">Gender</label><p className="mt-1 text-gray-900 font-medium">{profile?.gender || '—'}</p></div>
        <div><label className="block text-sm font-medium text-gray-500">Blood Group</label><p className="mt-1 text-gray-900 font-medium">{profile?.bloodGroup || '—'}</p></div>
        <div><label className="block text-sm font-medium text-gray-500">Account Status</label><p className="mt-1 text-gray-900 font-medium">{profile?.status || '—'}</p></div>
        <div><label className="block text-sm font-medium text-gray-500">Parent / Guardian Name</label><p className="mt-1 text-gray-900 font-medium">{profile?.parentName || '—'}</p></div>
        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-500 mb-1">Address</label>
          <p className="mt-1 text-gray-900 font-medium">{profile?.address || 'N/A'}</p>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-500 mb-1">Parent Phone Number</label>
          <p className="mt-1 text-gray-900 font-medium">{profile?.parentPhoneNumber || 'N/A'}</p>
        </div>
      </div>
    </div>
    <div className="card space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div><h2 className="text-xl font-bold text-gray-800">My Academic & Transportation History</h2><p className="text-sm text-gray-500">Applications, assigned buses, fees, payments, passes and transfers are retained separately for every academic year.</p></div>
        <label className="block min-w-56 text-sm font-medium text-gray-700">Academic Year
          <select className="input-field mt-1" value={selectedYearId} onChange={event=>setSelectedYearId(event.target.value)}>
            {history.map(year=><option key={year.academicYearId} value={year.academicYearId}>{year.academicYear}{year.active?' (Active)':''}</option>)}
          </select>
        </label>
      </div>
      {!selectedHistory ? <p className="rounded-lg bg-gray-50 p-6 text-center text-gray-500">No historical records are available.</p> : <>
        <div className="grid gap-3 sm:grid-cols-3">
          <HistoryValue label="Department" value={selectedHistory.branch}/><HistoryValue label="Study Year" value={selectedHistory.year}/><HistoryValue label="Semester" value={selectedHistory.semester}/>
        </div>
        <HistorySection title="Bus Applications" empty="No bus application in this academic year">
          {selectedHistory.applications?.map(item=><HistoryRow key={item.id} title={`${item.busNumber || 'Bus'} · ${item.boardingPointName || 'No boarding point'}`} details={`${item.startingPoint || '—'} → ${item.endingPoint || '—'}`} status={item.status}/>) }
        </HistorySection>
        <HistorySection title="Bus Allocations" empty="No bus was allocated in this academic year">
          {selectedHistory.allocations?.map(item=><HistoryRow key={item.id} title={`${item.busNumber || 'Bus'} · ${item.boardingPoint || '—'}`} details={`${item.startingPoint || '—'} → ${item.endingPoint || '—'} · In-charge: ${item.inchargeName || '—'}`} status={item.status}/>) }
        </HistorySection>
        <HistorySection title="Fee Details" empty="No transportation fee was generated">
          {selectedHistory.fees?.map(item=><HistoryRow key={item.id} title={`Total ₹${item.totalAmount || 0} · Paid ₹${item.paidAmount || 0}`} details={`Pending fee: ₹${item.remainingAmount || 0}`} status={item.status}/>) }
        </HistorySection>
        <HistorySection title="Payment Transactions" empty="No payment transactions in this academic year">
          {selectedHistory.payments?.map(item=><HistoryRow key={item.id} title={`₹${item.amount || 0} · ${item.orderId || 'Receipt unavailable'}`} details={item.paidAt || item.createdAt || '—'} status={item.status}/>) }
        </HistorySection>
        <HistorySection title="Bus Passes" empty="No bus pass was generated">
          {selectedHistory.passes?.map(item=><HistoryRow key={item.id} title={item.passNumber} details={`${item.busNumber || '—'} · ${item.boardingPoint || '—'} · Valid until ${item.validUntil || '—'}`} status={item.status} action={<button type="button" className="text-xs font-semibold text-blue-700 underline" onClick={()=>downloadHistoricalPass(item)}>Download</button>}/>) }
        </HistorySection>
        <HistorySection title="Bus Transfers" empty="No transfer requests in this academic year">
          {selectedHistory.transfers?.map(item=><HistoryRow key={item.id} title={`${item.currentBusNumber || '—'} → ${item.requestedBusNumber || '—'}`} details={item.reason || 'No reason recorded'} status={item.status}/>) }
        </HistorySection>
      </>}
    </div>
    </div>
  );
};

const HistoryValue=({label,value})=><div className="rounded-lg border bg-gray-50 p-3"><p className="text-xs uppercase tracking-wide text-gray-500">{label}</p><p className="mt-1 font-semibold text-gray-900">{value ?? '—'}</p></div>;
const HistorySection=({title,empty,children})=>{const items=React.Children.toArray(children).filter(Boolean);return <section><h3 className="mb-2 font-bold text-gray-800">{title}</h3>{items.length?<div className="divide-y rounded-lg border">{items}</div>:<p className="rounded-lg bg-gray-50 p-3 text-sm text-gray-500">{empty}</p>}</section>;};
const HistoryRow=({title,details,status,action})=><div className="flex flex-col gap-1 p-3 sm:flex-row sm:items-center sm:justify-between"><div><p className="font-medium text-gray-900">{title || '—'}</p><p className="text-sm text-gray-500">{details || '—'}</p></div><div className="flex items-center gap-3"><span className="w-fit rounded-full bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-800">{status || '—'}</span>{action}</div></div>;

export default StudentProfilePage;
