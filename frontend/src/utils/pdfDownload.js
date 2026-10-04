/**
 * Captures a DOM element and downloads it as a PDF that looks exactly
 * like the on-screen view (pixel-perfect, single page, centered).
 *
 * @param {string} elementId - The id of the element to capture (e.g. "bus-pass")
 * @param {string} filename  - The filename without extension (e.g. "bus-pass-22B01A0101")
 */
export async function downloadElementAsPdf(elementId, filename) {
  const { default: html2canvas } = await import('html2canvas');
  const { jsPDF } = await import('jspdf');

  const element = document.getElementById(elementId);
  if (!element) throw new Error(`Element #${elementId} not found`);

  const canvas = await html2canvas(element, {
    scale: 2.5,          // 2.5× for sharp, high-resolution output
    useCORS: true,
    allowTaint: true,
    backgroundColor: '#ffffff',
    logging: false,
  });

  const imgData = canvas.toDataURL('image/png', 1.0);
  const imgW = canvas.width;
  const imgH = canvas.height;

  // Create single-page A4 document and center the pass card
  const isLandscape = imgW > imgH * 1.2;
  const pdf = new jsPDF({
    orientation: isLandscape ? 'landscape' : 'portrait',
    unit: 'mm',
    format: 'a4',
  });

  const pageW = pdf.internal.pageSize.getWidth();
  const pageH = pdf.internal.pageSize.getHeight();
  const margin = 14;
  const maxW = pageW - margin * 2;
  const maxH = pageH - margin * 2;

  const ratio = Math.min(maxW / imgW, maxH / imgH);
  const drawW = imgW * ratio;
  const drawH = imgH * ratio;

  // Center horizontally and vertically on the single page
  const x = (pageW - drawW) / 2;
  const y = Math.max(margin, (pageH - drawH) / 2);

  pdf.addImage(imgData, 'PNG', x, y, drawW, drawH, undefined, 'FAST');
  pdf.save(`${filename}.pdf`);
}
