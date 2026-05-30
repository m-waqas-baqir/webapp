import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Attaches {@code Authorization: Bearer} for API calls; skips login/register.
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private readonly auth = inject(AuthService);

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (this.isAnonymousAuthCall(req.url)) {
      return next.handle(req);
    }
    const token = this.auth.getAccessToken();
    if (!token) {
      return next.handle(req);
    }
    return next.handle(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }

  private isAnonymousAuthCall(url: string): boolean {
    return url.includes('/auth/login') || url.includes('/auth/register');
  }
}
