import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Clears session and redirects to login when the API returns 401 (except login/register failures).
 */
@Injectable()
export class UnauthorizedInterceptor implements HttpInterceptor {
  private readonly auth = inject(AuthService);

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError((err: unknown) => {
        if (err instanceof HttpErrorResponse && err.status === 401 && !this.isAnonymousAuthPath(req.url)) {
          this.auth.handleUnauthorizedApiResponse();
        }
        return throwError(() => err);
      }),
    );
  }

  private isAnonymousAuthPath(url: string): boolean {
    return url.includes('/auth/login') || url.includes('/auth/register');
  }
}
