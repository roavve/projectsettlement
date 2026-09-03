import { Component, input, model, inject, ChangeDetectorRef, OnInit } from '@angular/core';
import { UsersService } from '../../core/users.service';

interface User { id: number; firstName: string; lastName: string; }

@Component({
  selector: 'app-user-select',
  imports: [],
  templateUrl: './user-select.html',
  styleUrl: './user-select.css'
})
export class UserSelect implements OnInit {
  private api = inject(UsersService);
  private cdr = inject(ChangeDetectorRef);

  modelValue = model<number | undefined>(undefined);
  placeholder = input('აირჩიეთ მომხმარებელი');

  users: User[] = [];

  async ngOnInit(): Promise<void> {
    try {
      this.users = await this.api.getUsers();
    } catch (error) {
      console.error('Error fetching users:', error);
    }
    this.cdr.markForCheck();
  }

  handleSelect(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.modelValue.set(value === '' ? undefined : Number(value));
  }

  clearSelection(): void {
    this.modelValue.set(undefined);
  }
}
