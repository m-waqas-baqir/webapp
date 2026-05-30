import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { OwnersDashboardComponent } from './owners-dashboard/owners-dashboard.component';

const routes: Routes = [
  {
    path: '',
    component: OwnersDashboardComponent,
    title: 'Owner registry',
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class OwnersRoutingModule {}
