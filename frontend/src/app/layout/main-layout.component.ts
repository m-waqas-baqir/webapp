import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { ChangeDetectorRef, Component, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatSidenav } from '@angular/material/sidenav';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs';
import { PERMISSION_CODES } from '../core/constants/permission-codes';
import { UserRole } from '../core/models/user-role';
import { AuthService } from '../core/services/auth.service';
import { routeFadeSlide } from '../shared/animations/fade.animation';

export interface MainNavItem {
  label: string;
  route: string;
  icon: string;
  /** When set, item is shown only if the user has one of these roles. */
  roles?: UserRole[];
  /** When set (e.g. for agents), item is shown only if the user has this permission. */
  permission?: string;
}

/** Workflow-based sidenav group (intent over module list). */
export interface MainNavGroup {
  id: string;
  /** Section heading; omit for a flat list at the top. */
  label: string | null;
  items: MainNavItem[];
}

@Component({
  selector: 'app-main-layout',
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
  standalone: false,
  animations: [routeFadeSlide],
})
export class MainLayoutComponent {
  @ViewChild('drawer') drawer?: MatSidenav;

  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly bp = inject(BreakpointObserver);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly contentRouteKey = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  readonly isHandset = signal(false);
  readonly sidenavOpen = signal(true);

  /** Staff: workflow groups — Properties, then oversight, then account. */
  private readonly staffNavGroupDefs: MainNavGroup[] = [
    {
      id: 'overview',
      label: null,
      items: [{ label: 'Dashboard', route: '/app/dashboard', icon: 'dashboard' }],
    },
    {
      id: 'properties',
      label: 'Properties',
      items: [
        { label: 'Plots', route: '/app/plots', icon: 'map' },
        {
          label: 'Plot hierarchy',
          route: '/app/plots/hierarchy',
          icon: 'account_tree',
          roles: [UserRole.DIRECTOR, UserRole.ADMIN],
        },
        { label: 'Rentals', route: '/app/rentals', icon: 'home_work' },
        { label: 'Owners', route: '/app/owners', icon: 'badge' },
      ],
    },
    {
      id: 'oversight',
      label: 'Oversight',
      items: [
        {
          label: 'Activity Logs',
          route: '/app/activity-logs',
          icon: 'history',
          roles: [UserRole.DIRECTOR, UserRole.ADMIN],
        },
        {
          label: 'Reports',
          route: '/app/admin/reports',
          icon: 'assessment',
          roles: [UserRole.DIRECTOR],
        },
        {
          label: 'Users',
          route: '/app/admin/users',
          icon: 'group',
          roles: [UserRole.DIRECTOR, UserRole.ADMIN],
        },
      ],
    },
    {
      id: 'resources',
      label: 'Resources',
      items: [{ label: 'Training hub', route: '/app/demo', icon: 'school' }],
    },
    {
      id: 'account',
      label: null,
      items: [{ label: 'Account', route: '/app/auth', icon: 'person' }],
    },
  ];

  /** Agent: focused work surface + optional directory access. */
  private readonly agentNavGroupDefs: MainNavGroup[] = [
    {
      id: 'work',
      label: 'Your work',
      items: [
        { label: 'Dashboard', route: '/app/dashboard', icon: 'dashboard' },
        { label: 'My Properties', route: '/app/my-properties', icon: 'home_work' },
      ],
    },
    {
      id: 'directory',
      label: 'Directory',
      items: [
        {
          label: 'Owners',
          route: '/app/owners',
          icon: 'badge',
          permission: PERMISSION_CODES.OWNER_VIEW,
        },
      ],
    },
    {
      id: 'resources',
      label: 'Resources',
      items: [{ label: 'Training hub', route: '/app/demo', icon: 'school' }],
    },
    {
      id: 'account',
      label: null,
      items: [{ label: 'Account', route: '/app/auth', icon: 'person' }],
    },
  ];

  constructor() {
    this.bp
      .observe('(max-width: 959px)')
      .pipe(takeUntilDestroyed())
      .subscribe((state: BreakpointState) => {
        this.isHandset.set(state.matches);
        if (state.matches) {
          this.sidenavOpen.set(false);
        } else {
          this.sidenavOpen.set(true);
        }
        this.cdr.markForCheck();
      });
  }

  visibleNavGroups(): MainNavGroup[] {
    const role = this.auth.user()?.role;
    const defs = role === UserRole.AGENT ? this.agentNavGroupDefs : this.staffNavGroupDefs;
    return defs
      .map((g) => ({
        ...g,
        items: g.items.filter((item) => this.itemVisible(item)),
      }))
      .filter((g) => g.items.length > 0);
  }

  private itemVisible(item: MainNavItem): boolean {
    if (item.roles?.length && !this.auth.hasAnyRole(...item.roles)) {
      return false;
    }
    if (item.permission && !this.auth.hasPermission(item.permission)) {
      return false;
    }
    return true;
  }

  logout(): void {
    this.auth.logout();
  }

  onSidenavOpenedChange(open: boolean): void {
    this.sidenavOpen.set(open);
  }

  toggleSidenav(): void {
    this.sidenavOpen.update((o) => !o);
  }
}
