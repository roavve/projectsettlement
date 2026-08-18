import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class FilterService {
  private initial() {
    return {
      region: 'აირჩიეთ რეგიონი',
      serviceCenter: 'აირჩიეთ მ/ც',
      withdrawType: [] as string[],
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
  }

  filter: any = this.initial();

  clearFilter(): void {
    Object.assign(this.filter, this.initial());
  }
}
