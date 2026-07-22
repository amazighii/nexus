import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { ApiService } from './api.service';
import type { ProductRequest, ProductResponse, ProductSortOption } from '../models/product.models';

@Injectable({ providedIn: 'root' })
export class ProductsService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  list(filters: { minPrice?: number | null; maxPrice?: number | null; sort?: ProductSortOption } = {}): Promise<ProductResponse[]> {
    let params = new HttpParams();
    if (filters.minPrice != null) params = params.set('min-price', filters.minPrice);
    if (filters.maxPrice != null) params = params.set('max-price', filters.maxPrice);
    if (filters.sort) params = params.set('sort', filters.sort);
    return firstValueFrom(this.http.get<ProductResponse[]>(this.api.url('/api/products'), { params }));
  }

  searchByName(query: string): Promise<ProductResponse[]> {
    return firstValueFrom(
      this.http.get<ProductResponse[]>(this.api.url('/api/products/name'), {
        params: new HttpParams().set('q', query),
      }),
    );
  }

  get(id: string): Promise<ProductResponse> {
    return firstValueFrom(this.http.get<ProductResponse>(this.api.url(`/api/products/${id}`)));
  }

  create(body: ProductRequest): Promise<ProductResponse> {
    return firstValueFrom(this.http.post<ProductResponse>(this.api.url('/api/products'), body));
  }

  update(id: string, body: ProductRequest): Promise<ProductResponse> {
    return firstValueFrom(this.http.put<ProductResponse>(this.api.url(`/api/products/${id}`), body));
  }

  delete(id: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(this.api.url(`/api/products/${id}`)));
  }
}
