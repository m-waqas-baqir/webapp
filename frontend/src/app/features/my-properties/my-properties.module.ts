import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { SharedModule } from '../../shared/shared.module';
import { MyPropertiesRoutingModule } from './my-properties-routing.module';
import { MyPropertiesComponent } from './my-properties.component';

@NgModule({
  declarations: [MyPropertiesComponent],
  imports: [SharedModule, MyPropertiesRoutingModule, MatCardModule, MatButtonModule],
})
export class MyPropertiesModule {}
