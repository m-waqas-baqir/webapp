import { LinkedEntityType } from './linked-entity-type';

export interface PropertyImage {
  id: number;
  linkedEntityType: LinkedEntityType;
  linkedEntityId: number;
  fileName: string;
  contentType: string;
  byteSize: number;
  createdAt: string;
  downloadUrl: string;
}
