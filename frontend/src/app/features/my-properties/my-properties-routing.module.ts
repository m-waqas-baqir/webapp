import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MyPropertiesComponent } from './my-properties.component';

const routes: Routes = [{ path: '', component: MyPropertiesComponent, title: 'My Properties' }];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class MyPropertiesRoutingModule {}
