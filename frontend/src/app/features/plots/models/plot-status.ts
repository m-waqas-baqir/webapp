/** Mirrors backend {@code com.app.backend.entity.PlotStatus}. */
export enum PlotStatus {
  AVAILABLE = 'AVAILABLE',
  RESERVED = 'RESERVED',
  SOLD = 'SOLD',
  HOLD = 'HOLD',
}

export const PLOT_STATUS_OPTIONS: readonly PlotStatus[] = [
  PlotStatus.AVAILABLE,
  PlotStatus.RESERVED,
  PlotStatus.SOLD,
  PlotStatus.HOLD,
] as const;
