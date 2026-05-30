import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { Observable, catchError, finalize, forkJoin, of, tap } from 'rxjs';
import { BulkImportResult } from '../../../core/models/bulk-import-result.model';
import { LinkedEntityType } from '../../../core/models/linked-entity-type';
import { UserRole } from '../../../core/models/user-role';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  PropertyImagesDialogComponent,
  PropertyImagesDialogData,
} from '../../../shared/components/property-images-dialog/property-images-dialog.component';
import { Khayaban } from '../models/khayaban.model';
import { Phase } from '../models/phase.model';
import { PageResponse } from '../models/page-response.model';
import { Plot } from '../models/plot.model';
import { PlotStatus } from '../models/plot-status';
import { PlotFormDialogComponent, PlotFormDialogData } from '../plot-form-dialog/plot-form-dialog.component';
import {
  PlotsConfirmDialogComponent,
  PlotsConfirmDialogData,
} from '../plots-confirm-dialog/plots-confirm-dialog.component';
import { PlotsApiService } from '../services/plots-api.service';

@Component({
  selector: 'app-plots-dashboard',
  templateUrl: './plots-dashboard.component.html',
  styleUrl: './plots-dashboard.component.scss',
  standalone: false,
})
export class PlotsDashboardComponent implements OnInit {
  private readonly api = inject(PlotsApiService);
  private readonly notify = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly auth = inject(AuthService);
  readonly UserRole = UserRole;
  readonly statusOptions = Object.values(PlotStatus);

  plots: Plot[] = [];
  loading = false;

  /** Excel bulk import/export */
  excelUploading = false;
  excelExporting = false;
  excelTemplateDownloading = false;
  importResult: BulkImportResult | null = null;

  phasesForFilter: Phase[] = [];
  khayabansForFilter: Khayaban[] = [];
  selectedPhaseId: number | null = null;
  selectedKhayabanId: number | null = null;
  selectedStatus: PlotStatus | null = null;

  minPrice: number | null = null;
  maxPrice: number | null = null;
  minSize: number | null = null;
  maxSize: number | null = null;

  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  readonly pageSizeOptions = [5, 10, 25, 50];

  sortActive = 'plotNumber';
  sortDirection: 'asc' | 'desc' = 'asc';

  /** Resolved labels for table cells */
  readonly phaseNameById: Record<number, string> = {};
  readonly khayabanNameById: Record<number, string> = {};
  private readonly khayabanCachePhases = new Set<number>();

  ngOnInit(): void {
    this.loadPhaseFilterOptions();
    this.loadPlots();
  }

