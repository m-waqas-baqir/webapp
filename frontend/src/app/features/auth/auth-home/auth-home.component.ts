import { Component, inject } from '@angular/core';
import { UserRole } from '../../../core/models/user-role';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-auth-home',
  templateUrl: './auth-home.component.html',
  styleUrl: './auth-home.component.scss',
  standalone: false,
})
export class AuthHomeComponent {
  readonly auth = inject(AuthService);
  readonly UserRole = UserRole;
}
