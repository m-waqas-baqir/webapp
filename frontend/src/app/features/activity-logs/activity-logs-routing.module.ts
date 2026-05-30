import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';
import { UserRole } from '../../core/models/user-role';
import { ActivityLogsComponent } from './activity-logs.component';

const routes: Routes = [
  {
    path: '',
    component: ActivityLogsComponent,
    canActivate: [roleGuard],
    data: { roles: [UserRole.DIRECTOR, UserRole.ADMIN] },
    title: 'Activity logs',
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ActivityLogsRoutingModule {}
