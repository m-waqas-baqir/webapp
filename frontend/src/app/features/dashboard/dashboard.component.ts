import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, map, tap } from 'rxjs/operators';
import { PERMISSION_CODES } from '../../core/constants/permission-codes';
import { UserRole } from '../../core/models/user-role';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { environment } from '../../../environments/environment';
import { ActivityLogRow, AdminApiService } from '../admin/services/admin-api.service';
import { UserManagementApiService } from '../admin/services/user-management-api.service';
import { OwnersApiService } from '../owners/services/owners-api.service';
import { PageResponse } from '../plots/models/page-response.model';
import { PlotsApiService } from '../plots/services/plots-api.service';
import { RentalsApiService } from '../rentals/services/rentals-api.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  standalone: false,
})
export class DashboardComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly adminApi = inject(AdminApiService);
  private readonly plotsApi = inject(PlotsApiService);
  private readonly rentalsApi = inject(RentalsApiService);
  private readonly ownersApi = inject(OwnersApiService);
  private readonly usersApi = inject(UserManagementApiService);
  private readonly notify = inject(NotificationService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly UserRole = UserRole;
  readonly appVersion = environment.appVersion;
  readonly PERMISSION_CODES = PERMISSION_CODES;

  /** Single load gate for headline metrics (avoids staggered flicker). */
  snapshotLoading = false;

  /** Staff & agent: plot inventory count (role-scoped by API). */
  plotTotal = 0;
  /** Staff & agent: rental listings count. */
  rentalTotal = 0;
  /** Staff: owner registry rows. */
  ownerTotal = 0;
  /** Staff: user directory size (all roles). */
  userDirectoryTotal = 0;
  /** Staff: audit events on record (same query as recent list). */
  activityLogTotal = 0;

  recentActivity: ActivityLogRow[] = [];
  agentActivity: ActivityLogRow[] = [];
  agentActivityLoading = false;

  ngOnInit(): void {
    if (this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN)) {
      this.loadStaffDashboard();
    } else if (this.auth.hasAnyRole(UserRole.AGENT)) {
      this.loadAgentDashboard();
      if (this.auth.hasPermission(PERMISSION_CODES.VIEW_ACTIVITY_LOGS)) {
        this.loadAgentActivity();
      }
    }
  }

  private emptyPage<T>(): PageResponse<T> {
    return {
      content: [],
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 0,
      first: true,
      last: true,
      empty: true,
    };
  }

  private loadStaffDashboard(): void {
    this.snapshotLoading = true;
    forkJoin({
      plots: this.plotsApi.listPlots({}, { page: 0, size: 1, sort: ['plotNumber,asc'] }).pipe(
        catchError(() => {
          this.notify.error('Could not load plot totals.');
          return of(this.emptyPage());
        }),
      ),
      rentals: this.rentalsApi.listRentalProperties({}, { page: 0, size: 1, sort: ['title,asc'] }).pipe(
        catchError(() => of(this.emptyPage())),
      ),
      owners: this.ownersApi.listOwners({ page: 0, size: 1, sort: ['name,asc'] }).pipe(
        catchError(() => of(this.emptyPage())),
      ),
      users: this.usersApi.listUsers({ page: 0, size: 1, sort: ['name,asc'] }).pipe(
        catchError(() => of(this.emptyPage())),
      ),
      activity: this.adminApi.listActivityLogs({ page: 0, size: 5, sort: ['occurredAt,desc'] }).pipe(
        catchError(() => of(this.emptyPage<ActivityLogRow>())),
      ),
    })
      .pipe(
        tap(({ plots, rentals, owners, users, activity }) => {
          this.plotTotal = plots.totalElements;
          this.rentalTotal = rentals.totalElements;
          this.ownerTotal = owners.totalElements;
          this.userDirectoryTotal = users.totalElements;
          this.activityLogTotal = activity.totalElements;
          this.recentActivity = activity.content;
        }),
        finalize(() => {
          this.snapshotLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe();
  }

  private loadAgentDashboard(): void {
    this.snapshotLoading = true;
    forkJoin({
      plots: this.plotsApi.listPlots({}, { page: 0, size: 1, sort: ['plotNumber,asc'] }).pipe(
        catchError(() => {
          this.notify.error('Could not load your plot queue.');
          return of(this.emptyPage());
        }),
      ),
      rentals: this.rentalsApi.listRentalProperties({}, { page: 0, size: 1, sort: ['title,asc'] }).pipe(
        catchError(() => of(this.emptyPage())),
      ),
    })
      .pipe(
        tap(({ plots, rentals }) => {
          this.plotTotal = plots.totalElements;
          this.rentalTotal = rentals.totalElements;
        }),
        finalize(() => {
          this.snapshotLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe();
  }

  private loadAgentActivity(): void {
    this.agentActivityLoading = true;
    this.adminApi
      .listActivityLogs({ page: 0, size: 5, sort: ['occurredAt,desc'] })
      .pipe(
        map((p) => p.content),
        catchError(() => of([] as ActivityLogRow[])),
        finalize(() => {
          this.agentActivityLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe((rows) => {
        this.agentActivity = rows;
      });
  }

  activitySummary(row: ActivityLogRow): string {
    return `${row.action} · ${row.entityType} #${row.entityId}`;
  }

  /** Combined “properties” headline for agents (plots + rentals in your scope). */
  get agentPropertyHeadline(): number {
    return this.plotTotal + this.rentalTotal;
  }
}
