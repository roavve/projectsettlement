import { Routes } from '@angular/router';
import { Login } from './views/login/login';
import { Transactions } from './views/transactions/transactions';
import { Files } from './views/files/files';

export const routes: Routes = [
  { path: '', component: Transactions },
  { path: 'files', component: Files },
  { path: 'login', component: Login }
];
