import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Confirm } from '../../components/modals/confirm/confirm';

interface Role { key: string; text: string; }

@Component({
  selector: 'app-users',
  imports: [FormsModule, Confirm],
  templateUrl: './users.html',
  styleUrl: './users.css'
})
export class Users {
  users: any[] = [];
  showModal = false;
  isEditing = false;
  showPassword = false;

  selectedUser: any = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    role: 'აირჩიეთ როლი'
  };

  roles: Role[] = [
    { key: 'ROLE_ADMIN', text: 'ადმინი' },
    { key: 'ROLE_OPERATOR', text: 'ოპერატორი' },
    { key: 'ROLE_MANAGER', text: 'მენეჯერი' },
    { key: 'ROLE_GUEST', text: 'სტუმარი' }
  ];

  get passwordFieldType(): string {
    return this.showPassword ? 'text' : 'password';
  }

  get hasErrors(): boolean {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return !this.selectedUser.firstName
      || !this.selectedUser.lastName
      || !emailRegex.test(this.selectedUser.email)
      || (!this.isEditing && !this.selectedUser.password)
      || this.selectedUser.role === 'აირჩიეთ როლი';
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  openCreateModal(): void {
    this.selectedUser = {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      role: 'აირჩიეთ როლი'
    };
    this.isEditing = false;
    this.showModal = true;
  }

  editUser(user: any): void {
    this.selectedUser = { ...user };
    this.isEditing = true;
    this.showModal = true;
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    return `${day}.${month}.${year}`;
  }
}
