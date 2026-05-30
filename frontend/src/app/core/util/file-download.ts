/**
 * Parse RFC 5987 / simple filename from Content-Disposition, e.g.
 * attachment; filename="plots-export.xlsx"
 */
export function filenameFromContentDisposition(header: string | null): string | undefined {
  if (!header) {
    return undefined;
  }
  const m = /filename\*?=(?:UTF-8''|")?([^";\n]+)/i.exec(header);
  if (m?.[1]) {
    return decodeURIComponent(m[1].replace(/"/g, '').trim());
  }
  return undefined;
}

export function triggerBlobDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.rel = 'noopener';
  a.click();
  URL.revokeObjectURL(url);
}
