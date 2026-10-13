const value = (input) => input === null || input === undefined || input === '' ? '-' : String(input);
const money = (input) => `INR ${Number(input || 0).toLocaleString('en-IN')}`;
const formatDate = (input) => {
  if (!input) return '-';
  const parsed = new Date(input);
  return Number.isNaN(parsed.getTime()) ? value(input) : parsed.toLocaleDateString('en-IN');
};

const readImage = async (url) => {
  if (!url) return null;
  if (url.startsWith('data:')) return url;
  try {
    const response = await fetch(url);
    if (!response.ok) return null;
    const blob = await response.blob();
    return await new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  } catch {
    return null;
  }
};

const imageFormat = (dataUrl) => dataUrl?.startsWith('data:image/png') ? 'PNG' : 'JPEG';

export async function downloadCompleteStudentHistoryPdf(data, photoUrl) {
  if (!data?.student) throw new Error('Student history is not available');
  const { jsPDF } = await import('jspdf');
  const pdf = new jsPDF({ unit: 'mm', format: 'a4' });
  const student = data.student;
  const histories = Array.isArray(data.history) ? data.history : [];
  const pageWidth = pdf.internal.pageSize.getWidth();
  const pageHeight = pdf.internal.pageSize.getHeight();
  const margin = 13;
  const contentWidth = pageWidth - (margin * 2);
  const bottom = pageHeight - 15;
  let y = 14;

  const logo = await readImage('/logo.jpg');

  const newPage = () => { pdf.addPage(); y = 14; };
  const ensure = (height) => { if (y + height > bottom) newPage(); };
  const sectionTitle = (title, count) => {
    // Keep the section label with at least a table header and one data row.
    ensure(24);
    pdf.setFillColor(226, 235, 250);
    pdf.roundedRect(margin, y, contentWidth, 8, 1.2, 1.2, 'F');
    pdf.setFont('helvetica', 'bold');
    pdf.setFontSize(10);
    pdf.setTextColor(24, 62, 125);
    pdf.text(`${title}${count === undefined ? '' : ` (${count})`}`, margin + 3, y + 5.4);
    pdf.setTextColor(25, 25, 25);
    y += 10;
  };
  const table = ({ headers, rows, ratios, x = margin, width = contentWidth, empty = 'No records available' }) => {
    const widths = ratios.map((ratio) => width * ratio);
    const fontSize = headers.length >= 6 ? 6.6 : 7.2;
    const padding = 1.4;
    const lineHeight = 3.3;
    const headerHeight = 7;
    const drawHeader = () => {
      pdf.setFillColor(37, 72, 145);
      pdf.setTextColor(255, 255, 255);
      pdf.setFont('helvetica', 'bold');
      pdf.setFontSize(fontSize);
      let cellX = x;
      headers.forEach((header, index) => {
        pdf.setFillColor(37, 72, 145);
        pdf.rect(cellX, y, widths[index], headerHeight, 'F');
        pdf.setTextColor(255, 255, 255);
        pdf.text(pdf.splitTextToSize(header, widths[index] - (padding * 2))[0], cellX + padding, y + 4.6);
        cellX += widths[index];
      });
      pdf.setTextColor(25, 25, 25);
      y += headerHeight;
    };
    ensure(headerHeight + 7);
    drawHeader();
    if (!rows.length) {
      pdf.setFillColor(249, 250, 252);
      pdf.setDrawColor(218, 223, 231);
      pdf.rect(x, y, width, 8, 'FD');
      pdf.setFont('helvetica', 'italic');
      pdf.setFontSize(7.5);
      pdf.setTextColor(105, 112, 125);
      pdf.text(empty, x + padding, y + 5.2);
      pdf.setTextColor(25, 25, 25);
      y += 10;
      return;
    }
    rows.forEach((row, rowIndex) => {
      const lines = row.map((cell, index) => pdf.splitTextToSize(value(cell), widths[index] - (padding * 2)));
      const rowHeight = Math.max(7, Math.max(...lines.map((cellLines) => cellLines.length)) * lineHeight + (padding * 2));
      if (y + rowHeight > bottom) { newPage(); drawHeader(); }
      pdf.setFillColor(rowIndex % 2 === 0 ? 249 : 241, rowIndex % 2 === 0 ? 250 : 245, rowIndex % 2 === 0 ? 252 : 250);
      pdf.setDrawColor(218, 223, 231);
      pdf.setFont('helvetica', 'normal');
      pdf.setFontSize(fontSize);
      let cellX = x;
      lines.forEach((cellLines, index) => {
        pdf.setFillColor(rowIndex % 2 === 0 ? 249 : 241, rowIndex % 2 === 0 ? 250 : 245, rowIndex % 2 === 0 ? 252 : 250);
        pdf.rect(cellX, y, widths[index], rowHeight, 'FD');
        pdf.setTextColor(25, 25, 25);
        pdf.text(cellLines, cellX + padding, y + padding + 2.2);
        cellX += widths[index];
      });
      y += rowHeight;
    });
    y += 3;
  };

  pdf.setFillColor(26, 62, 133);
  pdf.rect(0, 0, pageWidth, 31, 'F');
  if (logo) {
    try {
      pdf.setFillColor(255, 255, 255);
      pdf.roundedRect(margin, 3.5, 24, 24, 2, 2, 'F');
      pdf.addImage(logo, imageFormat(logo), margin + 1.5, 5, 21, 21);
    } catch {
      // The official text header remains visible if a browser cannot decode the logo.
    }
  }
  pdf.setTextColor(255, 255, 255);
  pdf.setFont('helvetica', 'bold');
  pdf.setFontSize(12.5);
  pdf.text('Lakireddy Bali Reddy College of Engineering (Autonomous)', margin + 29, 10);
  pdf.setFontSize(9.5);
  pdf.text('Bus Transportation Management System', margin + 29, 17);
  pdf.setFontSize(8.5);
  pdf.text('Complete Student History Report', margin + 29, 23);
  pdf.setFont('helvetica', 'normal');
  pdf.setFontSize(6.5);
  pdf.text(`Generated: ${new Date().toLocaleString('en-IN')}`, margin + 29, 28);
  pdf.setTextColor(25, 25, 25);
  y = 37;

  sectionTitle('Student Information');
  const photoWidth = 31;
  const detailsWidth = contentWidth - photoWidth - 5;
  table({
    x: margin,
    width: detailsWidth,
    headers: ['Field', 'Information'],
    ratios: [0.3, 0.7],
    rows: [
      ['Name', student.name], ['Roll Number', student.rollNumber], ['Account Status', student.status],
      ['Email', student.email], ['Phone Number', student.phoneNumber], ['Date of Birth', formatDate(student.dob)],
      ['Gender / Blood Group', `${value(student.gender)} / ${value(student.bloodGroup)}`],
      ['Parent / Guardian', student.parentName], ['Parent Phone', student.parentPhoneNumber], ['Address', student.address]
    ]
  });
  const detailsBottom = y;
  const photoTop = 49;
  pdf.setDrawColor(160, 170, 185);
  pdf.setFillColor(247, 248, 250);
  pdf.roundedRect(margin + detailsWidth + 5, photoTop, photoWidth, 40, 1.5, 1.5, 'FD');
  const photo = await readImage(photoUrl);
  if (photo) {
    try {
      pdf.addImage(photo, imageFormat(photo), margin + detailsWidth + 7, photoTop + 2, photoWidth - 4, 36);
    } catch {
      pdf.setFontSize(7); pdf.text('Photo unavailable', margin + detailsWidth + 8, photoTop + 21);
    }
  } else {
    pdf.setFont('helvetica', 'normal'); pdf.setFontSize(7); pdf.setTextColor(110, 115, 125);
    pdf.text('Photo unavailable', margin + detailsWidth + 8, photoTop + 21);
    pdf.setTextColor(25, 25, 25);
  }
  y = Math.max(detailsBottom, photoTop + 43);

  if (!histories.length) {
    sectionTitle('Academic and Transport History');
    table({ headers: ['Academic Year', 'Status'], ratios: [0.5, 0.5], rows: [] });
  }

  histories.forEach((year, yearIndex) => {
    if (yearIndex > 0) newPage();
    sectionTitle(`Academic Year ${value(year.academicYear)}${year.active ? ' - Active' : ''}`);
    table({
      headers: ['Department', 'Study Year', 'Semester', 'Academic Status'],
      ratios: [0.34, 0.2, 0.2, 0.26],
      rows: [[year.branch, year.year, year.semester, year.academicStatus]]
    });

    sectionTitle('Applications', year.applications?.length || 0);
    table({
      headers: ['Applied', 'Bus', 'Route', 'Boarding Point', 'Status'],
      ratios: [0.13, 0.13, 0.29, 0.25, 0.2],
      rows: (year.applications || []).map((item) => [formatDate(item.applicationDate || item.createdAt), item.busNumber, `${value(item.startingPoint)} - ${value(item.endingPoint)}`, item.boardingPointName, item.status])
    });

    sectionTitle('Bus Allocations', year.allocations?.length || 0);
    table({
      headers: ['Bus', 'Route', 'Boarding Point', 'In-charge', 'Allocated', 'Status'],
      ratios: [0.1, 0.24, 0.18, 0.2, 0.13, 0.15],
      rows: (year.allocations || []).map((item) => [item.busNumber, `${value(item.startingPoint)} - ${value(item.endingPoint)}`, item.boardingPoint, `${value(item.inchargeName)} (${value(item.inchargeTeacherId)})`, formatDate(item.allocatedAt), item.status])
    });

    sectionTitle('Fee Records', year.fees?.length || 0);
    table({
      headers: ['Total Fee', 'Paid Amount', 'Pending Amount', 'Status'],
      ratios: [0.25, 0.25, 0.25, 0.25],
      rows: (year.fees || []).map((item) => [money(item.totalAmount), money(item.paidAmount), money(item.remainingAmount), item.status])
    });

    sectionTitle('Payment Transactions', year.payments?.length || 0);
    table({
      headers: ['Date', 'Receipt / Order ID', 'Method', 'Amount', 'Status'],
      ratios: [0.15, 0.31, 0.18, 0.18, 0.18],
      rows: (year.payments || []).map((item) => [formatDate(item.paidAt || item.createdAt), item.orderId, item.paymentMethod, money(item.amount), item.status])
    });

    sectionTitle('Bus Passes', year.passes?.length || 0);
    table({
      headers: ['Pass Number', 'Bus', 'Boarding Point', 'Valid From', 'Valid Until', 'Status'],
      ratios: [0.19, 0.12, 0.21, 0.16, 0.16, 0.16],
      rows: (year.passes || []).map((item) => [item.passNumber, item.busNumber, item.boardingPoint, formatDate(item.validFrom), formatDate(item.validUntil), item.status])
    });

    sectionTitle('Transfer History', year.transfers?.length || 0);
    table({
      headers: ['Requested', 'From Bus', 'To Bus', 'Reason / Remarks', 'Status'],
      ratios: [0.14, 0.14, 0.14, 0.4, 0.18],
      rows: (year.transfers || []).map((item) => [formatDate(item.requestedAt || item.createdAt), item.currentBusNumber, item.requestedBusNumber, `${value(item.reason)} / ${value(item.remarks)}`, item.status])
    });
  });

  const totalPages = pdf.getNumberOfPages();
  for (let page = 1; page <= totalPages; page += 1) {
    pdf.setPage(page);
    pdf.setDrawColor(215, 220, 228);
    pdf.line(margin, pageHeight - 11, pageWidth - margin, pageHeight - 11);
    pdf.setFont('helvetica', 'normal');
    pdf.setFontSize(7.5);
    pdf.setTextColor(95, 102, 115);
    pdf.text(`LBRCE BTMS | ${value(student.rollNumber)} | Page ${page} of ${totalPages}`, pageWidth / 2, pageHeight - 7, { align: 'center' });
  }

  const safeRollNumber = value(student.rollNumber).replace(/[^a-zA-Z0-9_-]/g, '_');
  pdf.save(`student-history-${safeRollNumber}.pdf`);
}
