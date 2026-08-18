import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';

@Component({
  selector: 'app-navigation',
  imports: [RouterLink],
  templateUrl: './navigation.html',
  styleUrl: './navigation.css'
})
export class Navigation {
  private auth = inject(AuthService);
  private router = inject(Router);

  exportLoading = false;

  get isAuthenticated(): boolean {
    return this.auth.isAuthenticated();
  }

  showFiles = (): boolean =>
    this.auth.isAuthenticated() && this.auth.filesRoles.includes(this.auth.user()?.role);

  showTransactions = (): boolean =>
    this.auth.isAuthenticated() && this.auth.transactionsRoles.includes(this.auth.user()?.role);

  showUsers = (): boolean =>
    this.auth.isAuthenticated() && this.auth.usersRoles.includes(this.auth.user()?.role);

  showFileExport = (): boolean =>
    this.auth.isAuthenticated() && this.auth.fileExportRoles.includes(this.auth.user()?.role) && this.checkCurrentRoute('/');

  checkCurrentRoute = (path: string): boolean => this.router.url === path;

  getUserName = (): string => {
    const u = this.auth.user();
    return u?.firstName ? `${u.firstName[0]}. ${u.lastName}` : '';
  };

  async logout(): Promise<void> {
    try {
      await this.auth.logout();
      await this.router.navigate(['/login']);
    } catch (error) {
      console.error('Logout failed:', error);
    }
  }
}
