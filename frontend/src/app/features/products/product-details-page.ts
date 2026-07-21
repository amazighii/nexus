import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';

import { ProductsService } from '../../core/services/products.service';
import type { ProductResponse } from '../../core/models/product.models';
import { UserService } from '../../core/services/user.service';
import type { PublicUserProfileResponse } from '../../core/models/user.models';
import { SpinnerComponent } from '../../shared/components/spinner/spinner.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import { extractApiErrorMessage } from '../../core/utils/http-error';
import { CartService } from '../../core/services/cart.service';
import { SessionStore } from '../../core/state/session.store';
import { ToastService } from '../../core/services/toast.service';

@Component({
  standalone: true,
  imports: [RouterLink, CurrencyPipe, SpinnerComponent, ErrorStateComponent],
  template: `
    <div class="container">
      <a class="back muted" [routerLink]="backLink()">← Back</a>

      @if (loading()) {
        <div class="center"><app-spinner /></div>
      } @else if (error()) {
        <app-error-state title="Could not load product" [message]="error()" />
      } @else if (!product()) {
        <app-error-state title="Not found" message="This product no longer exists." />
      } @else {
        <div class="layout">
          <section class="surface media">
            <div class="media__main">
              @if (activeImage()) {
                <img class="media__image" [src]="activeImage()!" [alt]="product()!.name" />
              } @else {
                <div class="placeholder">No image</div>
              }
            </div>
            @if (product()!.imageUrls.length > 1) {
              <div class="thumbs">
                @for (img of product()!.imageUrls; track img) {
                  <button type="button" class="thumb" (click)="activeImage.set(img)" [class.is-active]="img === activeImage()">
                    <img [src]="img" alt="" />
                  </button>
                }
              </div>
            }
          </section>

          <section class="surface details">
            <h1 class="title">{{ product()!.name }}</h1>
            <div class="price">{{ asNumber(product()!.price) | currency : 'USD' : 'symbol' : '1.2-2' }}</div>
            <p class="muted">{{ product()!.description }}</p>
            @if (ownsProduct()) {
              <div class="alert alert-warning mt-3 mb-0" role="alert">
                You cannot buy products from your own seller account.
              </div>
            }

            <div class="d-flex flex-wrap gap-2 mt-3">
              <input class="form-control" style="width: 6rem" type="number" min="1" [max]="product()!.quantity" [value]="purchaseQuantity()" (input)="setPurchaseQuantity($event)" aria-label="Quantity" />
              <button class="btn btn-primary" type="button" [disabled]="ownsProduct()" (click)="buyNow()">Buy now</button>
              <button class="btn btn-outline-primary" type="button" [disabled]="ownsProduct()" (click)="addToCart()">Add to cart</button>
            </div>

            <div class="meta">
              <div><span class="muted">Quantity</span><div class="meta__val">{{ product()!.quantity }}</div></div>
              <div>
                <span class="muted">Seller</span>
                <div class="seller">
                  <a class="seller__avatarLink" [routerLink]="['/users', product()!.sellerId]" aria-label="View seller profile">
                    @if (seller()?.avatarUrl) {
                      <img class="seller__avatar" [src]="seller()!.avatarUrl!" alt="" />
                    } @else {
                      <span class="seller__initial">{{ sellerInitial() }}</span>
                    }
                  </a>
                  <div>
                    <div class="meta__val">{{ sellerName() }}</div>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .back {
        display: inline-flex;
        margin-bottom: 14px;
      }
      .center {
        display: grid;
        place-items: center;
        padding: 34px 0;
      }
      .layout {
        display: grid;
        grid-template-columns: 1.15fr 0.85fr;
        gap: 16px;
      }
      .media {
        overflow: hidden;
        min-width: 0;
      }
      .media__main {
        aspect-ratio: 4 / 3;
        background: var(--surface-2);
        border-bottom: 1px solid var(--border);
        display: grid;
        place-items: center;
        overflow: hidden;
        padding: 12px;
      }
      .placeholder {
        color: var(--muted);
        font-size: 13px;
      }
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
      }
      .media__image {
        max-width: 100%;
        max-height: min(72vh, 640px);
        object-fit: contain;
      }
      .thumbs {
        display: flex;
        gap: 8px;
        padding: 10px;
        overflow-x: auto;
      }
      .thumb {
        width: 70px;
        height: 70px;
        border-radius: 12px;
        overflow: hidden;
        border: 1px solid var(--border);
        padding: 0;
        background: transparent;
        cursor: pointer;
        flex: 0 0 auto;
      }
      .thumb.is-active {
        border-color: rgba(15, 118, 110, 0.45);
      }
      .details {
        padding: 18px;
        min-width: 0;
      }
      .title {
        margin: 0;
        letter-spacing: -0.03em;
        overflow-wrap: anywhere;
        word-break: break-word;
      }
      .price {
        margin-top: 10px;
        font-weight: 700;
        font-size: 18px;
        white-space: nowrap;
      }
      .meta {
        margin-top: 16px;
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 12px;
        padding-top: 16px;
        border-top: 1px solid var(--border);
      }
      .meta__val {
        margin-top: 4px;
        font-weight: 600;
        overflow-wrap: anywhere;
        word-break: break-word;
      }
      .seller {
        margin-top: 6px;
        display: flex;
        align-items: center;
        gap: 10px;
      }
      .seller__avatarLink {
        width: 36px;
        height: 36px;
        border-radius: 999px;
        border: 1px solid var(--border);
        background: var(--surface-2);
        display: grid;
        place-items: center;
        overflow: hidden;
        flex: 0 0 auto;
      }
      .seller__avatarLink:hover {
        border-color: rgba(15, 118, 110, 0.45);
      }
      .seller__avatar {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      .seller__initial {
        color: var(--primary);
        font-size: 14px;
        font-weight: 750;
      }
      .mono {
        font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
          monospace;
        font-size: 12px;
      }
      @media (max-width: 980px) {
        .layout {
          grid-template-columns: 1fr;
        }
      }
      @media (max-width: 640px) {
        .meta {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class ProductDetailsPage {
  private readonly route = inject(ActivatedRoute);
  private readonly productsService = inject(ProductsService);
  private readonly users = inject(UserService);
  private readonly router = inject(Router);
  private readonly carts = inject(CartService);
  private readonly session = inject(SessionStore);
  private readonly toast = inject(ToastService);

  readonly product = signal<ProductResponse | null>(null);
  readonly seller = signal<PublicUserProfileResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly activeImage = signal<string | null>(null);
  readonly purchaseQuantity = signal(1);

  readonly id = computed(() => this.route.snapshot.paramMap.get('id') ?? '');
  readonly backLink = computed(() => this.route.snapshot.queryParamMap.get('from') === 'seller' ? '/seller/products' : '/products');
  readonly sellerName = computed(() => {
    const seller = this.seller();
    if (!seller) return this.product()?.sellerId ?? 'Seller';
    return `${seller.firstName} ${seller.lastName}`.trim() || 'Seller';
  });
  readonly sellerInitial = computed(() => this.sellerName().trim().charAt(0).toUpperCase() || 'S');
  readonly ownsProduct = computed(() => this.session.isSeller() && this.product()?.sellerId === this.session.userId());

  constructor() {
    void this.load();
  }

  asNumber(value: string | number): number {
    return typeof value === 'number' ? value : Number(value);
  }

  setPurchaseQuantity(event: Event) {
    const value = Number((event.target as HTMLInputElement).value);
    const available = Number(this.product()?.quantity ?? 1);
    this.purchaseQuantity.set(Math.min(Math.max(1, Number.isFinite(value) ? value : 1), available));
  }

  async buyNow() {
    const product = this.product();
    if (!product) return;
    if (!this.session.isAuthed()) {
      await this.router.navigateByUrl('/login');
      return;
    }
    if (this.ownsProduct()) {
      this.toast.show('info', 'Unavailable', 'You cannot buy your own product.');
      return;
    }
    await this.router.navigate(['/orders/new'], { queryParams: { productId: product.id, quantity: this.purchaseQuantity() } });
  }

  addToCart() {
    const product = this.product();
    if (!product) return;
    if (!this.session.isAuthed()) {
      void this.router.navigateByUrl('/login');
      return;
    }
    if (this.ownsProduct()) {
      this.toast.show('info', 'Unavailable', 'You cannot add your own product to the cart.');
      return;
    }
    this.carts.addItem({ productId: product.id, quantity: this.purchaseQuantity() }).subscribe({
      next: () => this.toast.show('success', 'Added to cart'),
      error: (error: Error) => this.toast.show('error', 'Could not add to cart', error.message),
    });
  }

  async load() {
    try {
      this.loading.set(true);
      this.error.set(null);
      const product = await this.productsService.get(this.id());
      this.product.set(product);
      this.activeImage.set(product.imageUrls?.[0] ?? null);
      try {
        this.seller.set(await this.users.getPublicProfile(product.sellerId));
      } catch {
        this.seller.set(null);
      }
    } catch (e: any) {
      if (e?.status === 404) {
        this.product.set(null);
        this.error.set(null);
      } else {
        this.error.set(extractApiErrorMessage(e, 'This product could not be loaded.'));
      }
    } finally {
      this.loading.set(false);
    }
  }
}
