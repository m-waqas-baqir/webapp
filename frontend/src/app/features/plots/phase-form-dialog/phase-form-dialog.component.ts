import { CommonModule } from '@angular/common';
import { Component, Inject, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export type PhaseFormMode = 'create' | 'edit';

export interface PhaseFormDialogData {
  mode: PhaseFormMode;
  initialName?: string;
}

@Component({
  selector: 'app-phase-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './phase-form-dialog.component.html',
  styleUrl: './phase-form-dialog.component.scss',
})
export class PhaseFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly ref = inject(MatDialogRef<PhaseFormDialogComponent, string | undefined>);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
  });

  constructor(@Inject(MAT_DIALOG_DATA) readonly data: PhaseFormDialogData) {
    if (data.mode === 'edit' && data.initialName) {
      this.form.patchValue({ name: data.initialName });
    }
  }

  get title(): string {
    return this.data.mode === 'edit' ? 'Edit phase' : 'Add phase';
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.ref.close(this.form.getRawValue().name.trim());
  }

  cancel(): void {
    this.ref.close(undefined);
  }
}
