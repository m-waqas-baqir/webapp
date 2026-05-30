import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PropertyImagesDialogComponent } from '../../shared/components/property-images-dialog/property-images-dialog.component';
import { SharedModule } from '../../shared/shared.module';
import { RentalsDashboardComponent } from './rentals-dashboard/rentals-dashboard.component';
import { RentalsRoutingModule } from './rentals-routing.module';
import { BulkImportResultPanelComponent } from '../../shared/components/bulk-import-result-panel/bulk-import-result-panel.component';

@NgModule({
  declarations: [RentalsDashboardComponent],
  imports: [
    SharedModule,
    PropertyImagesDialogComponent,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatSelectModule,
    MatDialogModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatMenuModule,
    MatIconModule,
    MatProgressSpinnerModule,
    BulkImportResultPanelComponent,
    RentalsRoutingModule,
  ],
})
export class RentalsModule {}
