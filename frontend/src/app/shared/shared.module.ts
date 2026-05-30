import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { HasRoleDirective } from '../core/directives/has-role.directive';
import { EmptyStateComponent } from './components/empty-state/empty-state.component';
import { PageHeaderComponent } from './components/page-header/page-header.component';

const materialModules = [
  MatToolbarModule,
  MatSidenavModule,
  MatButtonModule,
  MatIconModule,
  MatListModule,
  MatCardModule,
  MatFormFieldModule,
  MatInputModule,
];

@NgModule({
  declarations: [PageHeaderComponent, EmptyStateComponent, HasRoleDirective],
  imports: [CommonModule, RouterModule, ReactiveFormsModule, ...materialModules],
  exports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    PageHeaderComponent,
    EmptyStateComponent,
    HasRoleDirective,
    ...materialModules,
  ],
})
export class SharedModule {}