  get tableColumns(): string[] {
    const cols = ['plotNumber', 'phase', 'khayaban', 'size', 'price', 'status'];
    if (this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN)) {
      cols.push('registryOwner', 'assignedAgent');
    }
    cols.push('actions');
    return cols;
  }

  canManage(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN);
  }

  canBulkImportExcel(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN);
  }

  canExportExcel(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN, UserRole.AGENT);
  }

  canEditPlot(row: Plot): boolean {
    return this.canManage() || row.canMutate === true;
  }

  phaseLabel(id: number): string {
    return this.phaseNameById[id] ?? `— (${id})`;
  }

  khayabanLabel(id: number): string {
    return this.khayabanNameById[id] ?? `— (${id})`;
  }

  onPhaseFilterChange(): void {
    this.selectedKhayabanId = null;
    this.khayabansForFilter = [];
    this.khayabanCachePhases.clear();
    this.pageIndex = 0;
    if (this.selectedPhaseId != null) {
      this.loadKhayabansForFilter(this.selectedPhaseId);
    }
    this.loadPlots();
  }

  onKhayabanOrStatusChange(): void {
    this.pageIndex = 0;
    this.loadPlots();
  }

  onNumericFiltersChange(): void {
    this.pageIndex = 0;
    this.loadPlots();
  }

  onPage(ev: PageEvent): void {
    this.pageIndex = ev.pageIndex;
    this.pageSize = ev.pageSize;
    this.loadPlots();
  }

  onSort(ev: Sort): void {
    const active = ev.direction ? ev.active : 'plotNumber';
    const direction = ev.direction === 'desc' ? 'desc' : 'asc';
    this.sortActive = active;
    this.sortDirection = direction;
    this.pageIndex = 0;
    this.loadPlots();
  }

  openCreate(): void {
    this.dialog
      .open<PlotFormDialogComponent, PlotFormDialogData, Plot | undefined>(PlotFormDialogComponent, {
        width: '560px',
        maxWidth: '95vw',
        autoFocus: 'dialog',
        data: {
          defaultPhaseId: this.selectedPhaseId,
          defaultKhayabanId: this.selectedKhayabanId,
        },
      })
      .afterClosed()
      .subscribe((plot) => {
        if (plot) {
          this.notify.success('Plot created.');
          this.reloadCachesAndPlots();
        }
      });
  }

  edit(plot: Plot): void {
    this.dialog
      .open<PlotFormDialogComponent, PlotFormDialogData, Plot | undefined>(PlotFormDialogComponent, {
        width: '560px',
        maxWidth: '95vw',
        data: { plot },
      })
      .afterClosed()
      .subscribe((updated) => {
        if (updated) {
          this.notify.success('Plot updated.');
          this.reloadCachesAndPlots();
        }
      });
  }

  openImages(plot: Plot): void {
    const data: PropertyImagesDialogData = {
      entityType: LinkedEntityType.PLOT,
      entityId: plot.id,
      title: plot.plotNumber,
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

  onPlotsExcelSelected(ev: Event): void {
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
      .importPlotsExcel(file)
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
          this.reloadCachesAndPlots();
        },
        error: (err: unknown) => this.notify.error(this.notify.fromHttpError(err)),
      });
  }

  exportPlotsExcel(): void {
    this.excelExporting = true;
    this.api
      .exportPlotsExcel()
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

  downloadPlotsImportTemplate(): void {
    this.excelTemplateDownloading = true;
    this.api
      .downloadPlotsImportTemplate()
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

  clearPlotsImportResult(): void {
    this.importResult = null;
  }

  private isValidXlsxFile(f: File): boolean {
    const nameOk = f.name.toLowerCase().endsWith('.xlsx');
    const mimeOk =
      !f.type ||
      f.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    return nameOk && mimeOk;
  }

  remove(plot: Plot): void {
    this.dialog
      .open<PlotsConfirmDialogComponent, PlotsConfirmDialogData, boolean>(PlotsConfirmDialogComponent, {
        width: '400px',
        data: {
          title: 'Delete plot',
          message: `Delete plot ${plot.plotNumber}? This cannot be undone.`,
          confirmLabel: 'Delete',
        },
      })
      .afterClosed()
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.deletePlot(plot.id).subscribe({
          next: () => {
            this.notify.success('Plot removed.');
            this.reloadCachesAndPlots();
          },
          error: (err: unknown) => this.notify.error(this.notify.fromHttpError(err)),
        });
      });
  }

  private loadPhaseFilterOptions(): void {
    this.api
      .listPhases({ page: 0, size: 500, sort: ['name,asc'] })
      .pipe(catchError((e) => this.handleErrPage<Phase>(e)))
      .subscribe((page) => {
        this.phasesForFilter = page.content;
        for (const ph of page.content) {
          this.phaseNameById[ph.id] = ph.name;
        }
        this.cdr.markForCheck();
      });
  }

  private loadKhayabansForFilter(phaseId: number): void {
    this.api
      .listKhayabans(phaseId, { page: 0, size: 1000, sort: ['name,asc'] })
      .pipe(catchError((e) => this.handleErrPage<Khayaban>(e)))
      .subscribe((page) => {
        this.khayabansForFilter = page.content;
        for (const k of page.content) {
          this.khayabanNameById[k.id] = k.name;
        }
        this.khayabanCachePhases.add(phaseId);
        this.cdr.markForCheck();
      });
  }

  private loadPlots(): void {
    this.loading = true;
    const sortField = this.toBackendSortField(this.sortActive);
    this.api
      .listPlots(
        {
          phaseId: this.selectedPhaseId ?? undefined,
          khayabanId: this.selectedKhayabanId ?? undefined,
          status: this.selectedStatus ?? undefined,
          minPrice: this.optionalNum(this.minPrice),
          maxPrice: this.optionalNum(this.maxPrice),
          minSize: this.optionalNum(this.minSize),
          maxSize: this.optionalNum(this.maxSize),
        },
        {
          page: this.pageIndex,
          size: this.pageSize,
          sort: [`${sortField},${this.sortDirection}`],
        },
      )
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (page) => {
          this.plots = page.content;
          this.totalElements = page.totalElements;
          this.enrichKhayabanNames(page.content);
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          this.notify.error(this.notify.fromHttpError(err));
        },
      });
  }

  private reloadCachesAndPlots(): void {
    this.khayabanCachePhases.clear();
    this.loadPlots();
  }

  private optionalNum(v: number | null): number | undefined {
    if (v == null || Number.isNaN(v)) {
      return undefined;
    }
    return v;
  }

  private enrichKhayabanNames(rows: Plot[]): void {
    const phaseIds = [...new Set(rows.map((r) => r.phaseId))];
    const missing = phaseIds.filter((id) => !this.khayabanCachePhases.has(id));
    if (missing.length === 0) {
      return;
    }
    forkJoin(
      missing.map((phaseId) =>
        this.api.listKhayabans(phaseId, { page: 0, size: 2000, sort: ['name,asc'] }).pipe(
          tap((page) => {
            this.khayabanCachePhases.add(phaseId);
            for (const k of page.content) {
              this.khayabanNameById[k.id] = k.name;
            }
          }),
          catchError((err: unknown) => {
            this.notify.error(this.notify.fromHttpError(err));
            return of(null);
          }),
        ),
      ),
    ).subscribe(() => this.cdr.markForCheck());
  }

  private toBackendSortField(active: string): string {
    switch (active) {
      case 'phase':
        return 'phase.id';
      case 'khayaban':
        return 'khayaban.id';
      default:
        return active;
    }
  }

  private handleErrPage<T>(err: unknown): Observable<PageResponse<T>> {
    this.notify.error(this.notify.fromHttpError(err));
    return of({
      content: [],
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 0,
      first: true,
      last: true,
      empty: true,
    } as PageResponse<T>);
  }

}
