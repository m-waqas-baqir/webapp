import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface PlotsConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
}

@Component({
  selector: 'app-plots-confirm-dialog',
  templateUrl: './plots-confirm-dialog.component.html',
  styleUrl: './plots-confirm-dialog.component.scss',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
})
export class PlotsConfirmDialogComponent {
  constructor(
    private readonly ref: MatDialogRef<PlotsConfirmDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) readonly data: PlotsConfirmDialogData,
  ) {}

  cancel(): void {
    this.ref.close(false);
  }

  confirm(): void {
    this.ref.close(true);
  }
}
