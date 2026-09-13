import React from 'react';

const StatusBadge = ({ status }) => {
  if (!status) return null;

  let colorClass = 'bg-gray-100 text-gray-800';
  const s = status.toUpperCase();

  if (['ACTIVE', 'APPROVED', 'SUCCESS', 'PAID', 'RESOLVED'].includes(s)) {
    colorClass = 'bg-green-100 text-green-800';
  } else if (['PENDING', 'UNDER_REVIEW', 'INITIATED', 'PARTIAL'].includes(s)) {
    colorClass = 'bg-yellow-100 text-yellow-800';
  } else if (['REJECTED', 'FAILED', 'CANCELLED', 'UNPAID'].includes(s)) {
    colorClass = 'bg-red-100 text-red-800';
  } else if (['OPEN', 'IN_PROGRESS'].includes(s)) {
    colorClass = 'bg-blue-100 text-blue-800';
  }

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${colorClass}`}>
      {status.replace(/_/g, ' ')}
    </span>
  );
};

export default StatusBadge;
