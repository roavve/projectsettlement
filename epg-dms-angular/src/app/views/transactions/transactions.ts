import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FilterDate } from '../../components/filter-date/filter-date';
import { UserSelect } from '../../components/user-select/user-select';
import { RecursiveRow } from '../../components/recursive-row/recursive-row';
import { Pagination } from '../../components/pagination/pagination';
import { Confirm } from '../../components/modals/confirm/confirm';

interface SortOption { text: string; by: string; dir: string; }

@Component({
  selector: 'app-transactions',
  imports: [FormsModule, FilterDate, UserSelect, RecursiveRow, Pagination, Confirm],
  templateUrl: './transactions.html',
  styleUrl: './transactions.css'
})
export class Transactions {
  isVisible = false;
  isTypeDropdownOpen = false;

  _regions: any[] = [];
  _serviceCenters: any[] = [];
  records: any[] | undefined = [];

  user: any = { role: 'ROLE_ADMIN' };
  currentPage = 1;
  pageSize = 20;
  totalPages = 1;
  totalElements = 20;

  _error = false;
  searchTerm = '';
  isDropdownOpen = false;
  canceledProject = '';
  sc: any[] = [];

  amount = '';
  remainder: number | undefined;

  similarPayments: any[] = [];
  similarPage = 1;
  similarPageSize = 10;
  similarTotalPages = 0;
  similarTotalElements = 0;
  similarLoading = false;

  extractionFee: any = {
    orderN: '', orderStatus: null, region: '', serviceCenter: '', projectID: '',
    withdrawType: '', note: '', tax: '', totalAmount: '', purpose: '', description: '',
    clarificationDate: null, transferDate: null, extractionDate: null,
    paymentOrderSentDate: null, treasuryRefundDate: null, transferPerson: null
  };

  filter: any = {
    region: 'აირჩიეთ რეგიონი',
    serviceCenter: 'აირჩიეთ მ/ც',
    withdrawType: [],
    status: 'ჩანაწერის სტატუსი',
    orderStatus: 'ორდერის სტატუსი',
    totalAmountStart: undefined,
    totalAmountEnd: undefined,
    orderN: '',
    projectID: '',
    id: '',
    purpose: '',
    tax: '',
    description: '',
    clarificationDateStart: undefined,
    clarificationDateEnd: undefined,
    transferDateStart: undefined,
    transferDateEnd: undefined,
    extractionDateStart: undefined,
    extractionDateEnd: undefined,
    note: '',
    history: '',
    change_person: undefined,
  };

  withdrawTypes: string[] = [
    '1 (პირველი გადახდა)',
    '2 (მეორე გადახდა)',
    '3 (სრული საფასურის გადახდა)',
    '4 (ერთანი გადახდა, გადანაწილებული რამოდენიმე პროექტის საფასურად)',
    '5 (სავარაუდოდ არაა ახალი მიერთების საფასური)',
    '6 (თანხის დაბრუნება)',
    '7 (გადანაწილებული გადახდა / რამოდენიმეჯერ გადახდა)',
    '8 (სააბონენტო ბარათზე თანხის დასმა)',
    '9 (ხაზის მშენებლობა / არარეგულირებული პროექტები (პირველი ან სრული გადახდა))',
    '10 (სისტემის ნებართვის საფასური)',
    '19 (ხაზის მშენებლობა / არარეგულირებული პროექტები (მეორე გადახდა))',
    '11 (მიწოდებიდან გადმოტანილი თანხა)',
    '12 (ჯარიმის გადატანა)',
    '13 (საპროექტო ტრასის შეტანხმება)',
    '14 (ჰესები DDSH)',
    '15 (ჰესები DDNA)',
    'null'
  ];

