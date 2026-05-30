import { RentalPropertyStatus } from './rental-property-status';
import { RentalPropertyType } from './rental-property-type';

export interface RentalProperty {
  id: number;
  title: string;
  type: RentalPropertyType;
  address: string;
  rentAmount: number;
  status: RentalPropertyStatus;
  ownerId?: number | null;
  ownerName?: string | null;
  assignedAgentId?: number | null;
  assignedAgentName?: string | null;
  canMutate?: boolean;
}

export interface RentalPropertyUpsertRequest {
  title: string;
  type: RentalPropertyType;
  address: string;
  rentAmount: number;
  status: RentalPropertyStatus;
  ownerId?: number | null;
  assignedAgentId?: number | null;
}
