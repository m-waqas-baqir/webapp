import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  templateUrl: './page-header.component.html',
  styleUrl: './page-header.component.scss',
  standalone: false,
})
export class PageHeaderComponent {
  @Input({ required: true }) title!: string;
  @Input() subtitle = '';
  /** Optional Material icon ligature for visual hierarchy */
  @Input() icon = '';
}
