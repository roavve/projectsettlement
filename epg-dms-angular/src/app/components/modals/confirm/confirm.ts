import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-confirm',
  imports: [],
  templateUrl: './confirm.html',
  styleUrl: './confirm.css'
})
export class Confirm {
  modalId = input.required<string>();
  question = input('ნამდვილად გსურთ წაშლა?');
  acceptText = input('დიახ');
  declineText = input('არა');

  accept = output<void>();
}
