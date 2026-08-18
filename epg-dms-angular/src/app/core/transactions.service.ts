import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { BASE_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class TransactionsService {
  private http = inject(HttpClient);

  private readonly dateKeys = [
    'clarificationDateStart', 'clarificationDateEnd',
    'transferDateStart', 'transferDateEnd',
    'extractionDateStart', 'extractionDateEnd'
  ];

  private readonly placeholders = [
    'აირჩიეთ რეგიონი',
    'აირჩიეთ მ/ც',
    'აირჩიეთ სტატუსი',
    'ჩანაწერის სტატუსი',
    'ორდერის სტატუსი'
  ];
  downloadExport(params: HttpParams): Promise<any> {
    return firstValueFrom(this.http.get(`${BASE_URL}connection-fees/download`, {
      params, responseType: 'blob', observe: 'response'
    }));
  }
  buildParams(filter: any, extra: Record<string, any> = {}): HttpParams {
    const obj: Record<string, string> = {};

    for (const [key, value] of Object.entries(extra)) {
      if (value !== undefined && value !== null && value !== '') {
        obj[key] = String(value);
      }
    }

    for (const [key, raw] of Object.entries(filter)) {
      const value: any = raw;
      if (!value) continue;
      if (this.placeholders.includes(value)) continue;
      if (Array.isArray(value)) {
        if (value.length === 0) continue;
        obj[key] = value.join(',');
        continue;
      }
      if (this.dateKeys.includes(key)) {
        let v = String(value);
        if (key.indexOf('extraction') === -1) {
          v += ` ${key.indexOf('Start') !== -1 ? '00' : '24'}:00:00.000000`;
        }
        obj[key] = v;
      } else {
        obj[key] = String(value);
      }
    }

    if (obj['orderN']) {
      obj['orderN'] = obj['orderN'].split(' ').map(o => o.trim()).filter(o => o !== '').join(',');
    }

    return new HttpParams({ fromObject: obj });
  }

  getFees(params: HttpParams): Promise<any> {
    return firstValueFrom(this.http.get(`${BASE_URL}connection-fees/filter`, { params }));
  }

  getRegionsByParentId(parentId: number): Promise<any> {
    return firstValueFrom(this.http.get(`${BASE_URL}business-units/by-parent/${parentId}`));
  }

  getServiceCenters(): Promise<any> {
    return firstValueFrom(this.http.get(`${BASE_URL}business-units/unit-key/62`));
  }
  updateRecord(id: any, body: any): Promise<any> {
    return firstValueFrom(this.http.put(`${BASE_URL}connection-fees/${id}`, body));
  }

  deleteRecord(id: any): Promise<any> {
    return firstValueFrom(this.http.delete(`${BASE_URL}connection-fees/soft-delete/${id}`));
  }

  divide(id: any, amounts: number[]): Promise<any> {
    return firstValueFrom(this.http.post(`${BASE_URL}connection-fees/divide-fee/${id}`, amounts));
  }
}
