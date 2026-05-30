import { NgModule } from '@angular/core';
import { AuthRoutingModule } from './auth-routing.module';
import { AuthHomeComponent } from './auth-home/auth-home.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [AuthHomeComponent],
  imports: [SharedModule, AuthRoutingModule],
})
export class AuthModule {}
