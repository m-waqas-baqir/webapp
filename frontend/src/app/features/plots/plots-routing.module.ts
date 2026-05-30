import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PlotHierarchyComponent } from './plot-hierarchy/plot-hierarchy.component';
import { PlotsDashboardComponent } from './plots-dashboard/plots-dashboard.component';

const routes: Routes = [
  {
    path: '',
    component: PlotsDashboardComponent,
    title: 'Plots',
  },
  {
    path: 'hierarchy',
    component: PlotHierarchyComponent,
    title: 'Plot hierarchy',
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PlotsRoutingModule {}
