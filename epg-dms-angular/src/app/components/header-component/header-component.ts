import { Component } from '@angular/core';
import { Navigation } from '../header/navigation/navigation';

@Component({
  selector: 'app-header-component',
  imports: [Navigation],
  templateUrl: './header-component.html',
  styleUrl: './header-component.css'
})
export class HeaderComponent {}
