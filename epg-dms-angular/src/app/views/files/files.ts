import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FileInput } from '../../components/uploads/file-input/file-input';
import { Pagination } from '../../components/pagination/pagination';
import { Confirm } from '../../components/modals/confirm/confirm';

@Component({
  selector: 'app-files',
  imports: [FormsModule, FileInput, Pagination, Confirm],
  templateUrl: './files.html',
  styleUrl: './files.css'
})
export class Files {
  user: any = { role: 'ROLE_ADMIN' };

  files: any[] | undefined = [];
  selectedFile: File | null = null;

  currentPage = 1;
  pageSize = 20;
  totalPages = 1;
  totalElements = 20;

  sheet: any[] | undefined = [];
  _currentPage = 1;
  _pageSize = 20;
  _totalPages = 1;
  _totalElements = 20;

  details: any = { amount: '', total: '', ok: '', warning: '' };

  filter: any = {
    startDate: undefined,
    endDate: undefined,
    totalAmountStart: undefined,
    totalAmountEnd: undefined,
    status: ''
  };

  baseName(path: string): string {
    return path.split('\\').at(-1) ?? '';
  }

  formatDate(dateString: string, includeTime = false): string {
    const date = new Date(dateString);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    if (includeTime) {
      const h = date.getHours().toString().padStart(2, '0');
      const m = date.getMinutes().toString().padStart(2, '0');
      const s = date.getSeconds().toString().padStart(2, '0');
      return `${day}.${month}.${year} ${h}:${m}:${s}`;
    }
    return `${day}.${month}.${year}`;
  }
}
