import { Component, inject, ChangeDetectorRef, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { TransactionsService } from '../../core/transactions.service';
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
export class Transactions implements OnInit {
  private api = inject(TransactionsService);
  private auth = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  isVisible = false;
  isTypeDropdownOpen = false;

  regions: any[] = [];
  _regions: any[] = [];
  _serviceCenters: any[] = [];
  records: any[] | undefined = [];

  user: any = null;
  currentPage = 1;
  pageSize = 20;
  totalPages = 1;
  totalElements = 20;

  sortBy: string | undefined;
  sortDir: string | undefined;

  _error = false;
  searchTerm = '';
  isDropdownOpen = false;
  canceledProject = '';
  sc: any[] = [];

  amount = '';
  remainder: number | undefined;
  divideId: any;

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

  async ngOnInit(): Promise<void> {
    this.user = this.auth.user();
    await this.loadRegions();
    await this.getFees();
    await this.loadServiceCenters();
  }

  async loadRegions(parentId = 68): Promise<void> {
    try {
      const res: any = await this.api.getRegionsByParentId(parentId);
      if (parentId === 68) {
        this.regions = res.data;
        this._regions = res.data;
      } else {
        this._serviceCenters = res.data;
      }
      this.cdr.markForCheck();
    } catch (error) {
      console.error('Error fetching data:', error);
    }
  }

  async loadServiceCenters(): Promise<void> {
    try {
      this.sc = await this.api.getServiceCenters();
      this.cdr.markForCheck();
    } catch (error) {
      console.error('Error service centers data:', error);
    }
  }

  async getFees(): Promise<void> {
    this.records = undefined;
    this.cdr.markForCheck();
    const params = this.api.buildParams(this.filter, {
      sortBy: this.sortBy,
      sortDir: this.sortDir,
      page: this.currentPage,
      size: this.pageSize
    });
    try {
      const data: any = await this.api.getFees(params);
      this.records = data.content;
      this.addShowProperty();
      this.totalPages = data.page.totalPages;
      this.totalElements = data.page.totalElements;
    } catch (error) {
      console.error('Error fetching sheets:', error);
    }
    this.cdr.markForCheck();
  }

  private addShowProperty(): void {
    const stack = [...(this.records ?? [])];
    while (stack.length > 0) {
      const record = stack.pop();
      record.show = false;
      const remainderChild = record.children.filter((c: any) => c.status === 'REMINDER');
      if (record.children && record.children.length > 0) {
        record.remainder = remainderChild.length > 0 ? remainderChild[0].totalAmount : 0;
        stack.push(...record.children);
      } else {
        record.remainder = record.totalAmount;
      }
    }
  }

  async handleEditClick(extraction: any): Promise<void> {
    this._error = false;
    this.searchTerm = extraction.serviceCenter;
    this.canceledProject = (extraction.canceledProject ?? []).join(' ');
    this.extractionFee = { ...extraction };

    (document.getElementById('my_modal_1') as HTMLDialogElement).showModal();

    if (!this.extractionFee.region) {
      this.extractionFee.region = 'აირჩიეთ რეგიონი';
    } else {
      const match = this.regions.find(r => r.name === extraction.region);
      if (match) await this.loadRegions(match.id);
    }

    if (!this.extractionFee.serviceCenter) {
      this.extractionFee.serviceCenter = 'აირჩიეთ მ/ც';
    }

    if (!this.extractionFee.withdrawType) {
      this.extractionFee.withdrawType = 'აირჩიეთ ტიპი';
    }

    this.cdr.markForCheck();
  }

  handleDivideClick(args: any[]): void {
    (document.getElementById('my_modal_7') as HTMLDialogElement).showModal();
    this.divideId = args[0];
    this.remainder = args[1];
    this.cdr.markForCheck();
  }

  async getSelectedParentId(event: Event): Promise<void> {
    const opt = (event.target as HTMLSelectElement).selectedOptions[0];
    await this.loadRegions(Number(opt.getAttribute('data-id')));
  }

  async handleFilter(): Promise<void> {
    this.currentPage = 1;
    await this.getFees();
  }

  async handleClear(): Promise<void> {
    this.filter = {
      region: 'აირჩიეთ რეგიონი', serviceCenter: 'აირჩიეთ მ/ც', withdrawType: [],
      status: 'ჩანაწერის სტატუსი', orderStatus: 'ორდერის სტატუსი',
      totalAmountStart: undefined, totalAmountEnd: undefined,
      orderN: '', projectID: '', id: '', purpose: '', tax: '', description: '',
      clarificationDateStart: undefined, clarificationDateEnd: undefined,
      transferDateStart: undefined, transferDateEnd: undefined,
      extractionDateStart: undefined, extractionDateEnd: undefined,
      note: '', history: '', change_person: undefined
    };
    this._serviceCenters = [];
    this.clearSortByDir();
    await this.handleFilter();
  }

  async onSortChange(): Promise<void> {
    this.sortBy = this.sortByDir.by;
    this.sortDir = this.sortByDir.dir;
    this.currentPage = 1;
    await this.getFees();
  }

  async onPageChange(page: number): Promise<void> {
    this.currentPage = page;
    await this.getFees();
  }

  async onPageSizeChange(size: number): Promise<void> {
    this.pageSize = size;
    this.currentPage = 1;
    await this.getFees();
  }

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
  wasCleared = false;

  handleClearClick(): void {
    this.wasCleared = true;
    this._error = false;
    this.extractionFee.orderN = '';
    this.extractionFee.orderStatus = null;
    this.extractionFee.projectID = '';
    this.extractionFee.withdrawType = 'აირჩიეთ ტიპი';
    this.extractionFee.paymentOrderSentDate = null;
    this.extractionFee.treasuryRefundDate = null;
    this.extractionFee.clarificationDate = null;
    this.extractionFee.note = '';
    this.searchTerm = '';
    this.extractionFee.region = 'აირჩიეთ რეგიონი';
    this.extractionFee.serviceCenter = 'აირჩიეთ მ/ც';
    this.canceledProject = '';
  }

  private clearPlaceholders(): void {
    if (this.extractionFee.region === 'აირჩიეთ რეგიონი' || this.extractionFee.region === '') {
      this.extractionFee.region = '';
    }
    if (this.extractionFee.serviceCenter === 'აირჩიეთ მ/ც') {
      this.extractionFee.serviceCenter = '';
    }
    if (this.extractionFee.withdrawType === 'აირჩიეთ ტიპი') {
      this.extractionFee.withdrawType = '';
    }
  }

  private async performSaveActions(): Promise<void> {
    const trimmed = this.canceledProject.trim();
    this.extractionFee.canceledProject = trimmed === '' ? [] : trimmed.split(/\s+/);
    this.canceledProject = '';
    this.wasCleared = false;

    try {
      const body = { ...this.extractionFee };
      delete body.changePerson;
      delete body.transferPerson;
      if (!body.note) body.note = '';
      if (!body.projectID) body.projectID = '';
      if (!body.orderStatus) body.orderStatus = null;
      await this.api.updateRecord(body.id, body);
    } catch (error) {
      console.error('Error updating fee:', error);
    }

    await this.getFees();
    this.searchTerm = '';
    (document.getElementById('my_modal_1') as HTMLDialogElement).close();
    this.cdr.markForCheck();
  }

  async handleSaveClick(): Promise<void> {
    const { region, withdrawType, serviceCenter, projectID, orderN } = this.extractionFee;
    const isRefund = withdrawType === '6 (თანხის დაბრუნება)';
    const isAdmin = this.auth.user()?.role === 'ROLE_ADMIN';
    const isTypeException = (orderN ?? '').trim().toUpperCase() === 'N/A';

    const isRegionInvalid = !isRefund && region === 'აირჩიეთ რეგიონი';
    const isServiceCenterInvalid = !isRefund && serviceCenter === 'აირჩიეთ მ/ც';
    const isWithdrawTypeInvalid = withdrawType === 'აირჩიეთ ტიპი';
    const isProjectInvalid = !isAdmin && !projectID;

    const isInvalid = isRegionInvalid || isServiceCenterInvalid || isWithdrawTypeInvalid || isProjectInvalid;

    if (this.wasCleared) {
      this.clearPlaceholders();
      await this.performSaveActions();
    } else if (isTypeException) {
      this.clearPlaceholders();
      if (!this.extractionFee.orderStatus || this.extractionFee.orderStatus === 'ORDER_INCOMPLETE') {
        this.extractionFee.orderStatus = 'ORDER_COMPLETE';
      }
      await this.performSaveActions();
    } else if (isInvalid) {
      if (this.canceledProject) {
        this.clearPlaceholders();
        await this.performSaveActions();
      } else {
        this._error = true;
        this.cdr.markForCheck();
      }
    } else {
      await this.performSaveActions();
    }
  }

  async hdc(): Promise<void> {
    this.canceledProject = '';
    try {
      await this.api.deleteRecord(this.extractionFee.id);
    } catch (error) {
      console.error(error);
    }
    await this.getFees();
    (document.getElementById('my_modal_1') as HTMLDialogElement).close();
    this.cdr.markForCheck();
  }

  async handleDivision(): Promise<void> {
    const amounts = this.amount.replace(/\s+/g, ' ').trim().split(' ').map(Number);
    try {
      await this.api.divide(this.divideId, amounts);
    } catch (error) {
      console.error(error);
    }
    await this.getFees();
    this.divideId = undefined;
    this.amount = '';
    this.remainder = undefined;
    this.cdr.markForCheck();
  }

  cancelModal(): void {
    this.searchTerm = '';
    this.canceledProject = '';
    this.wasCleared = false;
    (document.getElementById('my_modal_1') as HTMLDialogElement).close();
  }

  openDeleteModal(): void {
    (document.getElementById('delete_transaction_modal') as HTMLDialogElement).showModal();
  }
}
