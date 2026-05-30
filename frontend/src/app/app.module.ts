import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CoreModule } from './core/core.module';
import { SharedModule } from './shared/shared.module';
import { MainLayoutComponent } from './layout/main-layout.component';
import { LandingPageComponent } from './pages/landing-page/landing-page.component';
import { LoginComponent } from './pages/login/login.component';
import { DhaMapComponent } from './shared/components/dha-map/dha-map.component';
import { ThemeToggleComponent } from './shared/components/theme-toggle/theme-toggle.component';

@NgModule({
  declarations: [AppComponent, MainLayoutComponent, LandingPageComponent, LoginComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    MatSnackBarModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    CoreModule,
    SharedModule,
    AppRoutingModule,
    ThemeToggleComponent,
    DhaMapComponent,
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
