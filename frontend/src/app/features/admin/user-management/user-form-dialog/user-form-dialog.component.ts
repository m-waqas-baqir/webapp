import { Component, Inject, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ReactiveFormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';
import { UserRole } from '../../../../core/models/user-role';
import { PermissionModule } from '../../../../core/models/permission-module';
import { PERMISSION_CODES } from '../../../../core/constants/permission-codes';
import { AuthService } from '../../../../core/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
import {
  PermissionDefinition,
  UserManagementApiService,
} from '../../services/user-management-api.service';

export interface UserFormDialogData {
  mode: 'create' | 'edit';
  userId?: number;
}

const RESTRICTED_FOR_ADMIN = new Set<string>([
  PERMISSION_CODES.USER_MANAGE,
  PERMISSION_CODES.ASSIGN_AGENT,
]);

@Component({
  selector: 'app-user-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSlideToggleModule,
  ],
  templateUrl: './user-form-dialog.component.html',
  styleUrl: './user-form-dialog.component.scss',
})
export class UserFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(UserManagementApiService);
  private readonly notify = inject(NotificationService);
  readonly auth = inject(AuthService);

  readonly UserRole = UserRole;
  readonly PermissionModule = PermissionModule;
  readonly permissionCodes = PERMISSION_CODES;

 /** Dialog open / data fetch only — keeps form visible while saving. */
  bootstrapping = false;
  saving = false;
  catalog: PermissionDefinition[] = [];
  /** Module order for section headers (USER last before SYSTEM is fine). */
  readonly moduleOrder: PermissionModule[] = [
    PermissionModule.OWNER,
    PermissionModule.PLOT,
    PermissionModule.USER,
    PermissionModule.SYSTEM,
  ];

  private selected = new Set<string>();

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
    password: [''],
    newPassword: [''],
    role: [UserRole.AGENT as UserRole, Validators.required],
    active: [true],
  });

  constructor(
    private readonly ref: MatDialogRef<UserFormDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) readonly data: UserFormDialogData,
  ) {
    if (data.mode === 'create') {
      this.form.controls.password.setValidators([Validators.required, Validators.minLength(8), Validators.maxLength(120)]);
    } else {
      this.form.controls.password.clearValidators();
      this.form.controls.newPassword.setValidators([Validators.maxLength(120)]);
    }
  }

  ngOnInit(): void {
    this.bootstrapping = true;
    if (this.data.mode === 'create') {
      this.api
        .listPermissions()
        .pipe(
          tap((c) => (this.catalog = c)),
          catchError((e) => {
            this.notify.error(this.notify.fromHttpError(e));
            this.ref.close(false);
            return of(null);
          }),
          finalize(() => {
            this.bootstrapping = false;
          }),
        )
        .subscribe();
    } else if (this.data.userId != null) {
      forkJoin({
        detail: this.api.getUser(this.data.userId),
        perms: this.api.listPermissions(),
      })
        .pipe(
          tap(({ detail, perms }) => {
            this.catalog = perms;
            this.form.patchValue({
              name: detail.name,
              email: detail.email,
              role: detail.role,
              active: detail.active,
            });
            this.selected = new Set(detail.permissions);
          }),
          catchError((e) => {
            this.notify.error(this.notify.fromHttpError(e));
            this.ref.close(false);
            return of(null);
          }),
          finalize(() => {
            this.bootstrapping = false;
          }),
        )
        .subscribe();
    } else {
      this.bootstrapping = false;
    }
  }

  isDirector(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR);
  }

  isPermissionDisabled(def: PermissionDefinition): boolean {
    if (this.isDirector()) {
      return false;
    }
    return RESTRICTED_FOR_ADMIN.has(def.code);
  }

  permissionTooltip(def: PermissionDefinition): string {
    if (!this.isPermissionDisabled(def)) {
      return def.description;
    }
    return `${def.description} — Only a Director can assign this permission.`;
  }

  isChecked(code: string): boolean {
    return this.selected.has(code);
  }

  togglePermission(def: PermissionDefinition, checked: boolean): void {
    if (this.isPermissionDisabled(def)) {
      return;
    }
    if (checked) {
      this.selected.add(def.code);
    } else {
      this.selected.delete(def.code);
    }
  }

  permissionsForModule(mod: PermissionModule): PermissionDefinition[] {
    return this.catalog.filter((p) => p.module === mod).sort((a, b) => a.code.localeCompare(b.code));
  }

  moduleTitle(mod: PermissionModule): string {
    switch (mod) {
      case PermissionModule.OWNER:
        return 'Owner';
      case PermissionModule.PLOT:
        return 'Plot';
      case PermissionModule.USER:
        return 'User';
      case PermissionModule.SYSTEM:
        return 'System';
      default:
        return mod;
    }
  }

  cancel(): void {
    this.ref.close(false);
  }

  submit(): void {
    if (this.form.invalid || this.saving || this.bootstrapping) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    if (this.data.mode === 'create') {
      this.saving = true;
      this.api
        .createUser({
          name: v.name.trim(),
          email: v.email.trim().toLowerCase(),
          password: v.password,
          role: v.role,
          permissionCodes: this.sanitizedSelection(),
        })
        .pipe(
          finalize(() => {
            this.saving = false;
          }),
        )
        .subscribe({
          next: () => {
            this.notify.success('User created.');
            this.ref.close(true);
          },
          error: (e) => this.notify.error(this.notify.fromHttpError(e)),
        });
      return;
    }
    if (this.data.userId == null) {
      return;
    }
    const id = this.data.userId;
    const updateBody: {
      name?: string;
      email?: string;
      role?: UserRole;
      active?: boolean;
      newPassword?: string;
    } = {
      name: v.name.trim(),
      email: v.email.trim().toLowerCase(),
      role: v.role,
      active: v.active,
    };
    const np = v.newPassword?.trim();
    if (np) {
      if (np.length < 8) {
        this.notify.error('New password must be at least 8 characters.');
        return;
      }
      updateBody.newPassword = np;
    }
    this.saving = true;
    this.api
      .updateUser(id, updateBody)
      .pipe(
        switchMap(() =>
          this.api.replacePermissions(id, { permissionCodes: this.sanitizedSelection() }),
        ),
        finalize(() => {
          this.saving = false;
        }),
      )
      .subscribe({
        next: () => {
          this.notify.success('User updated.');
          this.ref.close(true);
        },
        error: (e) => this.notify.error(this.notify.fromHttpError(e)),
      });
  }

  private sanitizedSelection(): string[] {
    const codes = Array.from(this.selected);
    if (this.isDirector()) {
      return codes;
    }
    return codes.filter((c) => !RESTRICTED_FOR_ADMIN.has(c));
  }
}
