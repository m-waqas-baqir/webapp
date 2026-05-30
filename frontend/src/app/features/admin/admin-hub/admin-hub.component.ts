import { Component } from '@angular/core';
import { UserRole } from '../../../core/models/user-role';

@Component({
  selector: 'app-admin-hub',
  templateUrl: './admin-hub.component.html',
  styleUrl: './admin-hub.component.scss',
  standalone: false,
})
export class AdminHubComponent {
  readonly UserRole = UserRole;
}
