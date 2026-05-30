import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RentalsDashboardComponent } from './rentals-dashboard/rentals-dashboard.component';

const routes: Routes = [
  {
    path: '',
    component: RentalsDashboardComponent,
    title: 'Rentals',
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class RentalsRoutingModule {}
