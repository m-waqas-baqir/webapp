import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { BulkImportResult } from '../../../core/models/bulk-import-result.model';

@Component({
  selector: 'app-bulk-import-result-panel',
  standalone: true,
  imports: [MatCardModule, MatTableModule, MatButtonModule, MatIconModule],
  templateUrl: './bulk-import-result-panel.component.html',
  styleUrl: './bulk-import-result-panel.component.scss',
})
export class BulkImportResultPanelComponent {
  @Input({ required: true }) result!: BulkImportResult;
  @Input() heading = 'Import result';
  @Output() dismiss = new EventEmitter<void>();

  readonly errorColumns: string[] = ['row', 'message'];
}
