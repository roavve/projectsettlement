import { Routes } from '@angular/router';
import { Login } from './views/login/login';
import { Transactions } from './views/transactions/transactions';
import { Files } from './views/files/files';
import { Users } from './views/users/users';
import { NotFound } from './views/not-found/not-found';
import { Unauthorized } from './views/unauthorized/unauthorized';

export const routes: Routes = [
  { path: '', component: Transactions },
  { path: 'files', component: Files },
  { path: 'users', component: Users },
  { path: 'login', component: Login },
  { path: 'unauthorized', component: Unauthorized },
  { path: '**', component: NotFound }
];
