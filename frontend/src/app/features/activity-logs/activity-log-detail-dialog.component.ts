import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import type { ActivityLogRow } from '../admin/services/admin-api.service';

export interface ActivityLogDetailDialogData {
  row: ActivityLogRow;
  summary: string;
}

@Component({
  selector: 'app-activity-log-detail-dialog',
  templateUrl: './activity-log-detail-dialog.component.html',
  styleUrl: './activity-log-detail-dialog.component.scss',
  standalone: false,
})
export class ActivityLogDetailDialogComponent {
  constructor(
    readonly dialogRef: MatDialogRef<ActivityLogDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) readonly data: ActivityLogDetailDialogData,
  ) {}
}
