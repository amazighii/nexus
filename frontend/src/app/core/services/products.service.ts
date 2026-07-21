import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { ApiService } from './api.service';
import type { ProductAnalytics, ProductRequest, ProductResponse } from '../models/product.models';

@Injectable({ providedIn: 'root' })
export class ProductsService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  list(filters: { minPrice?: number | null; maxPrice?: number | null } = {}): Promise<ProductResponse[]> {
    let params = new HttpParams();
    if (filters.minPrice != null) params = params.set('min-price', filters.minPrice);
    if (filters.maxPrice != null) params = params.set('max-price', filters.maxPrice);
    return firstValueFrom(this.http.get<ProductResponse[]>(this.api.url('/api/products'), { params }));
  }

  searchByName(query: string): Promise<ProductResponse[]> {
    return firstValueFrom(
      this.http.get<ProductResponse[]>(this.api.url('/api/products/name'), {
        params: new HttpParams().set('q', query),
      }),
    );
  }

  clientMostBuyingProducts(limit = 5): Promise<ProductAnalytics[]> {
    return this.analytics('/api/products/client/most-buying-products', limit);
  }

  clientBestProducts(limit = 5): Promise<ProductAnalytics[]> {
    return this.analytics('/api/products/client/best-products', limit);
  }

  sellerBestSellingProducts(limit = 5): Promise<ProductAnalytics[]> {
    return this.analytics('/api/products/seller/best-selling-products', limit);
  }

  get(id: string): Promise<ProductResponse> {
    return firstValueFrom(this.http.get<ProductResponse>(this.api.url(`/api/products/${id}`)));
  }

  create(body: ProductRequest): Promise<ProductResponse> {
    console.log('Creating product with body', body);
    return firstValueFrom(this.http.post<ProductResponse>(this.api.url('/api/products'), body));
  }

  update(id: string, body: ProductRequest): Promise<ProductResponse> {
    return firstValueFrom(this.http.put<ProductResponse>(this.api.url(`/api/products/${id}`), body));
  }

  delete(id: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(this.api.url(`/api/products/${id}`)));
  }

  private async analytics(path: string, limit: number): Promise<ProductAnalytics[]> {
    const rows = await firstValueFrom(
      this.http.get<Array<ProductAnalytics & { totalQuantity: number | string; totalSpent: number | string }>>(
        this.api.url(path),
        { params: new HttpParams().set('limit', limit) },
      ),
    );
    return rows.map((row) => ({
      ...row,
      totalQuantity: Number(row.totalQuantity),
      totalSpent: Number(row.totalSpent),
    }));
  }
}
