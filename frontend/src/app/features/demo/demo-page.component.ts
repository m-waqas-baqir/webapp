import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { distinctUntilChanged, map } from 'rxjs';
import { UserRole } from '../../core/models/user-role';
import { AuthService } from '../../core/services/auth.service';
import { parseDemoRoleParam, userRoleToDemoKey } from './demo-role';

@Component({
  selector: 'app-demo-page',
  templateUrl: './demo-page.component.html',
  styleUrl: './demo-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DemoPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);
  readonly UserRole = UserRole;

  private readonly paramRole = toSignal(
    this.route.paramMap.pipe(
      map((p) => parseDemoRoleParam(p.get('role'))),
      distinctUntilChanged(),
    ),
    { initialValue: parseDemoRoleParam(this.route.snapshot.paramMap.get('role')) },
  );

  /** Effective workspace role for training copy (same as live session). */
  readonly role = computed(() => this.auth.user()?.role ?? UserRole.AGENT);

  readonly isAgent = computed(() => this.role() === UserRole.AGENT);
  readonly isDirector = computed(() => this.role() === UserRole.DIRECTOR);
  readonly isStaff = computed(() =>
    this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN),
  );

  readonly mainTitle = computed(() => {
    switch (this.role()) {
      case UserRole.DIRECTOR:
        return 'Director Training — Start Here';
      case UserRole.ADMIN:
        return 'Admin Training — Start Here';
      default:
        return 'Agent Training — Start Here';
    }
  });

  readonly purposeLine = computed(() => {
    switch (this.role()) {
      case UserRole.DIRECTOR:
        return 'Learn how to monitor activity and manage the system.';
      case UserRole.ADMIN:
        return 'Learn how to oversee operations, review activity, and support your team.';
      default:
        return 'Learn how to manage your assigned properties.';
    }
  });

  readonly viewingModeLabel = computed(() => {
    switch (this.role()) {
      case UserRole.DIRECTOR:
        return 'Director Training Mode';
      case UserRole.ADMIN:
        return 'Admin Training Mode';
      default:
        return 'Agent Training Mode';
    }
  });

  constructor() {
    effect(() => {
      this.auth.user();
      const param = this.paramRole();
      const userKey = userRoleToDemoKey(this.auth.user()?.role);
      if (param != null && param !== userKey) {
        void this.router.navigate(['/app', 'demo'], { replaceUrl: true });
      }
    });
  }
}
