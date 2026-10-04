import React, { useEffect, useState } from 'react';
import api from '../../utils/axios';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import StatusBadge from '../../components/common/StatusBadge';
import { ShieldCheck, Mail, Phone, User, Hash } from 'lucide-react';
import { useToast } from '../../components/common/Toast';

export default function AdminProfilePage() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const { addToast } = useToast();

  useEffect(() => {
    api.get('/admin/profile')
      .then((res) => {
        setProfile(res.data);
      })
      .catch((err) => {
        addToast(err.response?.data?.message || 'Failed to load admin profile', 'error');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [addToast]);

  if (loading) return <LoadingSpinner />;

  return (
    <div className="card max-w-3xl mx-auto space-y-6">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3 pb-4 border-b border-gray-100">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Administrator Profile</h2>
          <p className="text-sm text-gray-500">System administrator credentials and access details</p>
        </div>
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200">
          <ShieldCheck size={15} /> Transport Administrator
        </span>
      </div>

      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4 p-5 rounded-xl bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-100">
        <div className="w-20 h-20 rounded-full bg-blue-700 text-white flex items-center justify-center font-bold text-2xl shadow-md shrink-0">
          <ShieldCheck size={38} />
        </div>
        <div className="text-center sm:text-left space-y-1">
          <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2">
            <h3 className="text-xl font-bold text-gray-900">{profile?.name || 'Administrator'}</h3>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-800 text-white">
              {profile?.adminId || 'ADMIN'}
            </span>
          </div>
          <p className="text-sm text-blue-900 font-medium">LBRCE Bus Transport Management System</p>
          <div className="flex flex-wrap items-center justify-center sm:justify-start gap-4 pt-2 text-xs text-gray-600">
            {profile?.email && (
              <span className="flex items-center gap-1.5">
                <Mail size={14} className="text-blue-600" />
                {profile.email}
              </span>
            )}
            {profile?.phoneNumber && (
              <span className="flex items-center gap-1.5">
                <Phone size={14} className="text-blue-600" />
                {profile.phoneNumber}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Profile Details Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-5 pt-2">
        <div className="p-4 rounded-lg border border-gray-100 bg-gray-50 space-y-1">
          <span className="text-xs uppercase tracking-wider text-gray-500 font-medium flex items-center gap-1.5">
            <Hash size={13} className="text-blue-600" /> Administrator ID
          </span>
          <p className="font-semibold text-gray-900 text-base">{profile?.adminId || '—'}</p>
        </div>

        <div className="p-4 rounded-lg border border-gray-100 bg-gray-50 space-y-1">
          <span className="text-xs uppercase tracking-wider text-gray-500 font-medium flex items-center gap-1.5">
            <User size={13} className="text-blue-600" /> Full Name
          </span>
          <p className="font-semibold text-gray-900 text-base">{profile?.name || '—'}</p>
        </div>

        <div className="p-4 rounded-lg border border-gray-100 bg-gray-50 space-y-1">
          <span className="text-xs uppercase tracking-wider text-gray-500 font-medium flex items-center gap-1.5">
            <Mail size={13} className="text-blue-600" /> Email Address
          </span>
          <p className="font-semibold text-gray-900 text-base">{profile?.email || '—'}</p>
        </div>

        <div className="p-4 rounded-lg border border-gray-100 bg-gray-50 space-y-1">
          <span className="text-xs uppercase tracking-wider text-gray-500 font-medium flex items-center gap-1.5">
            <Phone size={13} className="text-blue-600" /> Contact Number
          </span>
          <p className="font-semibold text-gray-900 text-base">{profile?.phoneNumber || '—'}</p>
        </div>

        <div className="p-4 rounded-lg border border-gray-100 bg-gray-50 space-y-1">
          <span className="text-xs uppercase tracking-wider text-gray-500 font-medium flex items-center gap-1.5">
            <ShieldCheck size={13} className="text-blue-600" /> Assigned Role
          </span>
          <p className="font-semibold text-indigo-700 text-base">SYSTEM ADMINISTRATOR</p>
        </div>

        <div className="p-4 rounded-lg border border-gray-100 bg-gray-50 space-y-1">
          <span className="text-xs uppercase tracking-wider text-gray-500 font-medium">Account Status</span>
          <div className="pt-0.5">
            <StatusBadge status={profile?.status || 'ACTIVE'} />
          </div>
        </div>
      </div>
    </div>
  );
}
