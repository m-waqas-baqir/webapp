import { animate, style, transition, trigger } from '@angular/animations';

/** Optional horizontal nudge for alternate route transitions (keep subtle). */
export const routeSlideSoft = trigger('routeSlideSoft', [
  transition('* => *', [
    style({ opacity: 0.92, transform: 'translateX(6px)' }),
    animate(
      '220ms cubic-bezier(0.4, 0, 0.2, 1)',
      style({ opacity: 1, transform: 'translateX(0)' }),
    ),
  ]),
]);
