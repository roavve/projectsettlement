import { Component, input, output } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';

@Component({
  selector: 'tbody[app-recursive-row]',
  imports: [NgTemplateOutlet],
  templateUrl: './recursive-row.html',
  styleUrl: './recursive-row.css'
})
export class RecursiveRow {
  records = input.required<any[]>();
  user = input<any>({ role: 'ROLE_ADMIN' });

  handleEditClick = output<any>();
  handleDivideClick = output<any[]>();

  onEdit(extraction: any): void {
    if (extraction.status !== 'CANCELED' && extraction.status !== 'REMINDER') {
      this.handleEditClick.emit(extraction);
    }
  }

  formatDate(dateString: string, includeTime = false): string {
    const date = new Date(dateString);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    if (includeTime) {
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      const seconds = date.getSeconds().toString().padStart(2, '0');
      return `${day}.${month}.${year} ${hours}:${minutes}:${seconds}`;
    }
    return `${day}.${month}.${year}`;
  }

  marginFor(level: number, extraction: any): string {
    return `${level * 10 + (level !== 0 && extraction.children.length === 0 ? 20 : 0)}px`;
  }
}
