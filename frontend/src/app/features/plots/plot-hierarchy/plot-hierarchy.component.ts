import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { catchError, distinctUntilChanged, finalize, of, tap } from 'rxjs';
import { BulkImportResult } from '../../../core/models/bulk-import-result.model';
import { UserRole } from '../../../core/models/user-role';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { KhayabanFormDialogComponent, KhayabanFormDialogData } from '../khayaban-form-dialog/khayaban-form-dialog.component';
import { Khayaban } from '../models/khayaban.model';
import { Phase } from '../models/phase.model';
import { PhaseFormDialogComponent, PhaseFormDialogData } from '../phase-form-dialog/phase-form-dialog.component';
import { PlotsApiService, PageRequest } from '../services/plots-api.service';
import {
  PlotsConfirmDialogComponent,
  PlotsConfirmDialogData,
} from '../plots-confirm-dialog/plots-confirm-dialog.component';

@Component({
  selector: 'app-plot-hierarchy',
  templateUrl: './plot-hierarchy.component.html',
  styleUrl: './plot-hierarchy.component.scss',
  standalone: false,
})
export class PlotHierarchyComponent implements OnInit {
  private readonly api = inject(PlotsApiService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly fb = inject(FormBuilder);
  readonly auth = inject(AuthService);
  readonly UserRole = UserRole;

  readonly pageReq: PageRequest = { page: 0, size: 200, sort: ['name,asc'] };

  readonly phaseFilter = this.fb.control<number | null>(null);

  phases: Phase[] = [];
  khayabans: Khayaban[] = [];
  loadingPhases = false;
  loadingKhayabans = false;

  khayabanExcelUploading = false;
  khayabanExcelExporting = false;
  khayabanExcelTemplateDownloading = false;
  khayabanImportResult: BulkImportResult | null = null;

  ngOnInit(): void {
    this.reloadPhases();
    this.phaseFilter.valueChanges.pipe(distinctUntilChanged()).subscribe((id) => {
      this.loadKhayabansForPhase(id);
    });
  }

  canMutate(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN);
  }

