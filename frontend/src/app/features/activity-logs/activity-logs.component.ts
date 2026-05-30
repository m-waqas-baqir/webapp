import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { Sort } from '@angular/material/sort';
import { catchError, finalize, of, tap } from 'rxjs';
import { NotificationService } from '../../core/services/notification.service';
import { ActivityLogRow, AdminApiService, PageRequest } from '../admin/services/admin-api.service';
import { ActivityLogDetailDialogComponent } from './activity-log-detail-dialog.component';

const FILTER_FETCH_SIZE = 500;
const ENTITY_TYPES = ['PLOT', 'RENTAL_PROPERTY', 'OWNER', 'PHASE', 'KHAYABAN', 'LISTING'] as const;
const ACTIONS = ['CREATE', 'UPDATE', 'DELETE'] as const;

@Component({
  selector: 'app-activity-logs',
  templateUrl: './activity-logs.component.html',
  styleUrl: './activity-logs.component.scss',
  standalone: false,
})
export class ActivityLogsComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly notify = inject(NotificationService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);

  readonly filterForm = this.fb.group({
    actorEmail: this.fb.control<string>(''),
    action: this.fb.control<string>(''),
    entityType: this.fb.control<string>(''),
    dateFrom: this.fb.control<Date | null>(null),
    dateTo: this.fb.control<Date | null>(null),
  });

  rows: ActivityLogRow[] = [];
  /** Loaded window when filtering (up to FILTER_FETCH_SIZE). */
  private buffer: ActivityLogRow[] = [];
  /** Filtered + sorted view over buffer. */
  filteredBuffer: ActivityLogRow[] = [];
  filterMode = false;
  filterTruncated = false;
  totalServerElements = 0;

  loading = false;
  totalElements = 0;
  pageIndex = 0;
  pageSize = 25;
  readonly pageSizeOptions = [25, 50, 100];

  sort: Sort = { active: 'occurredAt', direction: 'desc' };

  readonly columns = ['occurredAt', 'actorEmail', 'action', 'entityType', 'entityId', 'summary'];
  readonly entityTypeOptions = [...ENTITY_TYPES];
  readonly actionOptions = [...ACTIONS];
  readonly filterFetchSize = FILTER_FETCH_SIZE;

  ngOnInit(): void {
    this.load();
  }

  actorOptions(): string[] {
    const set = new Set<string>();
    const source = this.filterMode ? this.buffer : this.rows;
    for (const r of source) {
      if (r.actorEmail) {
        set.add(r.actorEmail);
      }
    }
    return [...set].sort();
  }

  summary(row: ActivityLogRow): string {
    return `${row.action} on ${this.formatEntityType(row.entityType)} #${row.entityId}`;
  }

  formatEntityType(t: string): string {
    return t.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
  }

  actionChipClass(action: string): string {
    switch (action) {
      case 'CREATE':
        return 'chip chip--create';
      case 'UPDATE':
        return 'chip chip--update';
      case 'DELETE':
        return 'chip chip--delete';
      default:
        return 'chip';
    }
  }

  load(): void {
    this.loading = true;
    const req: PageRequest = {
      page: this.pageIndex,
      size: this.pageSize,
      sort: [this.sortParam()],
    };
    this.api
      .listActivityLogs(req)
      .pipe(
        tap((p) => {
          this.rows = p.content;
          this.totalElements = p.totalElements;
          this.totalServerElements = p.totalElements;
        }),
        catchError((e) => {
          this.notify.error(this.notify.fromHttpError(e));
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe();
  }

  private sortParam(): string {
    const dir = this.sort.direction === 'asc' ? 'asc' : 'desc';
    const col = this.sort.active || 'occurredAt';
    const allowed = new Set(['occurredAt', 'action', 'entityType', 'entityId', 'userId']);
    const field = allowed.has(col) ? col : 'occurredAt';
    return `${field},${dir}`;
  }

  onSortChange(sort: Sort): void {
    this.sort = sort.direction ? sort : { active: 'occurredAt', direction: 'desc' };
    if (this.filterMode) {
      this.applyBufferFilters();
    } else {
      this.pageIndex = 0;
      this.load();
    }
  }

  onPage(ev: PageEvent): void {
    this.pageIndex = ev.pageIndex;
    this.pageSize = ev.pageSize;
    if (this.filterMode) {
      this.applyBufferFilters();
    } else {
      this.load();
    }
  }

  applyServerFilters(): void {
    this.loading = true;
    const req: PageRequest = { page: 0, size: FILTER_FETCH_SIZE, sort: [this.sortParam()] };
    this.api
      .listActivityLogs(req)
      .pipe(
        tap((p) => {
          this.buffer = p.content;
          this.filterTruncated = p.totalElements > FILTER_FETCH_SIZE;
          this.filterMode = true;
          this.pageIndex = 0;
          this.applyBufferFilters();
        }),
        catchError((e) => {
          this.notify.error(this.notify.fromHttpError(e));
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe();
  }

  clearFilters(): void {
    this.filterForm.reset({
      actorEmail: '',
      action: '',
      entityType: '',
      dateFrom: null,
      dateTo: null,
    });
    this.filterMode = false;
    this.buffer = [];
    this.filteredBuffer = [];
    this.filterTruncated = false;
    this.pageIndex = 0;
    this.load();
  }

  private applyBufferFilters(): void {
    const f = this.filterForm.getRawValue();
    const fromMs = f.dateFrom ? this.startOfDay(f.dateFrom).getTime() : null;
    const toMs = f.dateTo ? this.endOfDay(f.dateTo).getTime() : null;
    let list = [...this.buffer];
    if (f.actorEmail) {
      list = list.filter((r) => r.actorEmail === f.actorEmail);
    }
    if (f.action) {
      list = list.filter((r) => r.action === f.action);
    }
    if (f.entityType) {
      list = list.filter((r) => r.entityType === f.entityType);
    }
    if (fromMs != null) {
      list = list.filter((r) => this.parseTime(r.occurredAt) >= fromMs);
    }
    if (toMs != null) {
      list = list.filter((r) => this.parseTime(r.occurredAt) <= toMs);
    }
    list.sort((a, b) => this.compareRows(a, b));
    this.filteredBuffer = list;
    this.totalElements = list.length;
    const totalPages = Math.ceil(list.length / this.pageSize) || 1;
    if (this.pageIndex >= totalPages) {
      this.pageIndex = Math.max(0, totalPages - 1);
    }
    const start = this.pageIndex * this.pageSize;
    this.rows = list.slice(start, start + this.pageSize);
    this.cdr.markForCheck();
  }

  private compareRows(a: ActivityLogRow, b: ActivityLogRow): number {
    const dir = this.sort.direction === 'asc' ? 1 : -1;
    const key = this.sort.active || 'occurredAt';
    const va = this.sortValue(a, key);
    const vb = this.sortValue(b, key);
    if (va < vb) {
      return -1 * dir;
    }
    if (va > vb) {
      return 1 * dir;
    }
    return 0;
  }

  private sortValue(row: ActivityLogRow, key: string): string | number {
    switch (key) {
      case 'actorEmail':
        return row.actorEmail ?? '';
      case 'action':
        return row.action;
      case 'entityType':
        return row.entityType;
      case 'entityId':
        return row.entityId;
      case 'userId':
        return row.userId;
      case 'occurredAt':
      default:
        return this.parseTime(row.occurredAt);
    }
  }

  private parseTime(iso: string): number {
    const t = Date.parse(iso);
    return Number.isNaN(t) ? 0 : t;
  }

  private startOfDay(d: Date): Date {
    const x = new Date(d);
    x.setHours(0, 0, 0, 0);
    return x;
  }

  private endOfDay(d: Date): Date {
    const x = new Date(d);
    x.setHours(23, 59, 59, 999);
    return x;
  }

  applyFilters(): void {
    if (!this.filterMode) {
      this.applyServerFilters();
      return;
    }
    this.pageIndex = 0;
    this.applyBufferFilters();
  }

  openRow(row: ActivityLogRow): void {
    this.dialog.open(ActivityLogDetailDialogComponent, {
      width: '480px',
      data: { row, summary: this.summary(row) },
    });
  }
}
