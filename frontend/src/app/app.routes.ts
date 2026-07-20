import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'products' },
  {
    path: 'products',
    loadChildren: () => import('./features/products/routes').then((m) => m.PRODUCT_ROUTES),
  },
  {
    path: 'login',
    canMatch: [guestGuard],
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    canMatch: [guestGuard],
    loadComponent: () => import('./features/auth/register-page').then((m) => m.RegisterPage),
  },
  {
    path: 'profile',
    canMatch: [authGuard],
    loadComponent: () => import('./features/profile/profile-page').then((m) => m.ProfilePage),
  },
  {
    path: 'orders',
    canMatch: [authGuard],
    loadChildren: () => import('./features/orders/routes').then((m) => m.ORDER_ROUTES),
  },
  {
    path: 'cart',
    canMatch: [authGuard],
    loadComponent: () => import('./features/orders/cart.component').then((m) => m.CartComponent),
  },
  {
    path: 'users/:id',
    loadComponent: () => import('./features/profile/public-profile-page').then((m) => m.PublicProfilePage),
  },
  {
    path: 'seller',
    canMatch: [
      authGuard,
      roleGuard(['SELLER']),
    ],
    loadChildren: () => import('./features/seller/routes').then((m) => m.SELLER_ROUTES),
  },
  {
    path: '**',
    loadComponent: () => import('./layout/not-found/not-found-page').then((m) => m.NotFoundPage),
  },
];
