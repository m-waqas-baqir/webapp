import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

const STORAGE_KEY = 'ri-theme-preference';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly doc = inject(DOCUMENT);

  /** When true, `app-dark-theme` and `dark-theme` are set on `html` (and mirrored on `body`); light uses `light-theme`. */
  readonly isDark = signal(false);

  constructor() {
    this.restore();
  }

  /** Toggle light/dark and persist choice. */
  toggle(): void {
    this.isDark.update((d) => !d);
    this.apply();
    try {
      this.doc.defaultView?.localStorage.setItem(STORAGE_KEY, this.isDark() ? 'dark' : 'light');
    } catch {
      /* ignore */
    }
  }

  private restore(): void {
    try {
      const v = this.doc.defaultView?.localStorage.getItem(STORAGE_KEY);
      if (v === 'dark') {
        this.isDark.set(true);
      } else if (v === 'light') {
        this.isDark.set(false);
      } else {
        const prefersDark =
          this.doc.defaultView?.matchMedia?.('(prefers-color-scheme: dark)').matches === true;
        this.isDark.set(prefersDark);
      }
    } catch {
      this.isDark.set(false);
    }
    this.apply();
  }

  private apply(): void {
    const dark = this.isDark();
    const root = this.doc.documentElement;
    root.classList.toggle('app-dark-theme', dark);
    root.classList.toggle('dark-theme', dark);
    root.classList.toggle('light-theme', !dark);

    const body = this.doc.body;
    if (body) {
      body.classList.toggle('dark-theme', dark);
      body.classList.toggle('light-theme', !dark);
    }
  }
}
