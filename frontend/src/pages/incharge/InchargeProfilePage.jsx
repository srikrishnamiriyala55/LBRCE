import React, { useEffect, useState } from 'react';
import api from '../../utils/axios';

export default function InchargeProfilePage() {
  const [profile, setProfile] = useState({}), [photoUrl, setPhotoUrl] = useState('');
  const load = async () => { try { const response = await api.get('/incharge/profile'); setProfile(response.data); if (response.data.photoAvailable) { const photo = await api.get('/incharge/profile/photo', { responseType: 'blob' }); setPhotoUrl(URL.createObjectURL(photo.data)); } } catch { setProfile({}); } };
  useEffect(() => { load(); return () => { if (photoUrl) URL.revokeObjectURL(photoUrl); }; }, []);
  const fields = [['Teacher ID', profile.teacherId], ['Name', profile.name], ['Email', profile.email], ['Phone Number', profile.phoneNumber], ['Department', profile.department], ['Designation', profile.designation], ['Address', profile.address], ['Assigned Bus', profile.assignedBus]];
  return <div className="card max-w-3xl"><h2 className="mb-5 text-xl font-bold">In-Charge Profile</h2><div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center"><div className="flex h-28 w-28 items-center justify-center overflow-hidden rounded-full border bg-gray-50 text-center text-xs text-gray-400">{photoUrl ? <img src={photoUrl} alt="Profile" className="h-full w-full object-cover"/> : 'Photo unavailable'}</div><p className="text-xs text-gray-500">Your profile photo is maintained by the transport administrator.</p></div><div className="grid gap-4 md:grid-cols-2">{fields.map(([label, value]) => <div key={label}><div className="text-xs uppercase text-gray-500">{label}</div><div className="font-medium">{value || '—'}</div></div>)}</div></div>;
}
