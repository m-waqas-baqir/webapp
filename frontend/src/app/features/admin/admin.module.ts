import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { SharedModule } from '../../shared/shared.module';
import { AdminHubComponent } from './admin-hub/admin-hub.component';
import { AdminRoutingModule } from './admin-routing.module';
import { DirectorReportComponent } from './director-report/director-report.component';
import { UserFormDialogComponent } from './user-management/user-form-dialog/user-form-dialog.component';
import { UserManagementComponent } from './user-management/user-management.component';

@NgModule({
  declarations: [AdminHubComponent, DirectorReportComponent, UserManagementComponent],
  imports: [
    SharedModule,
    AdminRoutingModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    UserFormDialogComponent,
  ],
})
export class AdminModule {}
