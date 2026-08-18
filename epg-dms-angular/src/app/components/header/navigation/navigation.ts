import { Component } from '@angular/core';

@Component({
  selector: 'app-navigation',
  imports: [],
  templateUrl: './navigation.html',
  styleUrl: './navigation.css'
})
export class Navigation {
  isAuthenticated = false;

  showFiles = () => false;
  showTransactions = () => false;
  showUsers = () => false;
  showFileExport = () => false;
  exportLoading = false;
  getUserName = () => '';
}
