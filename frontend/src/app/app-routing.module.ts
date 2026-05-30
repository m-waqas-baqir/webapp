import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { MainLayoutComponent } from './layout/main-layout.component';
import { LandingPageComponent } from './pages/landing-page/landing-page.component';
import { LoginComponent } from './pages/login/login.component';

const routes: Routes = [
  {
    path: '',
    component: LandingPageComponent,
    title: 'Real Investments',
  },
  {
    path: 'login',
    component: LoginComponent,
    title: 'Sign in',
  },
  {
    path: 'demo',
    redirectTo: '/app/demo',
    pathMatch: 'full',
  },
  {
    path: 'app',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadChildren: () => import('./features/dashboard/dashboard.module').then((m) => m.DashboardModule),
      },
      {
        path: 'demo',
        loadChildren: () => import('./features/demo/demo.module').then((m) => m.DemoModule),
      },
      {
        path: 'my-properties',
        loadChildren: () => import('./features/my-properties/my-properties.module').then((m) => m.MyPropertiesModule),
      },
      {
        path: 'activity-logs',
        loadChildren: () => import('./features/activity-logs/activity-logs.module').then((m) => m.ActivityLogsModule),
      },
      {
        path: 'auth',
        loadChildren: () => import('./features/auth/auth.module').then((m) => m.AuthModule),
      },
      {
        path: 'plots',
        loadChildren: () => import('./features/plots/plots.module').then((m) => m.PlotsModule),
      },
      {
        path: 'rentals',
        loadChildren: () => import('./features/rentals/rentals.module').then((m) => m.RentalsModule),
      },
      {
        path: 'owners',
        loadChildren: () => import('./features/owners/owners.module').then((m) => m.OwnersModule),
      },
      {
        path: 'admin',
        loadChildren: () => import('./features/admin/admin.module').then((m) => m.AdminModule),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { scrollPositionRestoration: 'enabled' })],
  exports: [RouterModule],
})
export class AppRoutingModule {}
