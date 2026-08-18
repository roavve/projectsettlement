import { Component, model, output } from '@angular/core';
import { Message } from '../../modals/message/message';

@Component({
  selector: 'app-file-input',
  imports: [Message],
  templateUrl: './file-input.html',
  styleUrl: './file-input.css'
})
export class FileInput {
  modelValue = model<File | null>(null);
  createFile = output<void>();

  selectedFileName = 'ფაილი არჩეული არ არის';
  showUploadButton = false;
  message = '';
}
