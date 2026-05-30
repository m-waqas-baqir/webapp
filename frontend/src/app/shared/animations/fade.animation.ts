import { animate, style, transition, trigger } from '@angular/animations';

/** Subtle fade + vertical slide for route host wrappers (200–240ms, ease). */
export const routeFadeSlide = trigger('routeFadeSlide', [
  transition('* => *', [
    style({ opacity: 0.88, transform: 'translateY(8px)' }),
    animate(
      '240ms cubic-bezier(0.4, 0, 0.2, 1)',
      style({ opacity: 1, transform: 'translateY(0)' }),
    ),
  ]),
]);
