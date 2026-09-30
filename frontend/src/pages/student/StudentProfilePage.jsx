import React, { useEffect, useState } from 'react';
import api from '../../utils/axios';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';

const StudentProfilePage = () => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [photoUrl, setPhotoUrl] = useState('');
  const { addToast } = useToast();

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const res = await api.get('/student/profile');
      setProfile(res.data);
      if (res.data.photoAvailable) { const photo = await api.get('/student/profile/photo', { responseType: 'blob' }); setPhotoUrl(URL.createObjectURL(photo.data)); }
    } catch (err) {
      addToast('Failed to load profile', 'error');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div className="card max-w-3xl mx-auto">
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
  );
};

export default StudentProfilePage;
