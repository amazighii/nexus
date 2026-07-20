import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';

import type { CreateOrderRequest, Order, OrderMessage, OrderSearchParams, OrderStatus } from '../models/order.models';
import { extractApiErrorMessage } from '../utils/http-error';
import { ApiService } from './api.service';

interface OrdersResponse {
  clientOrders: ApiOrder[];
}

interface ApiOrder extends Omit<Order, 'id' | 'status' | 'totalPrice' | 'products'> {
  /** Older Orders API responses use an uppercase `Id`; accept both during rollout. */
  Id?: string;
  id?: string;
  status: string;
  totalPrice: number | string;
  products: Array<Omit<Order['products'][number], 'price' | 'quantity'> & { price: number | string; quantity: number | string }>;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  getClientOrders(): Observable<Order[]> {
    return this.getOrders('/api/orders/client');
  }

  getSellerOrders(): Observable<Order[]> {
    return this.getOrders('/api/orders/seller');
  }

  searchOrders(query: OrderSearchParams): Observable<Order[]> {
    let params = new HttpParams();
    if (query.status) params = params.set('status', query.status);
    if (query.date) params = params.set('date', query.date);

    return this.http
      .get<OrdersResponse>(this.api.url('/api/orders/search'), { params })
      .pipe(map((response) => response.clientOrders.map((order) => this.toOrder(order))), catchError(this.handleError));
  }

  createOrder(request: CreateOrderRequest): Observable<OrderMessage> {
    return this.http
      .post<OrderMessage>(this.api.url('/api/orders'), request)
      .pipe(catchError(this.handleError));
  }

  cancelOrder(id: string): Observable<OrderMessage> {
    return this.http
      .put<OrderMessage>(this.api.url(`/api/orders/cancel/${encodeURIComponent(id)}`), {})
      .pipe(catchError(this.handleError));
  }

  removeOrder(id: string): Observable<OrderMessage> {
    return this.http
      .put<OrderMessage>(this.api.url(`/api/orders/remove/${encodeURIComponent(id)}`), {})
      .pipe(catchError(this.handleError));
  }

  private getOrders(path: string): Observable<Order[]> {
    return this.http
      .get<OrdersResponse>(this.api.url(path))
      .pipe(map((response) => response.clientOrders.map((order) => this.toOrder(order))), catchError(this.handleError));
  }

  private toOrder(order: ApiOrder): Order {
    return {
      ...order,
      id: order.id ?? order.Id ?? '',
      status: order.status as OrderStatus,
      totalPrice: Number(order.totalPrice),
      products: order.products.map((item) => ({ ...item, price: Number(item.price), quantity: Number(item.quantity) })),
    };
  }

  private handleError(error: unknown): Observable<never> {
    return throwError(() => new Error(extractApiErrorMessage(error, 'Order request failed.')));
  }
}
