import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Prefixes relative `/api` and `/auth` requests with {@link environment.apiBaseUrl} when set.
 * When {@link environment.apiBaseUrl} is empty, URLs stay relative (Nginx / same-origin).
 */
@Injectable()
export class ApiBaseUrlInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (this.isAbsoluteUrl(req.url) || !this.needsBasePrefix(req.url)) {
      return next.handle(req);
    }

    const base = (environment.apiBaseUrl ?? '').replace(/\/$/, '');
    const path = req.url.startsWith('/') ? req.url : `/${req.url}`;
    const url = base ? `${base}${path}` : path;
    return next.handle(req.clone({ url }));
  }

  private needsBasePrefix(url: string): boolean {
    return url.startsWith('/api') || url.startsWith('/auth');
  }

  private isAbsoluteUrl(url: string): boolean {
    return /^https?:\/\//i.test(url);
  }
}
