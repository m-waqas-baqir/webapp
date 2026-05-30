import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

const SNACK_OPTS = {
  horizontalPosition: 'end' as const,
  verticalPosition: 'bottom' as const,
};

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string, durationMs = 4200): void {
    this.snackBar.open(message, 'Dismiss', {
      ...SNACK_OPTS,
      duration: durationMs,
      panelClass: ['app-snackbar', 'app-snackbar--success'],
    });
  }

  error(message: string, durationMs = 7000): void {
    this.snackBar.open(message, 'Dismiss', {
      ...SNACK_OPTS,
      duration: durationMs,
      panelClass: ['app-snackbar', 'app-snackbar--error'],
    });
  }

  info(message: string, durationMs = 5000): void {
    this.snackBar.open(message, 'Dismiss', {
      ...SNACK_OPTS,
      duration: durationMs,
      panelClass: ['app-snackbar', 'app-snackbar--info'],
    });
  }

  /** Maps HTTP failures (including backend ApiResponse envelopes) to user-facing text. */
  fromHttpError(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      const body = err.error;
      if (body && typeof body === 'object' && 'message' in body) {
        const m = (body as { message?: unknown }).message;
        if (typeof m === 'string' && m.trim().length > 0) {
          return m;
        }
      }
      if (err.status === 0) {
        return 'Unable to reach the server. Check your connection.';
      }
      if (err.status === 401) {
        return 'Sign in required or session expired.';
      }
      if (err.status === 403) {
        return 'You do not have permission for this action.';
      }
      if (err.status === 404) {
        return 'Resource not found.';
      }
      if (err.status >= 500) {
        return 'Server error. Please try again later.';
      }
    }
    if (err instanceof Error && err.message?.trim()) {
      return err.message;
    }
    return 'Something went wrong. Please try again.';
  }
}
