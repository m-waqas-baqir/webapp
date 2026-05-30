import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs';
import { ThemeService } from './core/services/theme.service';
import { routeFadeSlide } from './shared/animations/fade.animation';

function appTopRouteKey(url: string): string {
  const path = url.split('?')[0];
  if (path === '' || path === '/') {
    return 'root';
  }
  return path.split('/').filter(Boolean)[0] ?? 'root';
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  standalone: false,
  animations: [routeFadeSlide],
})
export class AppComponent {
  title = 'Real Investments';

  /** Ensures theme preference is applied on bootstrap (not only when a toggle mounts). */
  private readonly _theme = inject(ThemeService);
  private readonly router = inject(Router);

  /** Animates only when the top-level segment changes (e.g. home → login → app). */
  readonly appRouteKey = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => appTopRouteKey(this.router.url)),
    ),
    { initialValue: appTopRouteKey(this.router.url) },
  );
}
