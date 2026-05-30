import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';
import { UserRole } from '../../core/models/user-role';
import { AdminHubComponent } from './admin-hub/admin-hub.component';
import { DirectorReportComponent } from './director-report/director-report.component';
import { UserManagementComponent } from './user-management/user-management.component';

const routes: Routes = [
  {
    path: '',
    component: AdminHubComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'users' },
      {
        path: 'activity',
        redirectTo: '/app/activity-logs',
        pathMatch: 'full',
      },
      {
        path: 'reports',
        component: DirectorReportComponent,
        title: 'Listing owner report',
        canActivate: [roleGuard],
        data: { roles: [UserRole.DIRECTOR] },
      },
      {
        path: 'users',
        component: UserManagementComponent,
        title: 'User management',
        canActivate: [roleGuard],
        data: { roles: [UserRole.DIRECTOR, UserRole.ADMIN] },
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AdminRoutingModule {}