  canBulkImportKhayabansExcel(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN);
  }

  canExportKhayabansExcel(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN, UserRole.AGENT);
  }

  reloadPhases(): void {
    this.loadingPhases = true;
    this.api
      .listPhases(this.pageReq)
      .pipe(
        tap((p) => {
          this.phases = p.content;
        }),
        catchError((e) => {
          this.notify.error(this.notify.fromHttpError(e));
          return of(null);
        }),
        finalize(() => {
          this.loadingPhases = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe();
  }

  private loadKhayabansForPhase(phaseId: number | null): void {
    this.khayabans = [];
    if (phaseId == null) {
      this.loadingKhayabans = false;
      this.cdr.markForCheck();
      return;
    }
    this.loadingKhayabans = true;
    this.api
      .listKhayabans(phaseId, this.pageReq)
      .pipe(
        tap((p) => {
          this.khayabans = p.content;
        }),
        catchError((e) => {
          this.notify.error(this.notify.fromHttpError(e));
          return of(null);
        }),
        finalize(() => {
          this.loadingKhayabans = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe();
  }

  createPhase(): void {
    const ref = this.dialog.open(PhaseFormDialogComponent, {
      width: '440px',
      data: { mode: 'create' } satisfies PhaseFormDialogData,
    });
    ref.afterClosed().subscribe((name) => {
      if (!name?.trim()) {
        return;
      }
      this.api
        .createPhase({ name: name.trim() })
        .pipe(
          tap(() => {
            this.notify.success('Phase created');
            this.reloadPhases();
          }),
          catchError((e) => {
            this.notify.error(this.notify.fromHttpError(e));
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  editPhase(phase: Phase): void {
    const ref = this.dialog.open(PhaseFormDialogComponent, {
      width: '440px',
      data: { mode: 'edit', initialName: phase.name } satisfies PhaseFormDialogData,
    });
    ref.afterClosed().subscribe((name) => {
      if (!name?.trim()) {
        return;
      }
      this.api
        .updatePhase(phase.id, { name: name.trim() })
        .pipe(
          tap(() => {
            this.notify.success('Phase updated');
            this.reloadPhases();
          }),
          catchError((e) => {
            this.notify.error(this.notify.fromHttpError(e));
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  deletePhase(phase: Phase): void {
    const ref = this.dialog.open(PlotsConfirmDialogComponent, {
      data: {
        title: 'Delete phase',
        message: `Delete phase "${phase.name}"? Only allowed when it has no khayabans.`,
        confirmLabel: 'Delete',
      } as PlotsConfirmDialogData,
    });
    ref.afterClosed().subscribe((ok) => {
      if (!ok) {
        return;
      }
      this.api
        .deletePhase(phase.id)
        .pipe(
          tap(() => {
            this.notify.success('Phase deleted');
            if (this.phaseFilter.value === phase.id) {
              this.phaseFilter.setValue(null, { emitEvent: true });
            }
            this.reloadPhases();
          }),
          catchError((e) => {
            this.notify.error(this.notify.fromHttpError(e));
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  createKhayaban(): void {
    if (this.phaseFilter.value == null) {
      this.notify.info('Select a phase first.');
      return;
    }
    const ref = this.dialog.open(KhayabanFormDialogComponent, {
      width: '480px',
      data: {
        mode: 'create',
        phases: this.phases,
        defaultPhaseId: this.phaseFilter.value,
      } satisfies KhayabanFormDialogData,
    });
    ref.afterClosed().subscribe((result) => {
      if (!result) {
        return;
      }
      this.api
        .createKhayaban({ name: result.name, phaseId: result.phaseId })
        .pipe(
          tap(() => {
            this.notify.success('Khayaban created');
            if (this.phaseFilter.value !== result.phaseId) {
              this.phaseFilter.setValue(result.phaseId);
            } else {
              this.loadKhayabansForPhase(result.phaseId);
            }
          }),
          catchError((e) => {
            this.notify.error(this.notify.fromHttpError(e));
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  editKhayaban(k: Khayaban): void {
    const ref = this.dialog.open(KhayabanFormDialogComponent, {
      width: '480px',
      data: {
        mode: 'edit',
        phases: this.phases,
        khayaban: k,
      } satisfies KhayabanFormDialogData,
    });
    ref.afterClosed().subscribe((result) => {
      if (!result) {
        return;
      }
      this.api
        .updateKhayaban(k.id, { name: result.name, phaseId: result.phaseId })
        .pipe(
          tap(() => {
            this.notify.success('Khayaban updated');
            if (this.phaseFilter.value !== result.phaseId) {
              this.phaseFilter.setValue(result.phaseId);
            } else {
              this.loadKhayabansForPhase(result.phaseId);
            }
          }),
          catchError((e) => {
            this.notify.error(this.notify.fromHttpError(e));
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  onKhayabansExcelSelected(ev: Event): void {
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
    this.khayabanExcelUploading = true;
    this.khayabanImportResult = null;
    this.api
      .importKhayabansExcel(file)
      .pipe(
        finalize(() => {
          this.khayabanExcelUploading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (r) => {
          this.khayabanImportResult = r;
          this.notify.success(`Import finished: ${r.successCount} succeeded, ${r.failureCount} failed.`);
          this.reloadPhases();
          if (this.phaseFilter.value != null) {
            this.loadKhayabansForPhase(this.phaseFilter.value);
          }
        },
        error: (e: unknown) => this.notify.error(this.notify.fromHttpError(e)),
      });
  }

  exportKhayabansExcel(): void {
    this.khayabanExcelExporting = true;
    this.api
      .exportKhayabansExcel()
      .pipe(
        finalize(() => {
          this.khayabanExcelExporting = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => this.notify.success('Export downloaded.'),
        error: (e: unknown) => this.notify.error(this.notify.fromHttpError(e)),
      });
  }

  downloadKhayabansImportTemplate(): void {
    this.khayabanExcelTemplateDownloading = true;
    this.api
      .downloadKhayabansImportTemplate()
      .pipe(
        finalize(() => {
          this.khayabanExcelTemplateDownloading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => this.notify.success('Template downloaded.'),
        error: (e: unknown) => this.notify.error(this.notify.fromHttpError(e)),
      });
  }

  clearKhayabanImportResult(): void {
    this.khayabanImportResult = null;
  }

  private isValidXlsxFile(f: File): boolean {
    const nameOk = f.name.toLowerCase().endsWith('.xlsx');
    const mimeOk =
      !f.type ||
      f.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    return nameOk && mimeOk;
  }

  deleteKhayaban(k: Khayaban): void {
    const ref = this.dialog.open(PlotsConfirmDialogComponent, {
      data: {
        title: 'Delete khayaban',
        message: `Delete "${k.name}"? Only allowed when no plots reference it.`,
        confirmLabel: 'Delete',
      } as PlotsConfirmDialogData,
    });
    ref.afterClosed().subscribe((ok) => {
      if (!ok) {
        return;
      }
      this.api
        .deleteKhayaban(k.id)
        .pipe(
          tap(() => {
            this.notify.success('Khayaban deleted');
            this.loadKhayabansForPhase(this.phaseFilter.value);
          }),
          catchError((e) => {
            this.notify.error(this.notify.fromHttpError(e));
            return of(null);
          }),
        )
        .subscribe();
    });
  }
}
