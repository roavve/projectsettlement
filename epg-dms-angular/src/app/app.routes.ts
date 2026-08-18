import { Routes } from '@angular/router';
import { Login } from './views/login/login';
import { Transactions } from './views/transactions/transactions';

export const routes: Routes = [
  { path: '', component: Transactions },
  { path: 'login', component: Login }
];
