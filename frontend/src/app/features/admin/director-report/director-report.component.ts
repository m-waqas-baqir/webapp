import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { catchError, finalize, of, tap } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { AdminApiService, ListingOwnerDetailRow } from '../services/admin-api.service';

@Component({
  selector: 'app-director-report',
  templateUrl: './director-report.component.html',
  styleUrl: './director-report.component.scss',
  standalone: false,
})
export class DirectorReportComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly notify = inject(NotificationService);
  private readonly cdr = inject(ChangeDetectorRef);

  rows: ListingOwnerDetailRow[] = [];
  loading = false;

  readonly columns = ['listingTitle', 'ownerName', 'ownerEmail', 'assignedAgentId'];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.api
      .directorListingOwnerReport()
      .pipe(
        tap((r) => {
          this.rows = r;
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

  get totalListings(): number {
    return this.rows.length;
  }

  get uniqueOwners(): number {
    return new Set(this.rows.map((r) => r.ownerEmail.toLowerCase())).size;
  }

  get assignedShareLabel(): string {
    if (!this.rows.length) {
      return '—';
    }
    const n = this.rows.filter((r) => r.assignedAgentId != null).length;
    const pct = Math.round((100 * n) / this.rows.length);
    return `${n} of ${this.rows.length} (${pct}%) have an agent assigned`;
  }

  /** Owner name appearing on the most rows — simple “concentration” signal. */
  get mostActiveOwnerLabel(): string | null {
    if (!this.rows.length) {
      return null;
    }
    const counts = new Map<string, number>();
    for (const r of this.rows) {
      const k = r.ownerName.trim();
      counts.set(k, (counts.get(k) ?? 0) + 1);
    }
    let best = '';
    let max = 0;
    for (const [name, c] of counts) {
      if (c > max) {
        max = c;
        best = name;
      }
    }
    return max > 1 ? `${best} (${max} listings)` : best;
  }

  get topListingTitle(): string | null {
    if (!this.rows.length) {
      return null;
    }
    return this.rows.reduce((a, b) => (a.listingTitle.length >= b.listingTitle.length ? a : b)).listingTitle;
  }

  isHighlightRow(row: ListingOwnerDetailRow): boolean {
    const top = this.topListingTitle;
    return top != null && row.listingTitle === top;
  }
}