  sortOptions: SortOption[] = [
    { text: 'N კლებადი', by: 'id', dir: 'DESC' },
    { text: 'N ზრდადი', by: 'id', dir: 'ASC' },
    { text: 'ორდერის N კლებადი', by: 'orderN', dir: 'DESC' },
    { text: 'ორდერის N ზრდადი', by: 'orderN', dir: 'ASC' },
    { text: 'რეგიონი კლებადი', by: 'region', dir: 'DESC' },
    { text: 'რეგიონი ზრდადი', by: 'region', dir: 'ASC' },
    { text: 'მ/ც კლებადი', by: 'serviceCenter', dir: 'DESC' },
    { text: 'მ/ც ზრდადი', by: 'serviceCenter', dir: 'ASC' },
    { text: 'პროექტის N კლებადი', by: 'projectID', dir: 'DESC' },
    { text: 'პროექტის N ზრდადი', by: 'projectID', dir: 'ASC' },
    { text: 'ტიპი კლებადი', by: 'withdrawType', dir: 'DESC' },
    { text: 'ტიპი ზრდადი', by: 'withdrawType', dir: 'ASC' },
    { text: 'გარკვევის თარიღი კლებადი', by: 'clarificationDate', dir: 'DESC' },
    { text: 'გარკვევის თარიღი ზრდადი', by: 'clarificationDate', dir: 'ASC' },
    { text: 'გადმოტანის თარიღი კლებადი', by: 'transferDate', dir: 'DESC' },
    { text: 'გადმოტანის თარიღი ზრდადი', by: 'transferDate', dir: 'ASC' },
    { text: 'ჩარიცხვის თარიღი კლებადი', by: 'extractionDate', dir: 'DESC' },
    { text: 'ჩარიცხვის თარიღი ზრდადი', by: 'extractionDate', dir: 'ASC' },
    { text: 'ბრუნვა კლებადი', by: 'totalAmount', dir: 'DESC' },
    { text: 'ბრუნვა თანხა ზრდადი', by: 'totalAmount', dir: 'ASC' },
  ];

  sortByDir: SortOption = this.sortOptions[16];

  get filteredServiceCenters(): any[] {
    return this.sc.filter(c => c.name.toLowerCase().includes(this.searchTerm));
  }

  closeDropdown(): void {
    setTimeout(() => this.isDropdownOpen = false, 200);
  }

  selectCenter(centerName: string, parentName: string): void {
    this.extractionFee.serviceCenter = centerName;
    this.searchTerm = centerName;
    this.extractionFee.region = parentName;
    this.isDropdownOpen = false;
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

  validateAmount(): boolean {
    const regex = /^(\d+(\.\d+)?(\s\d+(\.\d+)?)*)$/;
    const clean = this.amount.replace(/\s+/g, ' ').trim();
    const numbers = clean.split(' ').map(Number);
    const sum = numbers.reduce((a, b) => a + b, 0);
    return regex.test(clean) && numbers.every(n => n > 0) && sum <= (this.remainder ?? 0);
  }

  extractStartingNumber(type: string): string {
    if (type === 'null') return type;
    const match = type.match(/^\d+/);
    return match ? match[0] : '';
  }

  toggleType(type: string): void {
    const index = this.filter.withdrawType.indexOf(type);
    if (index === -1) {
      this.filter.withdrawType.push(type);
    } else {
      this.filter.withdrawType.splice(index, 1);
    }
  }

  clearRegion(): void {
    this.filter.region = 'აირჩიეთ რეგიონი';
    this._serviceCenters = [];
    this.clearSC();
  }

  clearSC(): void { this.filter.serviceCenter = 'აირჩიეთ მ/ც'; }
  clearType(): void { this.filter.withdrawType = []; }
  clearStatus(): void { this.filter.status = 'ჩანაწერის სტატუსი'; }
  clearOrderStatus(): void { this.filter.orderStatus = 'ორდერის სტატუსი'; }
  clearSortByDir(): void { this.sortByDir = this.sortOptions[16]; }

  clearClarification(): void {
    this.filter.clarificationDateStart = undefined;
    this.filter.clarificationDateEnd = undefined;
  }

  clearTransfer(): void {
    this.filter.transferDateStart = undefined;
    this.filter.transferDateEnd = undefined;
  }

  clearExtraction(): void {
    this.filter.extractionDateStart = undefined;
    this.filter.extractionDateEnd = undefined;
  }
}
