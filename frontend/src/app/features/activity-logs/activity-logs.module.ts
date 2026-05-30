import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { SharedModule } from '../../shared/shared.module';
import { ActivityLogDetailDialogComponent } from './activity-log-detail-dialog.component';
import { ActivityLogsRoutingModule } from './activity-logs-routing.module';
import { ActivityLogsComponent } from './activity-logs.component';

@NgModule({
  declarations: [ActivityLogsComponent, ActivityLogDetailDialogComponent],
  imports: [
    SharedModule,
    ReactiveFormsModule,
    ActivityLogsRoutingModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSortModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatDialogModule,
    MatDividerModule,
  ],
})
export class ActivityLogsModule {}
