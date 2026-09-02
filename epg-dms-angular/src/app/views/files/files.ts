import { Component, inject, ChangeDetectorRef, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FileInput } from '../../components/uploads/file-input/file-input';
import { Pagination } from '../../components/pagination/pagination';
import { Confirm } from '../../components/modals/confirm/confirm';
import { AuthService } from '../../core/auth.service';
import { FilesService } from '../../core/files.service';

@Component({
  selector: 'app-files',
  imports: [FormsModule, FileInput, Pagination, Confirm],
  templateUrl: './files.html',
  styleUrl: './files.css'
})
export class Files implements OnInit {
  private api = inject(FilesService);
  private auth = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  user: any = null;

  files: any[] | undefined = [];
  selectedFile: File | null = null;

  currentPage = 1;
  pageSize = 20;
  totalPages = 1;
  totalElements = 20;

  sheet: any[] | undefined = [];
  recordId: any;
  _currentPage = 1;
  _pageSize = 20;
  _totalPages = 1;
  _totalElements = 20;

  deleteId: any;
  transferId: any;

  details: any = { amount: undefined, total: undefined, ok: undefined, warning: undefined };

  filter: any = {
    startDate: undefined,
    endDate: undefined,
    totalAmountStart: undefined,
    totalAmountEnd: undefined,
    status: ''
  };

  async ngOnInit(): Promise<void> {
    this.user = this.auth.user();
    await this.getFiles();
  }

  async getFiles(): Promise<void> {
    try {
      this.files = undefined;
      this.cdr.markForCheck();
      const res: any = await this.api.getFiles(this.currentPage, this.pageSize);
      this.files = res.data.content;
      this.totalPages = res.data.page.totalPages;
      this.totalElements = res.data.page.totalElements;
    } catch (error) {
      console.error('Error fetching files:', error);
    }
    this.cdr.markForCheck();
  }

  async createFile(): Promise<void> {
    if (!this.selectedFile) return;
    try {
      await this.api.createFile(this.selectedFile);
      await this.getFiles();
    } catch (error) {
      console.error('Error creating sheet:', error);
    }
  }

  async deleteFile(): Promise<void> {
    try {
      await this.api.deleteFile(this.deleteId);
      (document.getElementById('delete_file_modal') as HTMLDialogElement).close();
      await this.getFiles();
    } catch (error) {
      console.error('Error deleting sheet:', error);
    }
  }

  async transferFile(): Promise<void> {
    try {
      await this.api.transferFile(this.transferId);
      (document.getElementById('transfer_file_modal') as HTMLDialogElement).close();
      await this.getFiles();
    } catch (error) {
      console.error('Error saving sheet:', error);
    }
  }

  async fetchSheetData(): Promise<void> {
    this.sheet = undefined;
    this.cdr.markForCheck();
    try {
      const res: any = await this.api.fetchSheetData(this.recordId, this._currentPage, this._pageSize, this.filter);
      this.sheet = res.excPage.content;
      this._totalPages = res.excPage.page.totalPages;
      this._totalElements = res.excPage.page.totalElements;
      this.details.ok = res.ok;
      this.details.warning = res.warn;
      this.details.total = res.excPage.page.totalElements;
      this.details.amount = res.totalAmountSum;
    } catch (error) {
      console.error('Error fetching sheet data:', error);
    }
    this.cdr.markForCheck();
  }

  async openDetails(file: any): Promise<void> {
    this.recordId = file.id;
    (document.getElementById('my_modal_1') as HTMLDialogElement).showModal();
    await this.fetchSheetData();
  }

  openDeleteModal(file: any): void {
    this.deleteId = file.id;
    (document.getElementById('delete_file_modal') as HTMLDialogElement).showModal();
  }

  openTransferModal(file: any): void {
    this.transferId = file.id;
    (document.getElementById('transfer_file_modal') as HTMLDialogElement).showModal();
  }

  async download(fileName: string): Promise<void> {
    try {
      const response: any = await this.api.downloadFile(fileName);
      let downloadName = fileName;
      const cd = response.headers.get('content-disposition');
      if (cd) {
        const match = cd.match(/filename\*?=UTF-8''([^;]+)/);
        if (match && match[1]) downloadName = decodeURIComponent(match[1]);
      }
      const url = window.URL.createObjectURL(response.body);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', downloadName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('File download failed:', err);
    }
  }

  async handleFilter(): Promise<void> {
    this._currentPage = 1;
    await this.fetchSheetData();
  }

  async handleClear(): Promise<void> {
    this.filter = {
      startDate: undefined, endDate: undefined,
      totalAmountStart: undefined, totalAmountEnd: undefined, status: ''
    };
    await this.handleFilter();
  }

  closeDetails(): void {
    this._currentPage = 1;
    this.sheet = undefined;
    this.filter = {
      startDate: undefined, endDate: undefined,
      totalAmountStart: undefined, totalAmountEnd: undefined, status: ''
    };
    (document.getElementById('my_modal_1') as HTMLDialogElement).close();
  }

  async onPageChange(page: number): Promise<void> {
    this.currentPage = page;
    await this.getFiles();
  }

  async onPageSizeChange(size: number): Promise<void> {
    this.pageSize = size;
    this.currentPage = 1;
    await this.getFiles();
  }

  async onDetailsPageChange(page: number): Promise<void> {
    this._currentPage = page;
    await this.fetchSheetData();
  }

  async onDetailsPageSizeChange(size: number): Promise<void> {
    this._pageSize = size;
    this._currentPage = 1;
    await this.fetchSheetData();
  }

  baseName(path: string): string {
    return path.split(/[\\/]/).at(-1) ?? '';
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    return `${day}.${month}.${year}`;
  }
}
