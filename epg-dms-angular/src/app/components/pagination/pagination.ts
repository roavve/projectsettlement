import { Component, input, model } from '@angular/core';

@Component({
  selector: 'app-pagination',
  imports: [],
  templateUrl: './pagination.html',
  styleUrl: './pagination.css'
})
export class Pagination {
  currentPage = model<number>(1);
  pageSize = model<number>(20);
  totalPages = model<number>(1);
  totalElements = model<number>(20);
  absolute = input<boolean>(false);

  showOptions = false;

  get startIndex(): number {
    return (this.currentPage() - 1) * this.pageSize() + 1;
  }

  get endIndex(): number {
    return Math.min(this.startIndex + this.pageSize() - 1, this.totalElements());
  }

  get options(): number[] {
    const predefined = [10, 20, 50, 100];
    const available = predefined.filter(o => o < this.totalElements());
    available.push(this.totalElements());
    return available;
  }

  previousPage(): void {
    if (this.currentPage() > 1) this.currentPage.set(this.currentPage() - 1);
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages()) this.currentPage.set(this.currentPage() + 1);
  }

  onEnter(event: Event): void {
    const page = parseInt((event.target as HTMLInputElement).value, 10);
    this.currentPage.set(page >= 1 && page <= this.totalPages() ? page : 1);
  }

  validatePage(event: Event): void {
    const el = event.target as HTMLInputElement;
    let page = parseInt(el.value, 10);
    if (el.value) {
      page = Math.max(1, Math.min(page, this.totalPages()));
      el.value = String(page);
    }
  }

  selectOption(option: number): void {
    this.showOptions = false;
    this.pageSize.set(option);
    this.currentPage.set(1);
  }
}
