import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-filter-date',
  imports: [FormsModule],
  templateUrl: './filter-date.html',
  styleUrl: './filter-date.css'
})
export class FilterDate {
  label = input.required<string>();
  startDate = model<string | null>(null);
  endDate = model<string | null>(null);
  clear = output<void>();

  get showClear(): boolean {
    return !!(this.startDate() || this.endDate());
  }

  handleClick(): void {
    this.clear.emit();
  }
}
