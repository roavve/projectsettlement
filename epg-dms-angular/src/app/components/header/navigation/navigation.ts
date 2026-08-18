import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { FilterService } from '../../../core/filter.service';
import { TransactionsService } from '../../../core/transactions.service';

@Component({
  selector: 'app-navigation',
  imports: [RouterLink],
  templateUrl: './navigation.html',
  styleUrl: './navigation.css'
})
export class Navigation {
  private auth = inject(AuthService);
  private router = inject(Router);
  private filterStore = inject(FilterService);
  private api = inject(TransactionsService);
  private cdr = inject(ChangeDetectorRef);

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

  async downloadExport(): Promise<void> {
    if (this.exportLoading) return;
    this.exportLoading = true;
    this.cdr.markForCheck();

    try {
      const params = this.api.buildParams(this.filterStore.filter);
      const response: any = await this.api.downloadExport(params);

      let fileName: string | null = null;
      const cd = response.headers.get('content-disposition');
      if (cd) {
        const idx = cd.toLowerCase().indexOf('filename=');
        if (idx !== -1) {
          fileName = cd.substring(idx + 9).trim().replace(/^"(.*)"$/, '$1');
        }
      }

      const url = window.URL.createObjectURL(response.body);
      const link = document.createElement('a');
      link.href = url;
      if (fileName) link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Export download failed:', err);
    } finally {
      this.exportLoading = false;
      this.cdr.markForCheck();
    }
  }

  async logout(): Promise<void> {
    try {
      await this.auth.logout();
      await this.router.navigate(['/login']);
    } catch (error) {
      console.error('Logout failed:', error);
    }
  }
}
