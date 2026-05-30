import { Component, Inject, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { catchError, finalize, of, tap } from 'rxjs';
import {
  OwnerRegistryRecord,
  OwnerUpsertRequest,
  OwnersApiService,
} from '../services/owners-api.service';
import { NotificationService } from '../../../core/services/notification.service';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReactiveFormsModule } from '@angular/forms';

export interface OwnerFormDialogData {
  mode: 'create' | 'edit';
  owner?: OwnerRegistryRecord;
}

@Component({
  selector: 'app-owner-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './owner-form-dialog.component.html',
  styleUrl: './owner-form-dialog.component.scss',
})
export class OwnerFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(OwnersApiService);
  private readonly notify = inject(NotificationService);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    contactInfo: ['', [Validators.required, Validators.maxLength(500)]],
    cnic: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(20)]],
  });

  loading = false;

  constructor(
    private readonly ref: MatDialogRef<OwnerFormDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) readonly data: OwnerFormDialogData,
  ) {}

  ngOnInit(): void {
    if (this.data.mode === 'edit' && this.data.owner) {
      this.loading = true;
      this.api
        .getOwner(this.data.owner.id)
        .pipe(
          tap((o) => {
            this.form.patchValue({
              name: o.name,
              contactInfo: o.contactInfo ?? '',
              cnic: o.cnic ?? '',
            });
          }),
          catchError((e) => {
            this.notify.error(this.notify.fromHttpError(e));
            this.ref.close(false);
            return of(null);
          }),
          finalize(() => {
            this.loading = false;
          }),
        )
        .subscribe();
    }
  }

  cancel(): void {
    this.ref.close(false);
  }

  submit(): void {
    if (this.form.invalid || this.loading) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body: OwnerUpsertRequest = {
      name: v.name.trim(),
      contactInfo: v.contactInfo.trim(),
      cnic: v.cnic.trim(),
    };
    this.loading = true;
    const req$ =
      this.data.mode === 'create'
        ? this.api.createOwner(body)
        : this.api.updateOwner(this.data.owner!.id, body);
    req$
      .pipe(
        tap(() => {
          this.notify.success(this.data.mode === 'create' ? 'Owner created' : 'Owner updated');
          this.ref.close(true);
        }),
        catchError((e) => {
          this.notify.error(this.notify.fromHttpError(e));
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
        }),
      )
      .subscribe();
  }
}
