import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, map, tap, throwError } from 'rxjs';
import { ApiResponse } from '../models/api-response';
import { AuthResponseDto } from '../models/auth-response.model';
import { AuthUser } from '../models/auth-user.model';
import { UserRole } from '../models/user-role';

const STORAGE_TOKEN = 'rp_access_token';
const STORAGE_USER = 'rp_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userSignal = signal<AuthUser | null>(this.readUserFromStorage());

  /** Current user or null when logged out. */
  readonly user = computed(() => this.userSignal());

  /**
   * Must read {@link userSignal} so this recomputes after login/logout.
   * Otherwise the first evaluation (often on the login page with no token) stays cached forever.
   */
  readonly isAuthenticated = computed(() => {
    this.userSignal();
    const token = this.getAccessToken();
    if (!token) {
      return false;
    }
    const exp = this.readJwtExp(token);
    if (exp != null && Date.now() >= exp * 1000) {
      return false;
    }
    return true;
  });

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {
    const token = this.getAccessToken();
    if (token) {
      const exp = this.readJwtExp(token);
      if (exp != null && Date.now() >= exp * 1000) {
        this.clearSession();
      } else if (!this.userSignal()) {
        void this.refreshProfile();
      }
    }
  }

  login(email: string, password: string): Observable<AuthUser> {
    const body = { email: email.trim(), password };
    return this.http
      .post<ApiResponse<AuthResponseDto>>('/auth/login', body)
      .pipe(
        map((res) => this.unwrap(res)),
        tap((dto) => this.persistFromAuthResponse(dto)),
        map((dto) => this.mapUser(dto)),
        catchError((err) => throwError(() => err)),
      );
  }

  logout(navigateToLogin = true): void {
    this.clearSession();
    if (navigateToLogin) {
      void this.router.navigate(['/login']);
    }
  }

  /**
   * Invoked when a protected API returns 401 (expired or revoked JWT).
   * Avoids treating login/register failures as session expiry.
   */
  handleUnauthorizedApiResponse(): void {
    const hadSession = this.getAccessToken() != null;
    this.clearSession();
    if (hadSession) {
      void this.router.navigate(['/login'], { queryParams: { sessionExpired: '1' } });
    }
  }

  getAccessToken(): string | null {
    return localStorage.getItem(STORAGE_TOKEN);
  }

  hasAnyRole(...roles: UserRole[]): boolean {
    const u = this.userSignal();
    if (!u) {
      return false;
    }
    return roles.includes(u.role);
  }

  /**
   * Fine-grained capability hint for navigation/UI only.
   * Always enforce authorization on the server.
   */
  hasPermission(code: string): boolean {
    const u = this.userSignal();
    if (!u?.active) {
      return false;
    }
    return u.permissions?.includes(code) ?? false;
  }

  /** Reload profile from API (e.g. after token refresh later). */
  refreshProfile(): Promise<void> {
    const token = this.getAccessToken();
    if (!token) {
      return Promise.resolve();
    }
    return new Promise((resolve) => {
      this.http.get<ApiResponse<AuthUser>>('/api/v1/me').subscribe({
        next: (res) => {
          const u = res.data;
          if (u && res.success) {
            localStorage.setItem(STORAGE_USER, JSON.stringify(u));
            this.userSignal.set(u);
          }
          resolve();
        },
        error: () => resolve(),
      });
    });
  }

  private unwrap(res: ApiResponse<AuthResponseDto>): AuthResponseDto {
    if (!res.success || !res.data) {
      throw new Error(res.message ?? 'Authentication failed');
    }
    return res.data;
  }

  private persistFromAuthResponse(dto: AuthResponseDto): void {
    localStorage.setItem(STORAGE_TOKEN, dto.accessToken);
    const user: AuthUser = this.mapUser(dto);
    localStorage.setItem(STORAGE_USER, JSON.stringify(user));
    this.userSignal.set(user);
  }

  private mapUser(dto: AuthResponseDto): AuthUser {
    return {
      id: dto.userId,
      email: dto.email,
      name: dto.name,
      role: dto.role,
      active: dto.active ?? true,
      permissions: Array.isArray(dto.permissions) ? dto.permissions : [],
    };
  }

  private clearSession(): void {
    localStorage.removeItem(STORAGE_TOKEN);
    localStorage.removeItem(STORAGE_USER);
    this.userSignal.set(null);
  }

  private readUserFromStorage(): AuthUser | null {
    const raw = localStorage.getItem(STORAGE_USER);
    if (!raw) {
      return null;
    }
    try {
      const parsed = JSON.parse(raw) as Partial<AuthUser>;
      if (typeof parsed.id !== 'number' || !parsed.email) {
        return null;
      }
      return {
        id: parsed.id,
        email: parsed.email,
        name: parsed.name ?? '',
        role: parsed.role as AuthUser['role'],
        active: parsed.active ?? true,
        permissions: Array.isArray(parsed.permissions) ? parsed.permissions : [],
      };
    } catch {
      return null;
    }
  }

  /** Seconds since epoch, JWT exp claim */
  private readJwtExp(token: string): number | null {
    try {
      const payload = token.split('.')[1];
      if (!payload) {
        return null;
      }
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const json = JSON.parse(atob(base64)) as { exp?: number };
      return typeof json.exp === 'number' ? json.exp : null;
    } catch {
      return null;
    }
  }
}
