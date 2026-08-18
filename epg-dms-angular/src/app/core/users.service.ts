import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { BASE_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private http = inject(HttpClient);

  getUsers(): Promise<any> {
    return firstValueFrom(this.http.get(`${BASE_URL}user`));
  }

  updateUser(id: any, body: any): Promise<any> {
    return firstValueFrom(this.http.put(`${BASE_URL}user/${id}`, body));
  }

  createUser(body: any): Promise<any> {
    return firstValueFrom(this.http.post(`${BASE_URL}auth/signup`, body));
  }

  deleteUser(id: any): Promise<any> {
    return firstValueFrom(this.http.delete(`${BASE_URL}user/${id}`));
  }
}
