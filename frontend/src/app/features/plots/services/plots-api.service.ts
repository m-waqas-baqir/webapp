import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { deleteWithApiEnvelope } from '../../../core/http/delete-with-api-envelope';
import { ApiResponse } from '../../../core/models/api-response';
import { BulkImportResult } from '../../../core/models/bulk-import-result.model';
import { filenameFromContentDisposition, triggerBlobDownload } from '../../../core/util/file-download';
import { Khayaban } from '../models/khayaban.model';
import { PageResponse } from '../models/page-response.model';
import { Phase } from '../models/phase.model';
import { Plot, PlotUpsertRequest } from '../models/plot.model';
import { PlotStatus } from '../models/plot-status';

export interface PageRequest {
  page: number;
  size: number;
  sort?: string[];
}

@Injectable({ providedIn: 'root' })
export class PlotsApiService {
  private readonly base = '/api/v1';

  constructor(private readonly http: HttpClient) {}

  listPhases(req: PageRequest): Observable<PageResponse<Phase>> {
    return this.http
      .get<ApiResponse<PageResponse<Phase>>>(`${this.base}/phases`, {
        params: this.pageParams(req, ['name,asc']),
      })
      .pipe(map((r) => this.unwrap(r)));
  }

  listKhayabans(phaseId: number | undefined, req: PageRequest): Observable<PageResponse<Khayaban>> {
    let params = this.pageParams(req, ['name,asc']);
    if (phaseId != null) {
      params = params.set('phaseId', String(phaseId));
    }
    return this.http
      .get<ApiResponse<PageResponse<Khayaban>>>(`${this.base}/khayabans`, { params })
      .pipe(map((r) => this.unwrap(r)));
  }

  listPlots(
    filters: {
      phaseId?: number;
      khayabanId?: number;
      status?: PlotStatus;
      minPrice?: number;
      maxPrice?: number;
      minSize?: number;
      maxSize?: number;
    },
    req: PageRequest,
  ): Observable<PageResponse<Plot>> {
    let params = this.pageParams(req, ['plotNumber,asc']);
    if (filters.phaseId != null) {
      params = params.set('phaseId', String(filters.phaseId));
    }
    if (filters.khayabanId != null) {
      params = params.set('khayabanId', String(filters.khayabanId));
    }
    if (filters.status != null) {
      params = params.set('status', filters.status);
    }
    if (filters.minPrice != null) {
      params = params.set('minPrice', String(filters.minPrice));
    }
    if (filters.maxPrice != null) {
      params = params.set('maxPrice', String(filters.maxPrice));
    }
    if (filters.minSize != null) {
      params = params.set('minSize', String(filters.minSize));
    }
    if (filters.maxSize != null) {
      params = params.set('maxSize', String(filters.maxSize));
    }
    return this.http
      .get<ApiResponse<PageResponse<Plot>>>(`${this.base}/plots`, { params })
      .pipe(map((r) => this.unwrap(r)));
  }

  getPlot(id: number): Observable<Plot> {
    return this.http
      .get<ApiResponse<Plot>>(`${this.base}/plots/${id}`)
      .pipe(map((r) => this.unwrap(r)));
  }

  createPlot(body: PlotUpsertRequest): Observable<Plot> {
    return this.http
      .post<ApiResponse<Plot>>(`${this.base}/plots`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  updatePlot(id: number, body: PlotUpsertRequest): Observable<Plot> {
    return this.http
      .put<ApiResponse<Plot>>(`${this.base}/plots/${id}`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  deletePlot(id: number): Observable<void> {
    return deleteWithApiEnvelope(this.http, `${this.base}/plots/${id}`);
  }

  getPhase(id: number): Observable<Phase> {
    return this.http
      .get<ApiResponse<Phase>>(`${this.base}/phases/${id}`)
      .pipe(map((r) => this.unwrap(r)));
  }

  createPhase(body: { name: string }): Observable<Phase> {
    return this.http
      .post<ApiResponse<Phase>>(`${this.base}/phases`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  updatePhase(id: number, body: { name: string }): Observable<Phase> {
    return this.http
      .put<ApiResponse<Phase>>(`${this.base}/phases/${id}`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  deletePhase(id: number): Observable<void> {
    return deleteWithApiEnvelope(this.http, `${this.base}/phases/${id}`);
  }

  getKhayaban(id: number): Observable<Khayaban> {
    return this.http
      .get<ApiResponse<Khayaban>>(`${this.base}/khayabans/${id}`)
      .pipe(map((r) => this.unwrap(r)));
  }

  createKhayaban(body: { name: string; phaseId: number }): Observable<Khayaban> {
    return this.http
      .post<ApiResponse<Khayaban>>(`${this.base}/khayabans`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  updateKhayaban(id: number, body: { name: string; phaseId: number }): Observable<Khayaban> {
    return this.http
      .put<ApiResponse<Khayaban>>(`${this.base}/khayabans/${id}`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  deleteKhayaban(id: number): Observable<void> {
    return deleteWithApiEnvelope(this.http, `${this.base}/khayabans/${id}`);
  }

  importPlotsExcel(file: File): Observable<BulkImportResult> {
    const body = new FormData();
    body.append('file', file);
    return this.http
      .post<ApiResponse<BulkImportResult>>(`${this.base}/plots/import`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  /** Triggers browser download; completes when the file has been handed off. */
  exportPlotsExcel(): Observable<void> {
    return this.http
      .get(`${this.base}/plots/export`, { responseType: 'blob', observe: 'response' })
      .pipe(map((res: HttpResponse<Blob>) => this.saveExportBlob(res, 'plots-export.xlsx')));
  }

  downloadPlotsImportTemplate(): Observable<void> {
    return this.http
      .get(`${this.base}/plots/template`, { responseType: 'blob', observe: 'response' })
      .pipe(map((res: HttpResponse<Blob>) => this.saveExportBlob(res, 'plots-import-template.xlsx')));
  }

  importKhayabansExcel(file: File): Observable<BulkImportResult> {
    const body = new FormData();
    body.append('file', file);
    return this.http
      .post<ApiResponse<BulkImportResult>>(`${this.base}/khayabans/import`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  exportKhayabansExcel(): Observable<void> {
    return this.http
      .get(`${this.base}/khayabans/export`, { responseType: 'blob', observe: 'response' })
      .pipe(map((res: HttpResponse<Blob>) => this.saveExportBlob(res, 'khayabans-export.xlsx')));
  }

  downloadKhayabansImportTemplate(): Observable<void> {
    return this.http
      .get(`${this.base}/khayabans/template`, { responseType: 'blob', observe: 'response' })
      .pipe(map((res: HttpResponse<Blob>) => this.saveExportBlob(res, 'khayabans-import-template.xlsx')));
  }

  private saveExportBlob(res: HttpResponse<Blob>, fallbackName: string): void {
    const body = res.body;
    if (!body || body.size === 0) {
      throw new Error('Empty export file');
    }
    const fn =
      filenameFromContentDisposition(res.headers.get('content-disposition')) ?? fallbackName;
    triggerBlobDownload(body, fn);
  }

  private pageParams(req: PageRequest, defaultSort: string[]): HttpParams {
    let p = new HttpParams().set('page', String(req.page)).set('size', String(req.size));
    const sorts = req.sort?.length ? req.sort : defaultSort;
    for (const s of sorts) {
      p = p.append('sort', s);
    }
    return p;
  }

  private unwrap<T>(res: ApiResponse<T>): T {
    if (!res.success || res.data === undefined) {
      throw new Error(res.message ?? 'Request failed');
    }
    return res.data;
  }
}
