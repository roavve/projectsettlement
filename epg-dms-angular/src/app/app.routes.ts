import { Routes } from '@angular/router';
import { Login } from './views/login/login';
import { Transactions } from './views/transactions/transactions';
import { Files } from './views/files/files';
import { Users } from './views/users/users';
import { NotFound } from './views/not-found/not-found';
import { Unauthorized } from './views/unauthorized/unauthorized';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: Transactions,
    canActivate: [authGuard(['ROLE_GUEST', 'ROLE_OPERATOR', '-*ROLE_MANAGER', 'ROLE_ADMIN'])]
  },
  {
    path: 'files',
    component: Files,
    canActivate: [authGuard(['ROLE_MANAGER', 'ROLE_ADMIN'])]
  },
  {
    path: 'users',
    component: Users,
    canActivate: [authGuard(['ROLE_ADMIN'])]
  },
  { path: 'login', component: Login },
  { path: 'unauthorized', component: Unauthorized },
  { path: '**', component: NotFound }
];
