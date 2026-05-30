import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response';
import { PageResponse } from '../../plots/models/page-response.model';

export interface ActivityLogRow {
  id: number;
  userId: number;
  actorEmail: string;
  action: string;
  entityType: string;
  entityId: number;
  occurredAt: string;
}

/** Matches OwnerFullDetailResponse (director report). */
export interface ListingOwnerDetailRow {
  listingId: number;
  listingTitle: string;
  ownerName: string;
  ownerEmail: string;
  assignedAgentId?: number | null;
}

export interface PageRequest {
  page: number;
  size: number;
  sort?: string[];
}

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  constructor(private readonly http: HttpClient) {}

  listActivityLogs(req: PageRequest): Observable<PageResponse<ActivityLogRow>> {
    return this.http
      .get<ApiResponse<PageResponse<ActivityLogRow>>>('/api/v1/admin/activity-logs', {
        params: this.pageParams(req, ['occurredAt,desc']),
      })
      .pipe(map((r) => this.unwrap(r)));
  }

  directorListingOwnerReport(): Observable<ListingOwnerDetailRow[]> {
    return this.http
      .get<ApiResponse<ListingOwnerDetailRow[]>>('/api/v1/reports/listings/owners')
      .pipe(map((r) => this.unwrap(r)));
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
