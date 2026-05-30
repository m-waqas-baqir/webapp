import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { deleteWithApiEnvelope } from '../http/delete-with-api-envelope';
import { ApiResponse } from '../models/api-response';
import { LinkedEntityType } from '../models/linked-entity-type';
import { PropertyImage } from '../models/property-image.model';

@Injectable({ providedIn: 'root' })
export class PropertyImagesApiService {
  private readonly base = '/api/v1/property-images';

  constructor(private readonly http: HttpClient) {}

  list(entityType: LinkedEntityType, entityId: number): Observable<PropertyImage[]> {
    const params = new HttpParams().set('entityType', entityType).set('entityId', String(entityId));
    return this.http
      .get<ApiResponse<PropertyImage[]>>(this.base, { params })
      .pipe(map((r) => this.unwrap(r)));
  }

  upload(entityType: LinkedEntityType, entityId: number, file: File): Observable<PropertyImage> {
    const form = new FormData();
    form.append('file', file);
    const params = new HttpParams().set('entityType', entityType).set('entityId', String(entityId));
    return this.http
      .post<ApiResponse<PropertyImage>>(this.base, form, { params })
      .pipe(map((r) => this.unwrap(r)));
  }

  delete(id: number): Observable<void> {
    return deleteWithApiEnvelope(this.http, `${this.base}/${id}`);
  }

  /** Download bytes (JWT sent by interceptor) for preview. */
  getFileBlob(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/${id}/file`, { responseType: 'blob' });
  }

  private unwrap<T>(res: ApiResponse<T>): T {
    if (!res.success || res.data === undefined) {
      throw new Error(res.message ?? 'Request failed');
    }
    return res.data;
  }
}
