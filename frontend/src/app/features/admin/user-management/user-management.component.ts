import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { UserRole } from '../../../core/models/user-role';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  ManagedUserListItem,
  UserManagementApiService,
} from '../services/user-management-api.service';
import {
  UserFormDialogComponent,
  UserFormDialogData,
} from './user-form-dialog/user-form-dialog.component';

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.scss',
  standalone: false,
})
export class UserManagementComponent implements OnInit {
  private readonly api = inject(UserManagementApiService);
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly UserRole = UserRole;

  displayedColumns = ['name', 'email', 'role', 'status', 'permissionCount', 'actions'];
  dataSource = new MatTableDataSource<ManagedUserListItem>([]);
  loading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  @ViewChild(MatPaginator) paginator?: MatPaginator;

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage(): void {
    this.loading = true;
    this.api
      .listUsers({ page: this.pageIndex, size: this.pageSize })
      .subscribe({
        next: (page) => {
          this.dataSource.data = page.content;
          this.totalElements = page.totalElements;
          this.loading = false;
        },
        error: (e) => {
          this.notify.error(this.notify.fromHttpError(e));
          this.loading = false;
        },
      });
  }

  onPage(ev: PageEvent): void {
    this.pageIndex = ev.pageIndex;
    this.pageSize = ev.pageSize;
    this.loadPage();
  }

  openCreate(): void {
    const ref = this.dialog.open<UserFormDialogComponent, UserFormDialogData, boolean>(
      UserFormDialogComponent,
      {
        width: 'min(680px, 96vw)',
        data: { mode: 'create' },
      },
    );
    ref.afterClosed().subscribe((ok) => {
      if (ok) {
        this.loadPage();
      }
    });
  }

  openEdit(row: ManagedUserListItem): void {
    const ref = this.dialog.open<UserFormDialogComponent, UserFormDialogData, boolean>(
      UserFormDialogComponent,
      {
        width: 'min(680px, 96vw)',
        data: { mode: 'edit', userId: row.id },
      },
    );
    ref.afterClosed().subscribe((ok) => {
      if (ok) {
        this.loadPage();
      }
    });
  }

  deactivate(row: ManagedUserListItem): void {
    const selfId = this.auth.user()?.id;
    if (selfId === row.id) {
      this.notify.error('You cannot deactivate your own account.');
      return;
    }
    if (!confirm(`Deactivate ${row.email}? They will no longer be able to sign in.`)) {
      return;
    }
    this.api.deactivateUser(row.id).subscribe({
      next: () => {
        this.notify.success('User deactivated.');
        this.loadPage();
      },
      error: (e) => this.notify.error(this.notify.fromHttpError(e)),
    });
  }

  canDeactivate(row: ManagedUserListItem): boolean {
    return this.auth.user()?.id !== row.id;
  }
}
