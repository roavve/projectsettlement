import { Component, model, output, signal } from '@angular/core';
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

  selectedFileName = signal('ფაილი არჩეული არ არის');
  showUploadButton = signal(false);
  message = signal('');

  triggerFileInput(input: HTMLInputElement): void {
    input.click();
  }

  handleFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.modelValue.set(file);
      this.selectedFileName.set(file.name);
      this.showUploadButton.set(true);
    }
    input.value = '';
  }

  upload(): void {
    try {
      this.createFile.emit();
      this.selectedFileName.set('');
      this.showUploadButton.set(false);
      this.message.set('ფაილი აიტვირთა წარმატებით');
    } catch (e) {
      this.message.set('შეცდომა ფაილის დამუშავებისას');
    }
  }
}
