import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { deleteWithApiEnvelope } from '../../../core/http/delete-with-api-envelope';
import { ApiResponse } from '../../../core/models/api-response';
import { PageResponse } from '../../plots/models/page-response.model';

/** Matches backend OwnerResponse — optional fields omitted by API for AGENT. */
export interface OwnerRegistryRecord {
  id: number;
  name: string;
  contactInfo?: string | null;
  cnic?: string | null;
}

export interface OwnerUpsertRequest {
  name: string;
  contactInfo: string;
  cnic: string;
}

export interface PageRequest {
  page: number;
  size: number;
  sort?: string[];
}

@Injectable({ providedIn: 'root' })
export class OwnersApiService {
  private readonly base = '/api/v1/owners';

  constructor(private readonly http: HttpClient) {}

  listOwners(req: PageRequest): Observable<PageResponse<OwnerRegistryRecord>> {
    return this.http
      .get<ApiResponse<PageResponse<OwnerRegistryRecord>>>(`${this.base}`, {
        params: this.pageParams(req, ['name,asc']),
      })
      .pipe(map((r) => this.unwrap(r)));
  }

  getOwner(id: number): Observable<OwnerRegistryRecord> {
    return this.http
      .get<ApiResponse<OwnerRegistryRecord>>(`${this.base}/${id}`)
      .pipe(map((r) => this.unwrap(r)));
  }

  createOwner(body: OwnerUpsertRequest): Observable<OwnerRegistryRecord> {
    return this.http
      .post<ApiResponse<OwnerRegistryRecord>>(`${this.base}`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  updateOwner(id: number, body: OwnerUpsertRequest): Observable<OwnerRegistryRecord> {
    return this.http
      .put<ApiResponse<OwnerRegistryRecord>>(`${this.base}/${id}`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  deleteOwner(id: number): Observable<void> {
    return deleteWithApiEnvelope(this.http, `${this.base}/${id}`);
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
