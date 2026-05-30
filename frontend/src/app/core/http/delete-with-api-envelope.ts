import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../models/api-response';

/**
 * DELETE requests often return an empty body behind reverse proxies, which breaks {@link HttpClient}'s default
 * JSON parser. We read the raw body as text and parse the {@link ApiResponse} envelope only when present.
 */
export function deleteWithApiEnvelope(http: HttpClient, url: string): Observable<void> {
  return http.delete(url, { observe: 'response', responseType: 'text' }).pipe(
    map((resp) => {
      if (resp.status < 200 || resp.status >= 300) {
        throw new Error('Request failed');
      }
      const raw = resp.body?.trim() ?? '';
      if (!raw) {
        return;
      }
      let parsed: ApiResponse<unknown>;
      try {
        parsed = JSON.parse(raw) as ApiResponse<unknown>;
      } catch {
        throw new Error('Invalid server response');
      }
      if (!parsed.success) {
        throw new Error(parsed.message ?? 'Delete failed');
      }
    }),
  );
}
