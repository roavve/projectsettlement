import { Component, input, effect, signal, inject, ElementRef, OnDestroy, afterNextRender } from '@angular/core';

@Component({
  selector: 'app-message',
  imports: [],
  templateUrl: './message.html',
  styleUrl: './message.css'
})
export class Message implements OnDestroy {
  private el = inject(ElementRef);

  message = input.required<string>();
  show = signal(false);

  constructor() {
    afterNextRender(() => {
      document.body.appendChild(this.el.nativeElement);
    });

    effect(() => {
      this.message();
      this.show.set(true);
      setTimeout(() => this.show.set(false), 3000);
    });
  }

  ngOnDestroy(): void {
    this.el.nativeElement.remove();
  }
}
