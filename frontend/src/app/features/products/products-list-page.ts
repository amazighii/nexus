import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';

import { ProductsService } from '../../core/services/products.service';
import type { ProductResponse, ProductSortOption } from '../../core/models/product.models';
import { SpinnerComponent } from '../../shared/components/spinner/spinner.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { extractApiErrorMessage } from '../../core/utils/http-error';
import { CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';
import { SessionStore } from '../../core/state/session.store';

@Component({
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, CurrencyPipe, SpinnerComponent, ErrorStateComponent, EmptyStateComponent],
  template: `
    <div class="container">
      <div class="hero">
        <h1>Products</h1>
        <p class="muted">Browse all listings. Sellers manage products through the dashboard.</p>
      </div>

      <form class="filters surface" [formGroup]="filters" (ngSubmit)="applyFilters()">
        <div class="field field--wide">
          <label for="q">Search</label>
          <input id="q" class="form-control" type="search" placeholder="Product name" formControlName="q" />
        </div>
        <div class="field">
          <label for="minPrice">Min price</label>
          <input id="minPrice" class="form-control" type="number" min="0" step="1" formControlName="minPrice" />
        </div>
        <div class="field">
          <label for="maxPrice">Max price</label>
          <input id="maxPrice" class="form-control" type="number" min="0" step="1" formControlName="maxPrice" />
        </div>
        <div class="field">
          <label for="sort">Sort</label>
          <select id="sort" class="form-control" formControlName="sort">
            <option value="newest">Newest</option>
            <option value="oldest">Oldest</option>
            <option value="price_asc">Price: Low to High</option>
            <option value="price_desc">Price: High to Low</option>
            <option value="name_asc">Name: A to Z</option>
            <option value="name_desc">Name: Z to A</option>
          </select>
        </div>
        <div class="actions">
          <button class="btn btn-primary" type="submit">Filter</button>
          <button class="btn btn-outline-secondary" type="button" (click)="clearFilters()">Clear</button>
        </div>
      </form>

      @if (loading()) {
        <div class="center"><app-spinner /></div>
      } @else if (error()) {
        <app-error-state title="Could not load products" [message]="error()" />
      } @else if (products().length === 0) {
        <app-empty-state title="No products yet" message="When sellers add products, they’ll appear here." />
      } @else {
        <div class="grid">
          @for (p of products(); track p.id) {
            <article class="card surface">
              <a class="card__img" [routerLink]="['/products', p.id]">
                @if (p.imageUrls.length) {
                  <img [src]="p.imageUrls[0]" [alt]="p.name" loading="lazy" />
                } @else {
                  <div class="placeholder">No image</div>
                }
              </a>
              <div class="card__body">
                <a class="card__title" [routerLink]="['/products', p.id]">{{ p.name }}</a>
                <div class="card__desc muted">{{ p.description }}</div>
                <div class="card__meta">
                  <div class="price">{{ asNumber(p.price) | currency : 'USD' : 'symbol' : '1.2-2' }}</div>
                  <div class="muted">Qty {{ p.quantity }}</div>
                </div>
                @if (isOwnedByCurrentSeller(p)) {
                  <div class="alert alert-warning py-2 px-3 mt-3 mb-0 small">You own this product.</div>
                }
                <div class="card__actions">
                  <button class="btn btn-sm btn-primary" type="button" [disabled]="isOwnedByCurrentSeller(p)" (click)="buyNow(p)">Buy now</button>
                  <button class="btn btn-sm btn-outline-primary" type="button" [disabled]="isOwnedByCurrentSeller(p)" (click)="addToCart(p)">Add to cart</button>
                </div>
              </div>
            </article>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .hero {
        padding: 8px 0 18px;
      }
      .filters {
        margin-bottom: 18px;
        padding: 14px;
        display: grid;
        grid-template-columns: 2fr 1fr 1fr 1.2fr auto;
        gap: 12px;
        align-items: end;
      }
      label {
        display: block;
        margin-bottom: 6px;
        color: var(--muted);
        font-size: 13px;
      }
      .actions {
        display: flex;
        gap: 8px;
      }
      h1 {
        margin: 0;
        letter-spacing: -0.03em;
      }
      .center {
        display: grid;
        place-items: center;
        padding: 34px 0;
      }
      .grid {
        display: grid;
        grid-template-columns: repeat(12, 1fr);
        gap: 14px;
      }
      .card {
        grid-column: span 4;
        overflow: hidden;
        transition: transform 120ms ease, box-shadow 120ms ease;
        color: inherit;
      }
      .card:hover {
        transform: translateY(-2px);
        box-shadow: var(--shadow);
      }
      .card__img {
        aspect-ratio: 4 / 3;
        background: var(--surface-2);
        border-bottom: 1px solid var(--border);
        display: grid;
        place-items: center;
        overflow: hidden;
      }
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      .placeholder {
        color: var(--muted);
        font-size: 13px;
      }
      .card__body {
        padding: 14px;
        min-width: 0;
      }
      .card__title {
        display: inline-flex;
        color: inherit;
        font-weight: 650;
        letter-spacing: -0.01em;
        overflow-wrap: anywhere;
        word-break: break-word;
      }
      .card__desc {
        margin-top: 6px;
        font-size: 13px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
      .card__meta {
        margin-top: 12px;
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        gap: 12px;
        min-width: 0;
        flex-wrap: wrap;
      }
      .price {
        font-weight: 650;
        white-space: nowrap;
      }
      .card__actions {
        margin-top: 14px;
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
      }
      @media (max-width: 980px) {
        .filters {
          grid-template-columns: 1fr 1fr;
        }
        .field--wide,
        .actions {
          grid-column: 1 / -1;
        }
        .card {
          grid-column: span 6;
        }
      }
      @media (max-width: 640px) {
        .filters {
          grid-template-columns: 1fr;
        }
        .card {
          grid-column: span 12;
        }
      }
    `,
  ],
})
export class ProductsListPage {
  private readonly productsService = inject(ProductsService);
  private readonly carts = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly products = signal<ProductResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly filters = this.fb.nonNullable.group({
    q: [''],
    minPrice: [''],
    maxPrice: [''],
    sort: ['newest' as ProductSortOption],
  });
  readonly currentUserId = computed(() => this.session.userId());

