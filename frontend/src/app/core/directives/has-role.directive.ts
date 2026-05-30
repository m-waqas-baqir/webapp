import {
  Directive,
  EmbeddedViewRef,
  Input,
  TemplateRef,
  ViewContainerRef,
  effect,
  inject,
} from '@angular/core';
import { UserRole } from '../models/user-role';
import { AuthService } from '../services/auth.service';

/**
 * Structural directive: renders template only when the user has one of the given roles.
 *
 * Example: {@code *appHasRole="[UserRole.DIRECTOR, UserRole.ADMIN]"}
 */
@Directive({
  selector: '[appHasRole]',
  standalone: false,
})
export class HasRoleDirective {
  private readonly auth = inject(AuthService);
  private readonly tpl = inject(TemplateRef<unknown>);
  private readonly vcr = inject(ViewContainerRef);

  private embedded?: EmbeddedViewRef<unknown>;
  private requiredRoles: UserRole[] = [];

  constructor() {
    effect(() => {
      this.auth.user();
      this.sync();
    });
  }

  @Input()
  set appHasRole(roles: UserRole[] | UserRole | undefined) {
    this.requiredRoles = roles == null ? [] : Array.isArray(roles) ? roles : [roles];
    this.sync();
  }

  private sync(): void {
    const ok =
      this.requiredRoles.length > 0 && this.auth.hasAnyRole(...this.requiredRoles);
    if (ok && !this.embedded) {
      this.embedded = this.vcr.createEmbeddedView(this.tpl);
    } else if (!ok && this.embedded) {
      this.embedded.destroy();
      this.embedded = undefined;
    }
  }
}
