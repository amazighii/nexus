import { Routes } from '@angular/router';

export const ORDER_ROUTES: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./order-list.component').then((m) => m.OrderListComponent) },
  { path: 'new', loadComponent: () => import('./order-create.component').then((m) => m.OrderCreateComponent) },
  { path: 'cart', loadComponent: () => import('./cart.component').then((m) => m.CartComponent) },
];
