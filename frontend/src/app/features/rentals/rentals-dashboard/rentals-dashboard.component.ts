import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { finalize } from 'rxjs';
import { BulkImportResult } from '../../../core/models/bulk-import-result.model';
import { LinkedEntityType } from '../../../core/models/linked-entity-type';
import { UserRole } from '../../../core/models/user-role';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  PropertyImagesDialogComponent,
  PropertyImagesDialogData,
} from '../../../shared/components/property-images-dialog/property-images-dialog.component';
import {
  PlotsConfirmDialogComponent,
  PlotsConfirmDialogData,
} from '../../plots/plots-confirm-dialog/plots-confirm-dialog.component';
import { RentalProperty } from '../models/rental-property.model';
import { RentalPropertyStatus } from '../models/rental-property-status';
import { RentalPropertyType } from '../models/rental-property-type';
import { RentalFormDialogComponent, RentalFormDialogData } from '../rental-form-dialog/rental-form-dialog.component';
import { RentalsApiService } from '../services/rentals-api.service';

@Component({
  selector: 'app-rentals-dashboard',
  templateUrl: './rentals-dashboard.component.html',
  styleUrl: './rentals-dashboard.component.scss',
  standalone: false,
})
export class RentalsDashboardComponent implements OnInit {
  private readonly api = inject(RentalsApiService);
  private readonly notify = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly auth = inject(AuthService);
  readonly UserRole = UserRole;
  readonly typeFilterOptions = Object.values(RentalPropertyType);
  readonly statusFilterOptions = Object.values(RentalPropertyStatus);

  rentals: RentalProperty[] = [];
  loading = false;

  excelUploading = false;
  excelExporting = false;
  excelTemplateDownloading = false;
  importResult: BulkImportResult | null = null;

  selectedType: RentalPropertyType | null = null;
  selectedStatus: RentalPropertyStatus | null = null;

  minRent: number | null = null;
  maxRent: number | null = null;

  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  readonly pageSizeOptions = [5, 10, 25, 50];

  sortActive = 'title';
  sortDirection: 'asc' | 'desc' = 'asc';

  ngOnInit(): void {
    this.loadRentals();
  }

  get tableColumns(): string[] {
    const cols = ['title', 'type', 'address', 'rentAmount', 'status'];
    if (this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN)) {
      cols.push('registryOwner', 'assignedAgent');
    }
    cols.push('actions');
    return cols;
  }

  canManage(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN);
  }

  canEditRental(row: RentalProperty): boolean {
    return this.canManage() || row.canMutate === true;
  }

  canBulkImportExcel(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN);
  }

  canExportExcel(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN, UserRole.AGENT);
  }

  onFilterChange(): void {
    this.pageIndex = 0;
    this.loadRentals();
  }

  onRentRangeChange(): void {
    this.pageIndex = 0;
    this.loadRentals();
  }

  onPage(ev: PageEvent): void {
    this.pageIndex = ev.pageIndex;
    this.pageSize = ev.pageSize;
    this.loadRentals();
  }

  onSort(ev: Sort): void {
    const active = ev.direction ? ev.active : 'title';
    const direction = ev.direction === 'desc' ? 'desc' : 'asc';
    this.sortActive = active;
    this.sortDirection = direction;
    this.pageIndex = 0;
    this.loadRentals();
  }

  onRentalsExcelSelected(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    if (!this.isValidXlsxFile(file)) {
      this.notify.error('Please choose a valid Excel file (.xlsx).');
      return;
    }
    this.excelUploading = true;
    this.importResult = null;
    this.api
      .importRentalsExcel(file)
      .pipe(
        finalize(() => {
          this.excelUploading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (r) => {
          this.importResult = r;
          this.notify.success(`Import finished: ${r.successCount} succeeded, ${r.failureCount} failed.`);
          this.loadRentals();
        },
        error: (err: unknown) => this.notify.error(this.notify.fromHttpError(err)),
      });
  }

  exportRentalsExcel(): void {
    this.excelExporting = true;
    this.api
      .exportRentalsExcel()
      .pipe(
        finalize(() => {
          this.excelExporting = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => this.notify.success('Export downloaded.'),
        error: (err: unknown) => this.notify.error(this.notify.fromHttpError(err)),
      });
  }

  downloadRentalsImportTemplate(): void {
    this.excelTemplateDownloading = true;
    this.api
      .downloadRentalsImportTemplate()
      .pipe(
        finalize(() => {
          this.excelTemplateDownloading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => this.notify.success('Template downloaded.'),
        error: (err: unknown) => this.notify.error(this.notify.fromHttpError(err)),
      });
  }

  clearRentalsImportResult(): void {
    this.importResult = null;
  }

  private isValidXlsxFile(f: File): boolean {
    const nameOk = f.name.toLowerCase().endsWith('.xlsx');
    const mimeOk =
      !f.type ||
      f.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    return nameOk && mimeOk;
  }

  openCreate(): void {
    this.dialog
      .open<RentalFormDialogComponent, RentalFormDialogData, RentalProperty | undefined>(
        RentalFormDialogComponent,
        {
          width: '560px',
          maxWidth: '95vw',
          autoFocus: 'dialog',
          data: {},
        },
      )
      .afterClosed()
      .subscribe((rental) => {
        if (rental) {
          this.notify.success('Rental property created.');
          this.loadRentals();
        }
      });
  }

  edit(row: RentalProperty): void {
    this.dialog
      .open<RentalFormDialogComponent, RentalFormDialogData, RentalProperty | undefined>(
        RentalFormDialogComponent,
        {
          width: '560px',
          maxWidth: '95vw',
          data: { rental: row },
        },
      )
      .afterClosed()
      .subscribe((updated) => {
        if (updated) {
          this.notify.success('Rental property updated.');
          this.loadRentals();
        }
      });
  }

  openImages(row: RentalProperty): void {
    const data: PropertyImagesDialogData = {
      entityType: LinkedEntityType.RENTAL_PROPERTY,
      entityId: row.id,
      title: row.title,
    };
    this.dialog
      .open(PropertyImagesDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        autoFocus: 'dialog',
        data,
      })
      .afterClosed()
      .subscribe(() => this.cdr.markForCheck());
  }

  remove(row: RentalProperty): void {
    this.dialog
      .open<PlotsConfirmDialogComponent, PlotsConfirmDialogData, boolean>(PlotsConfirmDialogComponent, {
        width: '400px',
        data: {
          title: 'Delete rental property',
          message: `Delete "${row.title}"? This cannot be undone.`,
          confirmLabel: 'Delete',
        },
      })
      .afterClosed()
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.deleteRentalProperty(row.id).subscribe({
          next: () => {
            this.notify.success('Rental property removed.');
            this.loadRentals();
          },
          error: (err: unknown) => this.notify.error(this.notify.fromHttpError(err)),
        });
      });
  }

  private loadRentals(): void {
    this.loading = true;
    this.api
      .listRentalProperties(
        {
          type: this.selectedType ?? undefined,
          status: this.selectedStatus ?? undefined,
          minRent: this.optionalNum(this.minRent),
          maxRent: this.optionalNum(this.maxRent),
        },
        {
          page: this.pageIndex,
          size: this.pageSize,
          sort: [`${this.sortActive},${this.sortDirection}`],
        },
      )
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (page) => {
          this.rentals = page.content;
          this.totalElements = page.totalElements;
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          this.notify.error(this.notify.fromHttpError(err));
          this.cdr.markForCheck();
        },
      });
  }

  private optionalNum(v: number | null): number | undefined {
    if (v == null || Number.isNaN(v)) {
      return undefined;
    }
    return v;
  }
}
