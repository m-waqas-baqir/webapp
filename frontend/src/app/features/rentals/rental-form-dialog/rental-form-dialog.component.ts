import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { finalize } from 'rxjs';
import { UserRole } from '../../../core/models/user-role';
import { UsersDirectoryApiService, type UserSummary } from '../../../core/services/users-directory-api.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { OwnersApiService, type OwnerRegistryRecord } from '../../owners/services/owners-api.service';
import { RentalProperty, RentalPropertyUpsertRequest } from '../models/rental-property.model';
import { RENTAL_STATUS_OPTIONS, RentalPropertyStatus } from '../models/rental-property-status';
import { RENTAL_TYPE_OPTIONS, RentalPropertyType } from '../models/rental-property-type';
import { RentalsApiService } from '../services/rentals-api.service';

export interface RentalFormDialogData {
  rental?: RentalProperty;
}

@Component({
  selector: 'app-rental-form-dialog',
  templateUrl: './rental-form-dialog.component.html',
  styleUrl: './rental-form-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
})
export class RentalFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(RentalsApiService);
  private readonly ownersApi = inject(OwnersApiService);
  private readonly usersDir = inject(UsersDirectoryApiService);
  private readonly notify = inject(NotificationService);
  readonly auth = inject(AuthService);

  readonly UserRole = UserRole;
  readonly typeOptions = RENTAL_TYPE_OPTIONS;
  readonly statusOptions = RENTAL_STATUS_OPTIONS;

  readonly form: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    type: [RentalPropertyType.RESIDENTIAL, Validators.required],
    address: ['', [Validators.required, Validators.maxLength(500)]],
    rentAmount: [null as number | null, [Validators.required, Validators.min(0)]],
    status: [RentalPropertyStatus.AVAILABLE, Validators.required],
    ownerId: [null as number | null],
    assignedAgentId: [null as number | null],
  });

  owners: OwnerRegistryRecord[] = [];
  agents: UserSummary[] = [];
  loadingOwners = false;
  loadingAgents = false;
  saving = false;
  readonly isEdit: boolean;

  constructor(
    private readonly ref: MatDialogRef<RentalFormDialogComponent, RentalProperty | undefined>,
    @Inject(MAT_DIALOG_DATA) readonly data: RentalFormDialogData,
  ) {
    this.isEdit = !!data.rental?.id;
  }

  ngOnInit(): void {
    if (this.canEditAssignments) {
      this.form.get('ownerId')?.addValidators(Validators.required);
      this.loadOwners();
      this.loadAgents();
    }
    this.form.get('ownerId')?.updateValueAndValidity();

    const r = this.data.rental;
    if (r) {
      this.form.patchValue(
        {
          title: r.title,
          type: r.type,
          address: r.address,
          rentAmount: Number(r.rentAmount),
          status: r.status,
          ownerId: r.ownerId ?? null,
          assignedAgentId: r.assignedAgentId ?? null,
        },
        { emitEvent: false },
      );
    }
  }

  get canEditAssignments(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN);
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue() as {
      title: string;
      type: RentalPropertyType;
      address: string;
      rentAmount: number;
      status: RentalPropertyStatus;
      ownerId: number | null;
      assignedAgentId: number | null;
    };

    const body: RentalPropertyUpsertRequest = {
      title: v.title.trim(),
      type: v.type,
      address: v.address.trim(),
      rentAmount: v.rentAmount,
      status: v.status,
    };
    if (this.canEditAssignments) {
      body.ownerId = v.ownerId ?? undefined;
      body.assignedAgentId = v.assignedAgentId ?? undefined;
    }

    this.saving = true;
    const req$ = this.isEdit
      ? this.api.updateRentalProperty(this.data.rental!.id, body)
      : this.api.createRentalProperty(body);

    req$
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (rental) => this.ref.close(rental),
        error: (err: unknown) => this.notify.error(this.notify.fromHttpError(err)),
      });
  }

  cancel(): void {
    this.ref.close(undefined);
  }

  private loadOwners(): void {
    this.loadingOwners = true;
    this.ownersApi
      .listOwners({ page: 0, size: 500, sort: ['name,asc'] })
      .pipe(finalize(() => (this.loadingOwners = false)))
      .subscribe({
        next: (page) => {
          this.owners = page.content;
        },
        error: (e: unknown) => this.notify.error(this.notify.fromHttpError(e)),
      });
  }

  private loadAgents(): void {
    this.loadingAgents = true;
    this.usersDir
      .listByRole(UserRole.AGENT, { page: 0, size: 500, sort: ['name,asc'] })
      .pipe(finalize(() => (this.loadingAgents = false)))
      .subscribe({
        next: (page) => {
          this.agents = page.content;
        },
        error: (e: unknown) => this.notify.error(this.notify.fromHttpError(e)),
      });
  }
}
