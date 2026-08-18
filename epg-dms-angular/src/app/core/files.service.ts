import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { BASE_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class FilesService {
  private http = inject(HttpClient);

  getFiles(page: number, size: number): Promise<any> {
    const params = new HttpParams().set('page', page).set('size', size);
    return firstValueFrom(this.http.get(`${BASE_URL}extraction-task/all-upls`, { params }));
  }

  createFile(file: File): Promise<any> {
    const formData = new FormData();
    formData.append('file', file);
    return firstValueFrom(this.http.post(`${BASE_URL}excels/upload`, formData));
  }

  deleteFile(fileId: any): Promise<any> {
    return firstValueFrom(this.http.delete(`${BASE_URL}connection-fees/delete-by-task/${fileId}`));
  }

  transferFile(fileId: any): Promise<any> {
    return firstValueFrom(this.http.post(`${BASE_URL}connection-fees/${fileId}`, {}));
  }

  fetchSheetData(recordId: any, page: number, size: number, filter: any): Promise<any> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('fileId', recordId ?? '');

    for (const [key, value] of Object.entries(filter)) {
      if (value) params = params.set(key, String(value));
    }

    return firstValueFrom(this.http.get(`${BASE_URL}excels/filter`, { params }));
  }

  downloadFile(fileName: string): Promise<any> {
    const params = new HttpParams().set('fileName', fileName);
    return firstValueFrom(this.http.get(`${BASE_URL}connection-fees/download-ext`, {
      params, responseType: 'blob', observe: 'response'
    }));
  }
}
