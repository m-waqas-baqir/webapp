import { NgModule } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { SharedModule } from '../../shared/shared.module';
import { DemoPageComponent } from './demo-page.component';
import { DemoRoutingModule } from './demo-routing.module';

@NgModule({
  declarations: [DemoPageComponent],
  imports: [SharedModule, DemoRoutingModule, MatCardModule],
})
export class DemoModule {}
