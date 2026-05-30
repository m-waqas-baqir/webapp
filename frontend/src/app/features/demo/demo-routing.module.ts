import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DemoPageComponent } from './demo-page.component';

const routes: Routes = [
  { path: '', component: DemoPageComponent, title: 'Training hub' },
  { path: ':role', component: DemoPageComponent, title: 'Training hub' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class DemoRoutingModule {}
