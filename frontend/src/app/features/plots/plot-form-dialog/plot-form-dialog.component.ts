import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserRole } from '../../../core/models/user-role';
import { Khayaban } from '../models/khayaban.model';
import { Phase } from '../models/phase.model';
import { Plot, PlotUpsertRequest } from '../models/plot.model';
import { PLOT_STATUS_OPTIONS, PlotStatus } from '../models/plot-status';
import { OwnersApiService, type OwnerRegistryRecord } from '../../owners/services/owners-api.service';
import { UsersDirectoryApiService, type UserSummary } from '../../../core/services/users-directory-api.service';
import { PlotsApiService } from '../services/plots-api.service';

export interface PlotFormDialogData {
  plot?: Plot;
  defaultPhaseId?: number | null;
  defaultKhayabanId?: number | null;
}

@Component({
  selector: 'app-plot-form-dialog',
  templateUrl: './plot-form-dialog.component.html',
  styleUrl: './plot-form-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
})
export class PlotFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(PlotsApiService);
  private readonly ownersApi = inject(OwnersApiService);
  private readonly usersDir = inject(UsersDirectoryApiService);
  private readonly notify = inject(NotificationService);
  readonly auth = inject(AuthService);

  readonly UserRole = UserRole;
  readonly statusOptions = PLOT_STATUS_OPTIONS;

  readonly form: FormGroup = this.fb.group({
    plotNumber: ['', [Validators.required, Validators.maxLength(64)]],
    size: [null as number | null, [Validators.required, Validators.min(0.0001)]],
    price: [null as number | null, [Validators.required, Validators.min(0)]],
    status: [PlotStatus.AVAILABLE, Validators.required],
    phaseId: [null as number | null, Validators.required],
    khayabanId: [null as number | null, Validators.required],
    ownerId: [null as number | null],
    assignedAgentId: [null as number | null],
  });

  phases: Phase[] = [];
  khayabans: Khayaban[] = [];
  owners: OwnerRegistryRecord[] = [];
  agents: UserSummary[] = [];
  loadingPhases = false;
  loadingKhayabans = false;
  loadingOwners = false;
  loadingAgents = false;
  saving = false;

  readonly isEdit: boolean;

  constructor(
    private readonly ref: MatDialogRef<PlotFormDialogComponent, Plot | undefined>,
    @Inject(MAT_DIALOG_DATA) readonly data: PlotFormDialogData,
  ) {
    this.isEdit = !!data.plot?.id;
  }

  ngOnInit(): void {
    this.loadPhases();
    if (this.canEditAssignments) {
      this.loadOwners();
      this.loadAgents();
    }
    const p = this.data.plot;
    if (p) {
      this.form.patchValue(
        {
          plotNumber: p.plotNumber,
          size: Number(p.size),
          price: Number(p.price),
          status: p.status,
          phaseId: p.phaseId,
          khayabanId: p.khayabanId,
          ownerId: p.ownerId ?? null,
          assignedAgentId: p.assignedAgentId ?? null,
        },
        { emitEvent: false },
      );
      this.loadKhayabansForPhase(p.phaseId, p.khayabanId);
    } else {
      const pid = this.data.defaultPhaseId ?? null;
      const kid = this.data.defaultKhayabanId ?? null;
      if (pid != null) {
        this.form.patchValue({ phaseId: pid }, { emitEvent: false });
        this.loadKhayabansForPhase(pid, kid ?? undefined);
      }
    }

    this.form.get('phaseId')?.valueChanges.subscribe((phaseId: number | null) => {
      this.form.patchValue({ khayabanId: null }, { emitEvent: false });
      this.khayabans = [];
      if (phaseId != null) {
        this.loadKhayabansForPhase(phaseId);
      }
    });
  }

  get canEditAssignments(): boolean {
    return this.auth.hasAnyRole(UserRole.DIRECTOR, UserRole.ADMIN);
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue() as {
      plotNumber: string;
      size: number;
      price: number;
      status: PlotStatus;
      phaseId: number;
      khayabanId: number;
      ownerId: number | null;
      assignedAgentId: number | null;
    };

    const body: PlotUpsertRequest = {
      plotNumber: v.plotNumber.trim(),
      size: v.size,
      price: v.price,
      status: v.status,
      phaseId: v.phaseId,
      khayabanId: v.khayabanId,
    };
    if (this.canEditAssignments) {
      body.ownerId = v.ownerId ?? undefined;
      body.assignedAgentId = v.assignedAgentId ?? undefined;
    }

    this.saving = true;
    const req$ = this.isEdit
      ? this.api.updatePlot(this.data.plot!.id, body)
      : this.api.createPlot(body);

    req$
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (plot) => this.ref.close(plot),
        error: (err: unknown) => {
          this.notify.error(this.notify.fromHttpError(err));
        },
      });
  }

  cancel(): void {
    this.ref.close(undefined);
  }

  private loadPhases(): void {
    this.loadingPhases = true;
    this.api
      .listPhases({ page: 0, size: 500, sort: ['name,asc'] })
      .pipe(finalize(() => (this.loadingPhases = false)))
      .subscribe({
        next: (page) => {
          this.phases = page.content;
        },
        error: (e: unknown) => this.notify.error(this.notify.fromHttpError(e)),
      });
  }

  private loadOwners(): void {
    this.loadingOwners = true;
    this.ownersApi
      .listOwners({ page: 0, size: 500, sort: ['name,asc'] })
      .pipe(finalize(() => (this.loadingOwners = false)))
      .subscribe({
        next: (page) => {
          this.owners = page.content;
        },
        error: (e: unknown) => this.notify.error(this.notify.fromHttpError(e)),
      });
  }

  private loadAgents(): void {
    this.loadingAgents = true;
    this.usersDir
      .listByRole(UserRole.AGENT, { page: 0, size: 500, sort: ['name,asc'] })
      .pipe(finalize(() => (this.loadingAgents = false)))
      .subscribe({
        next: (page) => {
          this.agents = page.content;
        },
        error: (e: unknown) => this.notify.error(this.notify.fromHttpError(e)),
      });
  }

  private loadKhayabansForPhase(phaseId: number, selectKhayabanId?: number): void {
    this.loadingKhayabans = true;
    this.api
      .listKhayabans(phaseId, { page: 0, size: 1000, sort: ['name,asc'] })
      .pipe(finalize(() => (this.loadingKhayabans = false)))
      .subscribe({
        next: (page) => {
          this.khayabans = page.content;
          if (selectKhayabanId != null) {
            const exists = this.khayabans.some((k) => k.id === selectKhayabanId);
            if (exists) {
              this.form.patchValue({ khayabanId: selectKhayabanId });
            }
          }
        },
        error: (e: unknown) => this.notify.error(this.notify.fromHttpError(e)),
      });
  }

}
