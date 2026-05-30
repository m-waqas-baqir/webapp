import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { deleteWithApiEnvelope } from '../../../core/http/delete-with-api-envelope';
import { ApiResponse } from '../../../core/models/api-response';
import { BulkImportResult } from '../../../core/models/bulk-import-result.model';
import { filenameFromContentDisposition, triggerBlobDownload } from '../../../core/util/file-download';
import { PageResponse } from '../models/page-response.model';
import { RentalProperty, RentalPropertyUpsertRequest } from '../models/rental-property.model';
import { RentalPropertyStatus } from '../models/rental-property-status';
import { RentalPropertyType } from '../models/rental-property-type';

export interface PageRequest {
  page: number;
  size: number;
  sort?: string[];
}

@Injectable({ providedIn: 'root' })
export class RentalsApiService {
  private readonly base = '/api/v1/rental-properties';

  constructor(private readonly http: HttpClient) {}

  listRentalProperties(
    filters: {
      type?: RentalPropertyType;
      status?: RentalPropertyStatus;
      minRent?: number;
      maxRent?: number;
    },
    req: PageRequest,
  ): Observable<PageResponse<RentalProperty>> {
    let params = this.pageParams(req, ['title,asc']);
    if (filters.type != null) {
      params = params.set('type', filters.type);
    }
    if (filters.status != null) {
      params = params.set('status', filters.status);
    }
    if (filters.minRent != null) {
      params = params.set('minRent', String(filters.minRent));
    }
    if (filters.maxRent != null) {
      params = params.set('maxRent', String(filters.maxRent));
    }
    return this.http
      .get<ApiResponse<PageResponse<RentalProperty>>>(this.base, { params })
      .pipe(map((r) => this.unwrap(r)));
  }

  getRentalProperty(id: number): Observable<RentalProperty> {
    return this.http.get<ApiResponse<RentalProperty>>(`${this.base}/${id}`).pipe(map((r) => this.unwrap(r)));
  }

  createRentalProperty(body: RentalPropertyUpsertRequest): Observable<RentalProperty> {
    return this.http
      .post<ApiResponse<RentalProperty>>(this.base, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  updateRentalProperty(id: number, body: RentalPropertyUpsertRequest): Observable<RentalProperty> {
    return this.http
      .put<ApiResponse<RentalProperty>>(`${this.base}/${id}`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  deleteRentalProperty(id: number): Observable<void> {
    return deleteWithApiEnvelope(this.http, `${this.base}/${id}`);
  }

  importRentalsExcel(file: File): Observable<BulkImportResult> {
    const body = new FormData();
    body.append('file', file);
    return this.http
      .post<ApiResponse<BulkImportResult>>(`${this.base}/import`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  exportRentalsExcel(): Observable<void> {
    return this.http
      .get(`${this.base}/export`, { responseType: 'blob', observe: 'response' })
      .pipe(map((res: HttpResponse<Blob>) => this.saveExportBlob(res, 'rentals-export.xlsx')));
  }

  downloadRentalsImportTemplate(): Observable<void> {
    return this.http
      .get(`${this.base}/template`, { responseType: 'blob', observe: 'response' })
      .pipe(map((res: HttpResponse<Blob>) => this.saveExportBlob(res, 'rentals-import-template.xlsx')));
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
