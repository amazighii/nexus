import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, map, of, shareReplay, startWith, switchMap } from 'rxjs';

import type { Cart } from '../../core/models/cart.models';
import { CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  standalone: true,
  imports: [AsyncPipe, CurrencyPipe, RouterLink],
  template: `
    <main class="container py-4">
      <div class="d-flex justify-content-between align-items-start gap-3 mb-4"><div><h1 class="h3 mb-1">My cart</h1><p class="text-body-secondary mb-0">Review items before checkout.</p></div><a class="btn btn-outline-secondary" routerLink="/products">Continue shopping</a></div>
      @if (cart$ | async; as state) {
        @if (state.loading) { <div class="text-center py-5"><div class="spinner-border"><span class="visually-hidden">Loading cart</span></div></div> }
        @else if (state.error) { <div class="alert alert-danger">{{ state.error }}</div> }
        @else if (!state.cart.products.length) { <div class="card card-body text-center py-5 text-body-secondary">Your cart is empty.</div> }
        @else { <div class="card"><div class="table-responsive"><table class="table align-middle mb-0"><thead><tr><th>Product</th><th>Quantity</th><th>Price</th><th class="text-end">Action</th></tr></thead><tbody>@for (item of state.cart.products; track item.productId) { <tr><td><div class="d-flex align-items-center gap-3">@if (item.imageUrl) { <img class="img-thumbnail" [src]="item.imageUrl" [alt]="item.productName" width="56" height="56" style="object-fit: cover" /> }<span>{{ item.productName }}</span></div></td><td>{{ item.quantity }}</td><td>{{ item.price | currency }}</td><td class="text-end"><button class="btn btn-sm btn-outline-danger" type="button" (click)="remove(item.productId)">Remove</button></td></tr> }</tbody></table></div><div class="card-footer d-flex justify-content-between align-items-center"><strong>Total: {{ state.total | currency }}</strong><a class="btn btn-primary" routerLink="/orders/new" [queryParams]="{ source: 'cart' }">Checkout</a></div></div> }
      }
    </main>`,
})
export class CartComponent {
  private readonly carts = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly reload$ = new BehaviorSubject<void>(undefined);
  readonly cart$ = this.reload$.pipe(switchMap(() => this.carts.getCart())).pipe(
    map((cart) => ({ loading: false, cart, total: cart.products.reduce((sum, item) => sum + item.price * item.quantity, 0), error: null as string | null })),
    startWith({ loading: true, cart: { id: null, clientId: '', products: [] } as Cart, total: 0, error: null as string | null }),
    catchError((error: Error) => of({ loading: false, cart: { id: null, clientId: '', products: [] } as Cart, total: 0, error: error.message })),
    shareReplay(1),
  );

  remove(productId: string) {
    this.carts.removeItem(productId).subscribe({
      next: () => { this.toast.show('success', 'Item removed'); this.reload$.next(); },
      error: (error: Error) => this.toast.show('error', 'Could not remove item', error.message),
    });
  }
}
