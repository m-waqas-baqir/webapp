import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response';
import { UserRole } from '../../../core/models/user-role';
import { PageResponse } from '../../plots/models/page-response.model';
import { PermissionModule } from '../../../core/models/permission-module';

export interface ManagedUserListItem {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  active: boolean;
  permissionCount: number;
}

export interface ManagedUserDetail {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  active: boolean;
  permissions: string[];
}

export interface UserCreateRequest {
  name: string;
  email: string;
  password: string;
  role: UserRole;
  permissionCodes: string[];
}

export interface UserUpdateRequest {
  name?: string;
  email?: string;
  role?: UserRole;
  active?: boolean;
  newPassword?: string;
}

export interface UserPermissionsRequest {
  permissionCodes: string[];
}

export interface PermissionDefinition {
  code: string;
  description: string;
  module: PermissionModule;
}

export interface PageRequest {
  page: number;
  size: number;
  sort?: string[];
}

@Injectable({ providedIn: 'root' })
export class UserManagementApiService {
  constructor(private readonly http: HttpClient) {}

  listUsers(req: PageRequest): Observable<PageResponse<ManagedUserListItem>> {
    return this.http
      .get<ApiResponse<PageResponse<ManagedUserListItem>>>('/api/v1/users', {
        params: this.pageParams(req, ['name,asc']),
      })
      .pipe(map((r) => this.unwrap(r)));
  }

  getUser(id: number): Observable<ManagedUserDetail> {
    return this.http
      .get<ApiResponse<ManagedUserDetail>>(`/api/v1/users/${id}`)
      .pipe(map((r) => this.unwrap(r)));
  }

  createUser(body: UserCreateRequest): Observable<ManagedUserDetail> {
    return this.http
      .post<ApiResponse<ManagedUserDetail>>('/api/v1/users', body)
      .pipe(map((r) => this.unwrap(r)));
  }

  updateUser(id: number, body: UserUpdateRequest): Observable<ManagedUserDetail> {
    return this.http
      .put<ApiResponse<ManagedUserDetail>>(`/api/v1/users/${id}`, body)
      .pipe(map((r) => this.unwrap(r)));
  }

  deactivateUser(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`/api/v1/users/${id}`)
      .pipe(map((r) => this.unwrapVoid(r)));
  }

  replacePermissions(id: number, body: UserPermissionsRequest): Observable<void> {
    return this.http
      .put<ApiResponse<void>>(`/api/v1/users/${id}/permissions`, body)
      .pipe(map((r) => this.unwrapVoid(r)));
  }

  listPermissions(): Observable<PermissionDefinition[]> {
    return this.http
      .get<ApiResponse<PermissionDefinition[]>>('/api/v1/permissions')
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

  private unwrapVoid(res: ApiResponse<void>): void {
    if (!res.success) {
      throw new Error(res.message ?? 'Request failed');
    }
  }
}
