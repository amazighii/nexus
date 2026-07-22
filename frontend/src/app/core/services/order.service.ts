import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { catchError, firstValueFrom, map, Observable, throwError } from 'rxjs';

import type { CreateOrderRequest, DashboardAnalytics, Order, OrderMessage, OrderProductAnalytics, OrderSearchParams, OrderStatus } from '../models/order.models';
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

interface ApiProductAnalytics extends Omit<OrderProductAnalytics, 'totalQuantity' | 'totalSpent'> {
  totalQuantity: number | string;
  totalSpent: number | string;
}

interface ApiDashboardAnalytics {
  totalAmount: number | string;
  topProducts: ApiProductAnalytics[];
  history: Array<{ label: string; value: number | string }>;
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
    if (query.view) params = params.set('view', query.view);

    return this.http
      .get<OrdersResponse>(this.api.url('/api/orders/search'), { params })
      .pipe(map((response) => response.clientOrders.map((order) => this.toOrder(order))), catchError(this.handleError));
  }

  createOrder(request: CreateOrderRequest): Observable<OrderMessage> {
    return this.http
      .post<OrderMessage>(this.api.url('/api/orders'), request)
      .pipe(catchError(this.handleError));
  }

  clientMostPurchasedProducts(limit = 5): Promise<OrderProductAnalytics[]> {
    return this.analytics('/api/orders/client/most-buying-products', limit);
  }

  clientBestProducts(limit = 5): Promise<OrderProductAnalytics[]> {
    return this.analytics('/api/orders/client/best-products', limit);
  }

  sellerBestSellingProducts(limit = 5): Promise<OrderProductAnalytics[]> {
    return this.analytics('/api/orders/seller/best-selling-products', limit);
  }

  clientDashboard(limit = 5): Promise<DashboardAnalytics> {
    return this.dashboard('/api/orders/client/dashboard', limit);
  }

  sellerDashboard(limit = 5): Promise<DashboardAnalytics> {
    return this.dashboard('/api/orders/seller/dashboard', limit);
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

  private async analytics(path: string, limit: number): Promise<OrderProductAnalytics[]> {
    const rows = await firstValueFrom(
      this.http.get<ApiProductAnalytics[]>(this.api.url(path), {
        params: new HttpParams().set('limit', limit),
      }).pipe(catchError(this.handleError)),
    );
    return rows.map((row) => this.toProductAnalytics(row));
  }

  private async dashboard(path: string, limit: number): Promise<DashboardAnalytics> {
    const dashboard = await firstValueFrom(
      this.http.get<ApiDashboardAnalytics>(this.api.url(path), {
        params: new HttpParams().set('limit', limit),
      }).pipe(catchError(this.handleError)),
    );
    return {
      totalAmount: Number(dashboard.totalAmount),
      topProducts: dashboard.topProducts.map((row) => this.toProductAnalytics(row)),
      history: dashboard.history.map((point) => ({ label: point.label, value: Number(point.value) })),
    };
  }

  private toProductAnalytics(row: ApiProductAnalytics): OrderProductAnalytics {
    return {
      ...row,
      totalQuantity: Number(row.totalQuantity),
      totalSpent: Number(row.totalSpent),
    };
  }

  private handleError(error: unknown): Observable<never> {
    return throwError(() => new Error(extractApiErrorMessage(error, 'Order request failed.')));
  }
}
