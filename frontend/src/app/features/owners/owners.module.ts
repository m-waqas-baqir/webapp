import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { SharedModule } from '../../shared/shared.module';
import { OwnerFormDialogComponent } from './owner-form-dialog/owner-form-dialog.component';
import { OwnersDashboardComponent } from './owners-dashboard/owners-dashboard.component';
import { OwnersRoutingModule } from './owners-routing.module';

@NgModule({
  declarations: [OwnersDashboardComponent],
  imports: [
    SharedModule,
    OwnersRoutingModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    OwnerFormDialogComponent,
  ],
})
export class OwnersModule {}
