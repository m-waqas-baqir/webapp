import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { catchError, finalize, of, tap } from 'rxjs';
import { UserRole } from '../../../core/models/user-role';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  OwnerRegistryRecord,
  OwnersApiService,
  PageRequest,
} from '../services/owners-api.service';
import { OwnerFormDialogComponent, OwnerFormDialogData } from '../owner-form-dialog/owner-form-dialog.component';
import { PlotsConfirmDialogComponent, PlotsConfirmDialogData } from '../../plots/plots-confirm-dialog/plots-confirm-dialog.component';

@Component({
  selector: 'app-owners-dashboard',
  templateUrl: './owners-dashboard.component.html',
  styleUrl: './owners-dashboard.component.scss',
  standalone: false,
})
export class OwnersDashboardComponent implements OnInit {
  private readonly api = inject(OwnersApiService);
  private readonly notify = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly cdr = inject(ChangeDetectorRef);
  readonly auth = inject(AuthService);
  readonly UserRole = UserRole;

  owners: OwnerRegistryRecord[] = [];
  loading = false;
  totalElements = 0;
  pageSize = 15;
  pageIndex = 0;
  readonly pageSizeOptions = [10, 15, 25, 50];

  readonly displayedColumns = (): string[] => {
    const cols: string[] = ['id', 'name'];
    if (this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN)) {
      cols.push('contactInfo', 'cnic');
    }
    if (this.canMutate()) {
      cols.push('actions');
    }
    return cols;
  };

  ngOnInit(): void {
    this.load();
  }

  canMutate(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN);
  }

  load(): void {
    this.loading = true;
    const req: PageRequest = {
      page: this.pageIndex,
      size: this.pageSize,
      sort: ['name,asc'],
    };
    this.api
      .listOwners(req)
      .pipe(
        tap((p) => {
          this.owners = p.content;
          this.totalElements = p.totalElements;
        }),
        catchError((e) => {
          this.notify.error(this.notify.fromHttpError(e));
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe();
  }

  onPage(ev: PageEvent): void {
    this.pageIndex = ev.pageIndex;
    this.pageSize = ev.pageSize;
    this.load();
  }

  openCreate(): void {
    const ref = this.dialog.open(OwnerFormDialogComponent, {
      width: '520px',
      data: { mode: 'create' } satisfies OwnerFormDialogData,
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.load();
      }
    });
  }

  openEdit(row: OwnerRegistryRecord): void {
    const ref = this.dialog.open(OwnerFormDialogComponent, {
      width: '520px',
      data: { mode: 'edit', owner: row } satisfies OwnerFormDialogData,
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.load();
      }
    });
  }

  deleteOwner(row: OwnerRegistryRecord): void {
    const ref = this.dialog.open(PlotsConfirmDialogComponent, {
      data: {
        title: 'Delete owner record',
        message: `Remove owner registry row #${row.id}?`,
        confirmLabel: 'Delete',
      } as PlotsConfirmDialogData,
    });
    ref.afterClosed().subscribe((ok) => {
      if (!ok) {
        return;
      }
      this.api
        .deleteOwner(row.id)
        .pipe(
          tap(() => {
            this.notify.success('Owner deleted');
            this.load();
          }),
          catchError((e) => {
            this.notify.error(this.notify.fromHttpError(e));
            return of(null);
          }),
        )
        .subscribe();
    });
  }
}
