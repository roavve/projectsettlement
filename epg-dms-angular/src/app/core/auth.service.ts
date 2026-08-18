import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { BASE_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  readonly filesRoles = ['ROLE_MANAGER', 'ROLE_ADMIN'];
  readonly transactionsRoles = ['ROLE_GUEST', 'ROLE_OPERATOR', 'ROLE_MANAGER', 'ROLE_ADMIN'];
  readonly usersRoles = ['ROLE_ADMIN'];
  readonly fileExportRoles = ['ROLE_GUEST', 'ROLE_OPERATOR', 'ROLE_MANAGER', 'ROLE_ADMIN'];

  user = signal<any>(null);
  token = signal<string | null>(null);
  isAuthenticated = signal(false);

  async login(email: string, password: string): Promise<void> {
    const data: any = await firstValueFrom(
      this.http.post(`${BASE_URL}auth/signin`, { email, password })
    );
    this.token.set(data.jwtAuthenticationResponse.token);
    this.user.set(data.user);
    this.isAuthenticated.set(true);
    this.setCookie('auth_token', data.jwtAuthenticationResponse.token, 1);
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post(`${BASE_URL}auth/logout`, {}));
      this.token.set(null);
      this.user.set(null);
      this.isAuthenticated.set(false);
      this.deleteCookie('auth_token');
    } catch (error) {
      console.error('Logout failed:', error);
    }
  }

  async restoreSession(): Promise<void> {
    const savedToken = this.getCookie('auth_token');
    if (savedToken) {
      this.token.set(savedToken);
      this.isAuthenticated.set(true);
      try {
        const data = await firstValueFrom(this.http.get(`${BASE_URL}auth/user`));
        this.user.set(data);
      } catch (error) {
        console.error('Failed to restore session:', error);
        await this.logout();
      }
    }
  }

  private setCookie(name: string, value: string, days: number): void {
    const expires = new Date(Date.now() + days * 864e5).toUTCString();
    document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/`;
  }

  private getCookie(name: string): string | null {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? decodeURIComponent(match[2]) : null;
  }

  private deleteCookie(name: string): void {
    document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
  }
}
