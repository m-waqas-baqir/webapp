import {
  AfterViewInit,
  afterNextRender,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Injector,
  NgZone,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import type { Feature } from 'geojson';
import type { Layer } from 'leaflet';
import * as L from 'leaflet';
import { DHA_PHASE8_BOUNDARY_FEATURE } from './dha-phase8-boundary';

/** OSM street basemap */
const OSM_TILE_TEMPLATE = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';

/** Esri World Imagery (satellite) — no API key for tile access */
const ESRI_WORLD_IMAGERY =
  'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}';

const GOOGLE_MAPS_EXTERNAL =
  'https://www.google.com/maps/@24.8607,67.0011,15z' as const;

/** Real Investments — DHA Phase 8 office (static coordinates for div marker only). */
const OFFICE_LAT = 24.792514;
const OFFICE_LNG = 67.075634;

/**
 * DHA Karachi neighborhood map — Leaflet with OSM + Esri imagery basemaps (no API keys).
 * Phase boundary via GeoJSON; optional overlay group for plot markers / future layers.
 */
@Component({
  selector: 'app-dha-map',
  templateUrl: './dha-map.component.html',
  styleUrl: './dha-map.component.scss',
  standalone: true,
  imports: [MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DhaMapComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapHost') mapHost?: ElementRef<HTMLDivElement>;
  @ViewChild('fullscreenRoot') fullscreenRoot?: ElementRef<HTMLDivElement>;

  readonly placeholderSrc = '/dha-phase8-placeholder.svg';

  showMapHost = false;
  mapReady = false;

  /** Active basemap: only one of `streetLayer` / `satelliteLayer` is on the map at a time. */
  basemapMode: 'street' | 'satellite' = 'street';
  isFullscreen = false;

  private map?: L.Map;
  private streetLayer?: L.TileLayer;
  private satelliteLayer?: L.TileLayer;
  private overlayLayer?: L.LayerGroup;
  private destroyed = false;
  private visibilitySeen = false;
  private idleScheduled = false;

  private intersectionObserver?: IntersectionObserver;
  private invalidateRetryTimer?: ReturnType<typeof setTimeout>;
  private layoutRetryCount = 0;

  /** Karachi DHA area — Leaflet uses [lat, lng] */
  private static readonly CENTER: [number, number] = [24.8607, 67.0011];
  private static readonly INITIAL_ZOOM = 13;
  private static readonly MIN_ZOOM = 10;
  private static readonly MAX_ZOOM = 18;
  private static readonly INVALIDATE_MS = 280;
  private static readonly MIN_DELAY_AFTER_VISIBLE_MS = 280;
  private static readonly MAX_LAYOUT_RETRIES = 10;
  private static readonly IDLE_CALLBACK_TIMEOUT_MS = 1500;

  /** Phase 8 boundary highlight (static GeoJSON; fits bounds in {@link addPhaseBoundary}). */
  private static readonly BOUNDARY_STYLE: L.PathOptions = {
    color: '#8b5cf6',
    weight: 2,
    opacity: 0.9,
    fillColor: '#8b5cf6',
    fillOpacity: 0.08,
  };

  private readonly onFullscreenChange = (): void => {
    this.ngZone.run(() => {
      this.isFullscreen = !!this.getFullscreenElement();
      this.cdr.markForCheck();
      requestAnimationFrame(() => this.map?.invalidateSize({ animate: false }));
    });
  };

  constructor(
    private readonly hostRef: ElementRef<HTMLElement>,
    private readonly cdr: ChangeDetectorRef,
    private readonly injector: Injector,
    private readonly ngZone: NgZone,
  ) {}

  ngAfterViewInit(): void {
    document.addEventListener('fullscreenchange', this.onFullscreenChange);
    document.addEventListener('webkitfullscreenchange', this.onFullscreenChange);
    this.attachVisibilityObserverOnly();
  }

  get basemapToggleLabel(): string {
    return this.basemapMode === 'street' ? 'Satellite View' : 'Street View';
  }

  get fullscreenToggleLabel(): string {
    return this.isFullscreen ? 'Exit Fullscreen' : 'Fullscreen';
  }

  ngOnDestroy(): void {
    document.removeEventListener('fullscreenchange', this.onFullscreenChange);
    document.removeEventListener('webkitfullscreenchange', this.onFullscreenChange);
    const root = this.fullscreenRoot?.nativeElement;
    if (root && this.getFullscreenElement() === root) {
      void this.exitFullscreenCompat();
    }
    this.destroyed = true;
    this.teardownObserver();
    if (this.invalidateRetryTimer) {
      clearTimeout(this.invalidateRetryTimer);
      this.invalidateRetryTimer = undefined;
    }
    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }
    this.streetLayer = undefined;
    this.satelliteLayer = undefined;
    this.overlayLayer = undefined;
  }

  /** Swap OSM ↔ Esri imagery without re-creating the map or overlay layers. */
  toggleBasemap(): void {
    if (!this.map || !this.streetLayer || !this.satelliteLayer) {
      return;
    }
    let next: 'street' | 'satellite';
    this.ngZone.runOutsideAngular(() => {
      if (this.basemapMode === 'street') {
        this.map!.removeLayer(this.streetLayer!);
        this.satelliteLayer!.addTo(this.map!);
        next = 'satellite';
      } else {
        this.map!.removeLayer(this.satelliteLayer!);
        this.streetLayer!.addTo(this.map!);
        next = 'street';
      }
    });
    this.ngZone.run(() => {
      this.basemapMode = next!;
      this.cdr.markForCheck();
    });
  }

  toggleFullscreen(): void {
    const el = this.fullscreenRoot?.nativeElement;
    if (!el) {
      return;
    }
    if (this.getFullscreenElement()) {
      void this.exitFullscreenCompat();
    } else {
      void this.requestFullscreenCompat(el);
    }
  }

  openGoogleMapsExternal(): void {
    window.open(GOOGLE_MAPS_EXTERNAL, '_blank', 'noopener,noreferrer');
  }

  private getFullscreenElement(): Element | null {
    const d = document as Document & {
      webkitFullscreenElement?: Element | null;
      msFullscreenElement?: Element | null;
    };
    return (
      document.fullscreenElement ??
      d.webkitFullscreenElement ??
      d.msFullscreenElement ??
      null
    );
  }

  private requestFullscreenCompat(el: HTMLElement): Promise<void> | undefined {
    const anyEl = el as HTMLElement & {
      webkitRequestFullscreen?: () => Promise<void>;
      msRequestFullscreen?: () => Promise<void>;
    };
    return (
      el.requestFullscreen?.() ??
      anyEl.webkitRequestFullscreen?.() ??
      anyEl.msRequestFullscreen?.()
    );
  }

  private exitFullscreenCompat(): Promise<void> | undefined {
    const d = document as Document & {
      webkitExitFullscreen?: () => Promise<void>;
      msExitFullscreen?: () => Promise<void>;
    };
    return (
      document.exitFullscreen?.() ??
      d.webkitExitFullscreen?.() ??
      d.msExitFullscreen?.()
    );
  }

  private attachVisibilityObserverOnly(): void {
    if (this.destroyed) {
      return;
    }
    this.ngZone.runOutsideAngular(() => {
      const host = this.hostRef.nativeElement;
      this.intersectionObserver = new IntersectionObserver(
        (entries) => {
          if (!entries.some((e) => e.isIntersecting) || this.visibilitySeen) {
            return;
          }
          this.visibilitySeen = true;
          this.scheduleInitAfterIdleAndDelay();
        },
        { root: null, rootMargin: '100px 0px', threshold: 0 },
      );
      this.intersectionObserver.observe(host);
    });
  }

  private scheduleInitAfterIdleAndDelay(): void {
    if (this.destroyed || this.idleScheduled) {
      return;
    }
    this.idleScheduled = true;
    this.teardownObserver();

    this.ngZone.runOutsideAngular(() => {
      const gateStart = Date.now();
      const proceed = (): void => {
        const elapsed = Date.now() - gateStart;
        const waitMore = Math.max(0, DhaMapComponent.MIN_DELAY_AFTER_VISIBLE_MS - elapsed);
        setTimeout(() => {
          if (this.destroyed) {
            return;
          }
          this.ngZone.run(() => this.beginMapHostRender());
        }, waitMore);
      };

      if (typeof requestIdleCallback === 'function') {
        requestIdleCallback(proceed, { timeout: DhaMapComponent.IDLE_CALLBACK_TIMEOUT_MS });
      } else {
        setTimeout(proceed, DhaMapComponent.MIN_DELAY_AFTER_VISIBLE_MS);
      }
    });
  }

  private teardownObserver(): void {
    this.intersectionObserver?.disconnect();
    this.intersectionObserver = undefined;
  }

  private beginMapHostRender(): void {
    if (this.destroyed || this.showMapHost) {
      return;
    }
    this.showMapHost = true;
    this.cdr.markForCheck();

    afterNextRender(
      () => {
        this.ngZone.runOutsideAngular(() => this.installMapEngine());
      },
      { injector: this.injector },
    );
  }

  /** One Leaflet map per component lifecycle. */
  private installMapEngine(): void {
    if (this.destroyed || this.map) {
      return;
    }

    const el = this.mapHost?.nativeElement;
    if (!el) {
      this.retryLayoutForHost();
      return;
    }

    const r = el.getBoundingClientRect();
    if (
      (r.width < 4 || r.height < 4) &&
      this.layoutRetryCount < DhaMapComponent.MAX_LAYOUT_RETRIES
    ) {
      this.layoutRetryCount += 1;
      requestAnimationFrame(() => {
        this.ngZone.runOutsideAngular(() => {
          if (!this.destroyed && !this.map) {
            this.installMapEngine();
          }
        });
      });
      return;
    }

    this.layoutRetryCount = 0;

    this.map = L.map(el, {
      center: DhaMapComponent.CENTER,
      zoom: DhaMapComponent.INITIAL_ZOOM,
      minZoom: DhaMapComponent.MIN_ZOOM,
      maxZoom: DhaMapComponent.MAX_ZOOM,
      zoomControl: false,
      attributionControl: true,
    });

    this.streetLayer = L.tileLayer(OSM_TILE_TEMPLATE, {
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright" rel="noopener">OpenStreetMap</a>',
      maxNativeZoom: 19,
    });

    this.satelliteLayer = L.tileLayer(ESRI_WORLD_IMAGERY, {
      attribution: '© Esri',
      maxZoom: DhaMapComponent.MAX_ZOOM,
    });

    this.streetLayer.addTo(this.map);
    this.basemapMode = 'street';

    /* Top-left: keeps custom toolbar (top-right) clear of +/- zoom */
    L.control.zoom({ position: 'topleft' }).addTo(this.map);

    this.overlayLayer = L.layerGroup().addTo(this.map);
    this.addPhaseBoundary();

    /* Office div marker (no routing UI). */
    this.addOfficeMarker();

    this.scheduleInvalidateAndReveal();
  }

  /** Static Phase 8 outline — GeoJSON polygon, popup, hover; visible framing via fitBounds. */
  private addPhaseBoundary(): void {
    if (!this.map || !this.overlayLayer) {
      return;
    }

    const base = DhaMapComponent.BOUNDARY_STYLE;

    const gj = L.geoJSON(DHA_PHASE8_BOUNDARY_FEATURE as GeoJSON.GeoJsonObject, {
      style: () => ({ ...base }),
      onEachFeature: (feature: Feature, layer: Layer) => {
        const label = feature.properties?.['label'];
        if (label != null) {
          layer.bindPopup(String(label));
        }
        const pathLayer = layer as L.Path;
        layer.on('mouseover', () => {
          pathLayer.setStyle({ ...base, fillOpacity: 0.16 });
          this.map!.getContainer().style.cursor = 'grab';
        });
        layer.on('mouseout', () => {
          pathLayer.setStyle(base);
          this.map!.getContainer().style.cursor = '';
        });
        layer.on('click', () => {
          if (label != null) {
            layer.openPopup();
          }
        });
      },
    });
    gj.addTo(this.overlayLayer);

    const bounds = gj.getBounds();
    if (bounds.isValid()) {
      this.map.fitBounds(bounds.pad(0.12));
    }
  }

  /**
   * Hook for future plot markers: `plotMarker.addTo(this.overlayLayer)` from parent/services.
   * Overlay group is created in {@link installMapEngine}.
   */
  getOverlayLayer(): L.LayerGroup | undefined {
    return this.overlayLayer;
  }

  /** Office: branded div marker (always-visible label; no popup / routing). */
  private addOfficeMarker(): void {
    if (!this.map || !this.overlayLayer) {
      return;
    }

    const officeIcon = L.divIcon({
      className: 'custom-office-marker',
      html: '<div class="pin"></div><div class="label">Real Investments</div>',
      iconSize: [120, 40],
      iconAnchor: [60, 40],
    });

    const officeMarker = L.marker([OFFICE_LAT, OFFICE_LNG], {
      icon: officeIcon,
      interactive: true,
      keyboard: true,
      title: 'Real Investments — DHA Phase 8 Office',
    }).addTo(this.overlayLayer);
    officeMarker.bindPopup('Real Investments — DHA Phase 8 Office');
  }

  private scheduleInvalidateAndReveal(): void {
    if (!this.map || this.destroyed) {
      return;
    }

    const runInvalidate = (): void => {
      if (!this.map || this.destroyed) {
        return;
      }
      this.map.invalidateSize({ animate: false });
    };

    runInvalidate();

    this.invalidateRetryTimer = setTimeout(() => {
      this.invalidateRetryTimer = undefined;
      if (!this.map || this.destroyed) {
        return;
      }
      runInvalidate();
      requestAnimationFrame(() => {
        if (!this.destroyed) {
          this.ngZone.run(() => {
            this.mapReady = true;
            this.cdr.markForCheck();
          });
        }
      });
    }, DhaMapComponent.INVALIDATE_MS);
  }

  private retryLayoutForHost(): void {
    if (this.destroyed || this.map) {
      return;
    }
    if (this.layoutRetryCount < DhaMapComponent.MAX_LAYOUT_RETRIES) {
      this.layoutRetryCount += 1;
      requestAnimationFrame(() => {
        this.ngZone.runOutsideAngular(() => {
          if (!this.destroyed && !this.map) {
            this.installMapEngine();
          }
        });
      });
    }
  }
}
