import { Component, input, model } from '@angular/core';

interface User { id: number; firstName: string; lastName: string; }

@Component({
  selector: 'app-user-select',
  imports: [],
  templateUrl: './user-select.html',
  styleUrl: './user-select.css'
})
export class UserSelect {
  modelValue = model<number | undefined>(undefined);
  placeholder = input('აირჩიეთ მომხმარებელი');

  users: User[] = [];

  handleSelect(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.modelValue.set(value === '' ? undefined : Number(value));
  }

  clearSelection(): void {
    this.modelValue.set(undefined);
  }
}