  constructor() {
    void this.load();
  }

  asNumber(value: string | number): number {
    return typeof value === 'number' ? value : Number(value);
  }

  async load() {
    try {
      this.loading.set(true);
      this.error.set(null);
      this.products.set(await this.productsService.list({ sort: this.filters.controls.sort.value }));
    } catch (e) {
      this.error.set(extractApiErrorMessage(e, 'Could not load products.'));
    } finally {
      this.loading.set(false);
    }
  }

  async applyFilters() {
    const filters = this.filters.getRawValue();
    const minPrice = filters.minPrice === '' ? null : Number(filters.minPrice);
    const maxPrice = filters.maxPrice === '' ? null : Number(filters.maxPrice);
    if ((minPrice != null && minPrice < 0) || (maxPrice != null && maxPrice < 0)) {
      this.error.set('Price filters cannot be negative.');
      return;
    }
    if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
      this.error.set('Minimum price cannot be greater than maximum price.');
      return;
    }

    try {
      this.loading.set(true);
      this.error.set(null);
      const byPrice = await this.productsService.list({ minPrice, maxPrice, sort: filters.sort as ProductSortOption });
      const q = filters.q.trim();
      if (!q) {
        this.products.set(byPrice);
        return;
      }
      this.products.set(byPrice.filter((product) => product.name.toLowerCase().includes(q.toLowerCase())));
    } catch (e) {
      this.error.set(extractApiErrorMessage(e, 'Could not filter products.'));
    } finally {
      this.loading.set(false);
    }
  }

  clearFilters() {
    this.filters.reset({ q: '', minPrice: '', maxPrice: '', sort: 'newest' });
    void this.load();
  }

  isOwnedByCurrentSeller(product: ProductResponse): boolean {
    return this.session.isSeller() && product.sellerId === this.currentUserId();
  }

  async buyNow(product: ProductResponse) {
    if (!this.session.isAuthed()) {
      await this.router.navigateByUrl('/login');
      return;
    }
    if (this.isOwnedByCurrentSeller(product)) {
      this.toast.show('info', 'Unavailable', 'You cannot buy your own product.');
      return;
    }
    await this.router.navigate(['/orders/checkout'], { queryParams: { productId: product.id, quantity: 1 } });
  }

  addToCart(product: ProductResponse) {
    if (!this.session.isAuthed()) {
      void this.router.navigateByUrl('/login');
      return;
    }
    if (this.isOwnedByCurrentSeller(product)) {
      this.toast.show('info', 'Unavailable', 'You cannot add your own product to the cart.');
      return;
    }
    this.carts.addItem({ productId: product.id, quantity: 1 }).subscribe({
      next: () => this.toast.show('success', 'Added to cart'),
      error: (error: Error) => this.toast.show('error', 'Could not add to cart', error.message),
    });
  }
}
