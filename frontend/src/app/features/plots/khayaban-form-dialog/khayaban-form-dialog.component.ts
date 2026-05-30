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
import { MatSelectModule } from '@angular/material/select';
import { Khayaban } from '../models/khayaban.model';
import { Phase } from '../models/phase.model';

export type KhayabanFormMode = 'create' | 'edit';

export interface KhayabanFormDialogData {
  mode: KhayabanFormMode;
  phases: Phase[];
  defaultPhaseId?: number | null;
  khayaban?: Khayaban;
}

export interface KhayabanFormDialogResult {
  name: string;
  phaseId: number;
}

@Component({
  selector: 'app-khayaban-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './khayaban-form-dialog.component.html',
  styleUrl: './khayaban-form-dialog.component.scss',
})
export class KhayabanFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly ref = inject(MatDialogRef<KhayabanFormDialogComponent, KhayabanFormDialogResult | undefined>);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    phaseId: [null as number | null, Validators.required],
  });

  constructor(@Inject(MAT_DIALOG_DATA) readonly data: KhayabanFormDialogData) {
    if (data.mode === 'edit' && data.khayaban) {
      this.form.patchValue({
        name: data.khayaban.name,
        phaseId: data.khayaban.phaseId,
      });
    } else {
      const pid = data.defaultPhaseId ?? null;
      if (pid != null) {
        this.form.patchValue({ phaseId: pid });
      }
    }
  }

  get title(): string {
    return this.data.mode === 'edit' ? 'Edit khayaban' : 'Add khayaban';
  }

  get hasPhases(): boolean {
    return this.data.phases.length > 0;
  }

  submit(): void {
    if (this.form.invalid || !this.hasPhases) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.ref.close({
      name: v.name.trim(),
      phaseId: v.phaseId as number,
    });
  }

  cancel(): void {
    this.ref.close(undefined);
  }
}
