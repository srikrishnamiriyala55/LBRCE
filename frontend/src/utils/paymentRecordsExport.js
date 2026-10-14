const text = (value) => value === null || value === undefined || value === '' ? '—' : String(value);
const amount = (value) => Number(value || 0).toLocaleString('en-IN');
const dateTime = (value) => {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? text(value) : parsed.toLocaleString('en-IN');
};

const exportRows = (rows) => rows.map((row) => [
  row.rollNumber, row.studentName, row.branch, row.year, row.semester, row.phoneNumber, row.email,
  row.academicYear, dateTime(row.paidAt || row.createdAt), row.amount, row.transactionId,
  row.orderId, row.gatewayName, row.status, row.totalFee, row.paidTillNow, row.pendingAmount
]);

const headers = ['Roll Number', 'Student Name', 'Department', 'Year', 'Semester', 'Phone', 'Email',
  'Academic Year', 'Payment Date', 'Amount Paid', 'Transaction ID', 'Order / Receipt ID', 'Payment Mode',
  'Status', 'Total Fee', 'Paid Till Now', 'Pending'];

const safeDate = () => new Date().toISOString().slice(0, 10);
const csvCell = (value) => `"${text(value).replaceAll('"', '""')}"`;

export function downloadPaymentsCsv(rows) {
  const numericColumns = new Set([9, 14, 15, 16]);
  const body = exportRows(rows).map((row) => row.map((value, index) => csvCell(numericColumns.has(index) ? Number(value || 0) : value)).join(','));
  const csv = `\uFEFF${headers.map(csvCell).join(',')}\r\n${body.join('\r\n')}`;
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = `payment-records-${safeDate()}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

const readLogo = async () => {
  try {
    const response = await fetch('/logo.jpg');
    if (!response.ok) return null;
    const blob = await response.blob();
    return await new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  } catch { return null; }
};

export async function downloadPaymentsPdf(rows, filters = {}) {
  const { jsPDF } = await import('jspdf');
  const pdf = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
  const logo = await readLogo();
  const pageWidth = pdf.internal.pageSize.getWidth();
  const pageHeight = pdf.internal.pageSize.getHeight();
  const margin = 8;
  const bottom = pageHeight - 12;
  const widths = [18, 28, 16, 27, 24, 22, 25, 20, 26, 23, 23, 21];
  const pdfHeaders = ['Roll No.', 'Student', 'Dept / Year / Sem', 'Email / Phone', 'Paid Date', 'Amount',
    'Transaction ID', 'Mode', 'Total Fee', 'Paid Till Now', 'Pending', 'Status'];
  let y = 0;

  const drawPageHeader = () => {
    pdf.setFillColor(30, 64, 175); pdf.rect(0, 0, pageWidth, 27, 'F');
    if (logo) {
      try { pdf.setFillColor(255, 255, 255); pdf.roundedRect(margin, 2.5, 22, 22, 2, 2, 'F'); pdf.addImage(logo, 'JPEG', margin + 1.5, 4, 19, 19); } catch { /* text header remains */ }
    }
    pdf.setTextColor(255, 255, 255); pdf.setFont('helvetica', 'bold'); pdf.setFontSize(12);
    pdf.text('Lakireddy Bali Reddy College of Engineering (Autonomous)', 34, 8);
    pdf.setFontSize(9); pdf.text('Bus Transportation Management System — Payment Records', 34, 14.5);
    pdf.setFont('helvetica', 'normal'); pdf.setFontSize(6.5);
    const range = filters.fromDate || filters.toDate ? `Period: ${filters.fromDate || 'Beginning'} to ${filters.toDate || 'Today'}` : 'Period: All dates';
    pdf.text(`${range}  |  Records: ${rows.length}  |  Generated: ${new Date().toLocaleString('en-IN')}`, 34, 21);
    y = 31;
  };
  const drawTableHeader = () => {
    pdf.setFont('helvetica', 'bold'); pdf.setFontSize(5.5); pdf.setTextColor(255, 255, 255);
    let x = margin;
    pdfHeaders.forEach((header, index) => { pdf.setFillColor(37, 72, 145); pdf.rect(x, y, widths[index], 8, 'F'); pdf.text(pdf.splitTextToSize(header, widths[index] - 2)[0], x + 1, y + 5); x += widths[index]; });
    y += 8;
  };
  const newPage = () => { pdf.addPage(); drawPageHeader(); drawTableHeader(); };

  drawPageHeader(); drawTableHeader();
  rows.forEach((row, rowIndex) => {
    const cells = [row.rollNumber, row.studentName, `${text(row.branch)} / ${text(row.year)} / ${text(row.semester)}`,
      `${text(row.email)} / ${text(row.phoneNumber)}`, dateTime(row.paidAt || row.createdAt), `INR ${amount(row.amount)}`,
      row.transactionId, row.gatewayName, `INR ${amount(row.totalFee)}`, `INR ${amount(row.paidTillNow)}`,
      `INR ${amount(row.pendingAmount)}`, row.status];
    const lines = cells.map((cell, index) => pdf.splitTextToSize(text(cell), widths[index] - 2));
    const height = Math.max(7, Math.max(...lines.map((line) => line.length)) * 3 + 2);
    if (y + height > bottom) newPage();
    let x = margin; pdf.setFont('helvetica', 'normal'); pdf.setFontSize(5.5); pdf.setTextColor(30, 35, 45);
    lines.forEach((line, index) => { pdf.setFillColor(rowIndex % 2 ? 245 : 255, rowIndex % 2 ? 247 : 255, rowIndex % 2 ? 250 : 255); pdf.setDrawColor(220, 224, 230); pdf.rect(x, y, widths[index], height, 'FD'); pdf.text(line, x + 1, y + 4); x += widths[index]; });
    y += height;
  });
  const pages = pdf.getNumberOfPages();
  for (let page = 1; page <= pages; page += 1) { pdf.setPage(page); pdf.setFontSize(6); pdf.setTextColor(90, 96, 108); pdf.text(`Official LBRCE BTMS Payment Report | Page ${page} of ${pages}`, pageWidth / 2, pageHeight - 5, { align: 'center' }); }
  pdf.save(`payment-records-${safeDate()}.pdf`);
}
