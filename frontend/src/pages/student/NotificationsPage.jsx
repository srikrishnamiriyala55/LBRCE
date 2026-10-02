import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import LoadingSpinner from '../../components/common/LoadingSpinner';

const NotificationsPage = () => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchNotifications();
  }, []);

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const res = await api.get('/student/notifications?unpaged=true');
      setNotifications(res.data.content || res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const markAsRead = async (id, isRead) => {
    if (isRead) return;
    try {
      await api.put(`/student/notifications/${id}/read`);
      setNotifications(notifications.map(n => n.id === id ? { ...n, isRead: true } : n));
    } catch (err) {}
  };

  if (loading && notifications.length === 0) return <LoadingSpinner />;

  return (
    <div className="card max-w-4xl mx-auto">
      <h2 className="text-xl font-bold text-gray-800 mb-6">Notifications</h2>
      
      <div className="space-y-4">
        {notifications.length === 0 ? (
          <p className="text-gray-500 text-center py-8">No notifications found.</p>
        ) : (
          notifications.map(notif => (
            <div 
              key={notif.id} 
              onClick={() => markAsRead(notif.id, notif.isRead)}
              className={`p-4 rounded-lg border cursor-pointer transition-colors ${notif.isRead ? 'bg-white border-gray-200' : 'bg-blue-50 border-blue-200'}`}
            >
              <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-1 mb-1">
                <h4 className={`font-semibold ${notif.isRead ? 'text-gray-700' : 'text-blue-900'}`}>{notif.title}</h4>
                <span className="text-xs text-gray-500">{new Date(notif.createdAt).toLocaleDateString()}</span>
              </div>
              <p className="text-sm text-gray-600">{notif.message}</p>
            </div>
          ))
        )}
      </div>
      
    </div>
  );
};

export default NotificationsPage;
