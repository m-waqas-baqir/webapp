import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../models/api-response';
import { PageResponse } from '../../features/plots/models/page-response.model';
import { UserRole } from '../models/user-role';

export interface UserSummary {
  id: number;
  name: string;
  email: string;
  role: UserRole;
}

export interface PageRequest {
  page: number;
  size: number;
  sort?: string[];
}

@Injectable({ providedIn: 'root' })
export class UsersDirectoryApiService {
  constructor(private readonly http: HttpClient) {}

  listByRole(role: UserRole, req: PageRequest): Observable<PageResponse<UserSummary>> {
    let params = new HttpParams().set('role', role).set('page', String(req.page)).set('size', String(req.size));
    const sorts = req.sort?.length ? req.sort : ['name,asc'];
    for (const s of sorts) {
      params = params.append('sort', s);
    }
    return this.http
      .get<ApiResponse<PageResponse<UserSummary>>>('/api/v1/user-directory', { params })
      .pipe(map((r) => this.unwrap(r)));
  }

  private unwrap<T>(res: ApiResponse<T>): T {
    if (!res.success || res.data === undefined) {
      throw new Error(res.message ?? 'Request failed');
    }
    return res.data;
  }
}
