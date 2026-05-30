import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  templateUrl: './empty-state.component.html',
  styleUrl: './empty-state.component.scss',
  standalone: false,
})
export class EmptyStateComponent {
  /** Material icon ligature name */
  @Input({ required: true }) icon!: string;
  @Input({ required: true }) titleText!: string;
  @Input() hint = '';
}
