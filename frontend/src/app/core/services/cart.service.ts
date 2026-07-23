import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';

import type { AddCartItemRequest, Cart } from '../models/cart.models';
import { extractApiErrorMessage } from '../utils/http-error';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  getCart(): Observable<Cart> {
    return this.http.get<Cart>(this.api.url('/api/products/cart')).pipe(catchError(this.handleError));
  }

  addItem(request: AddCartItemRequest): Observable<Cart> {
    return this.http.post<Cart>(this.api.url('/api/products/cart'), request).pipe(catchError(this.handleError));
  }

  removeItem(productId: string): Observable<void> {
    return this.http.delete<void>(this.api.url('/api/products/cart'), { params: new HttpParams().set('productId', productId) }).pipe(catchError(this.handleError));
  }

  clear(): Observable<void> {
    return this.http.delete<void>(this.api.url('/api/products/cart')).pipe(catchError(this.handleError));
  }

  private handleError(error: unknown): Observable<never> {
    return throwError(() => new Error(extractApiErrorMessage(error, 'Cart request failed.')));
  }
}
