import { PlotStatus } from './plot-status';

export interface Plot {
  id: number;
  plotNumber: string;
  size: number;
  price: number;
  status: PlotStatus;
  phaseId: number;
  khayabanId: number;
  ownerId?: number | null;
  ownerName?: string | null;
  assignedAgentId?: number | null;
  assignedAgentName?: string | null;
  canMutate?: boolean;
}

export interface PlotUpsertRequest {
  plotNumber: string;
  size: number;
  price: number;
  status: PlotStatus;
  phaseId: number;
  khayabanId: number;
  ownerId?: number | null;
  assignedAgentId?: number | null;
}
