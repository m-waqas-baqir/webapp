import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  standalone: false,
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notify = inject(NotificationService);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  busy = false;
  errorMessage = '';

  private returnUrl = '/app';

  ngOnInit(): void {
    const qp = this.route.snapshot.queryParamMap;
    this.returnUrl = qp.get('returnUrl') ?? '/app';

    if (qp.get('sessionExpired') === '1') {
      this.notify.info('Your session expired. Please sign in again.');
      const ru = qp.get('returnUrl');
      void this.router.navigate(['/login'], {
        queryParams: ru ? { returnUrl: ru } : {},
        replaceUrl: true,
      });
      return;
    }

    if (this.auth.isAuthenticated()) {
      void this.router.navigateByUrl(this.returnUrl);
    }
  }

  onForgotPassword(): void {
    this.notify.info('Password reset is not available yet — please contact support.');
  }

  submit(): void {
    this.errorMessage = '';
    if (this.form.invalid || this.busy) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, password } = this.form.getRawValue();
    this.busy = true;
    this.auth.login(email, password).subscribe({
      next: () => {
        this.busy = false;
        void this.router.navigateByUrl(this.returnUrl);
      },
      error: (err: unknown) => {
        this.busy = false;
        this.errorMessage = this.formatLoginError(err);
      },
    });
  }

  private formatLoginError(err: unknown): string {
    if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object') {
      const body = err.error as { message?: string };
      if (typeof body.message === 'string' && body.message.length > 0) {
        return body.message;
      }
    }
    return 'Sign-in failed';
  }
}
