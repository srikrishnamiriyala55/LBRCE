import React from 'react';
import { useNavigate } from 'react-router-dom';

const DashboardCard = ({ title, value, icon: Icon, color = 'blue', subtitle, to, searchParams }) => {
  const navigate = useNavigate();
  const colorMap = {
    blue: 'bg-blue-50 text-blue-600',
    green: 'bg-green-50 text-green-600',
    red: 'bg-red-50 text-red-600',
    yellow: 'bg-yellow-50 text-yellow-600',
    purple: 'bg-purple-50 text-purple-600',
  };

  const iconBg = colorMap[color] || colorMap.blue;
  const isClickable = Boolean(to);

  const handleClick = () => {
    if (!to) return;
    const query = searchParams ? `?${new URLSearchParams(searchParams).toString()}` : '';
    navigate(`${to}${query}`);
  };

  return (
    <div
      className={`card transition-shadow hover:shadow-md ${isClickable ? 'cursor-pointer hover:ring-2 hover:ring-blue-300' : ''}`}
      onClick={isClickable ? handleClick : undefined}
      role={isClickable ? 'button' : undefined}
      tabIndex={isClickable ? 0 : undefined}
      onKeyDown={isClickable ? (e) => { if (e.key === 'Enter' || e.key === ' ') handleClick(); } : undefined}
      title={isClickable ? `Go to ${title}` : undefined}
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500 mb-1">{title}</p>
          <h3 className="text-2xl font-bold text-gray-900">{value}</h3>
          {subtitle && <p className="text-xs text-gray-500 mt-2">{subtitle}</p>}
          {isClickable && <p className="text-xs text-blue-500 mt-1">Click to view →</p>}
        </div>
        {Icon && (
          <div className={`p-3 rounded-lg ${iconBg}`}>
            <Icon size={24} />
          </div>
        )}
      </div>
    </div>
  );
};

export default DashboardCard;
