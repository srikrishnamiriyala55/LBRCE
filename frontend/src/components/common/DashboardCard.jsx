import React from 'react';

const DashboardCard = ({ title, value, icon: Icon, color = 'blue', subtitle }) => {
  const colorMap = {
    blue: 'bg-blue-50 text-blue-600',
    green: 'bg-green-50 text-green-600',
    red: 'bg-red-50 text-red-600',
    yellow: 'bg-yellow-50 text-yellow-600',
    purple: 'bg-purple-50 text-purple-600',
  };
  
  const iconBg = colorMap[color] || colorMap.blue;

  return (
    <div className="card hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500 mb-1">{title}</p>
          <h3 className="text-2xl font-bold text-gray-900">{value}</h3>
          {subtitle && <p className="text-xs text-gray-500 mt-2">{subtitle}</p>}
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
