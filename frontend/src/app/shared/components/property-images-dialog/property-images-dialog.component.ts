import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { LinkedEntityType } from '../../../core/models/linked-entity-type';
import { PropertyImage } from '../../../core/models/property-image.model';
import { NotificationService } from '../../../core/services/notification.service';
import { PropertyImagesApiService } from '../../../core/services/property-images-api.service';

export interface PropertyImagesDialogData {
  entityType: LinkedEntityType;
  entityId: number;
  title: string;
}

@Component({
  selector: 'app-property-images-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatCardModule],
  templateUrl: './property-images-dialog.component.html',
  styleUrl: './property-images-dialog.component.scss',
})
export class PropertyImagesDialogComponent implements OnInit, OnDestroy {
  private readonly api = inject(PropertyImagesApiService);
  private readonly notify = inject(NotificationService);
  private readonly sanitizer = inject(DomSanitizer);

  images: PropertyImage[] = [];
  loading = false;
  uploading = false;
  /** Inline confirmation so users see impact beyond the snackbar. */
  readonly uploadBanner = signal<string | null>(null);
  previewUrl: SafeUrl | null = null;
  private previewRevoke?: string;

  constructor(
    private readonly ref: MatDialogRef<PropertyImagesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) readonly data: PropertyImagesDialogData,
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  ngOnDestroy(): void {
    if (this.previewRevoke) {
      URL.revokeObjectURL(this.previewRevoke);
    }
  }

  reload(): void {
    this.loading = true;
    this.api.list(this.data.entityType, this.data.entityId).subscribe({
      next: (rows) => {
        this.images = rows;
        this.loading = false;
      },
      error: (err: unknown) => {
        this.loading = false;
        this.notify.error(this.notify.fromHttpError(err));
      },
    });
  }

  onFileSelected(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    this.uploading = true;
    this.api.upload(this.data.entityType, this.data.entityId, file).subscribe({
      next: () => {
        this.uploading = false;
        this.uploadBanner.set(`Uploaded “${file.name}” — it now appears in the list below.`);
        this.notify.success('Image uploaded.');
        this.reload();
      },
      error: (err: unknown) => {
        this.uploading = false;
        this.notify.error(this.notify.fromHttpError(err));
      },
    });
  }

  preview(img: PropertyImage): void {
    if (this.previewRevoke) {
      URL.revokeObjectURL(this.previewRevoke);
      this.previewRevoke = undefined;
    }
    this.api.getFileBlob(img.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        this.previewRevoke = url;
        this.previewUrl = this.sanitizer.bypassSecurityTrustUrl(url);
      },
      error: (err: unknown) => this.notify.error(this.notify.fromHttpError(err)),
    });
  }

  remove(img: PropertyImage): void {
    this.api.delete(img.id).subscribe({
      next: () => {
        this.uploadBanner.set(null);
        this.notify.success('Image removed.');
        if (this.previewUrl && this.images.some((i) => i.id === img.id)) {
          this.previewUrl = null;
          if (this.previewRevoke) {
            URL.revokeObjectURL(this.previewRevoke);
            this.previewRevoke = undefined;
          }
        }
        this.reload();
      },
      error: (err: unknown) => this.notify.error(this.notify.fromHttpError(err)),
    });
  }

  close(): void {
    this.ref.close();
  }

  titleCase(): string {
    return this.data.entityType === LinkedEntityType.PLOT ? 'Plot' : 'Rental';
  }
}
