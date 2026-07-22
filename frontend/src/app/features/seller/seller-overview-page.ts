import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ProductsService } from '../../core/services/products.service';
import type { ProductResponse } from '../../core/models/product.models';
import type { DashboardAnalytics, OrderProductAnalytics } from '../../core/models/order.models';
import { OrderService } from '../../core/services/order.service';
import { SessionStore } from '../../core/state/session.store';
import { SpinnerComponent } from '../../shared/components/spinner/spinner.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import { extractApiErrorMessage } from '../../core/utils/http-error';

@Component({
  standalone: true,
  imports: [RouterLink, CurrencyPipe, SpinnerComponent, ErrorStateComponent],
  template: `
    <div class="surface page">
      <div class="top">
        <div>
          <h1>Overview</h1>
          <p class="muted">Quick snapshot of your seller account.</p>
        </div>
        <a class="btn" routerLink="/seller/products/new">New product</a>
      </div>

      @if (loading()) {
        <div class="center"><app-spinner /></div>
      } @else if (error()) {
        <app-error-state title="Could not load overview" [message]="error()" />
      } @else {
        <div class="cards">
          <div class="card">
            <div class="muted">Total products</div>
            <div class="num">{{ myProducts().length }}</div>
          </div>
          <div class="card">
            <div class="muted">Total images</div>
            <div class="num">{{ totalImages() }}</div>
          </div>
          <div class="card">
            <div class="muted">Total revenue</div>
            <div class="num">{{ dashboard()?.totalAmount ?? 0 | currency : 'USD' : 'symbol' : '1.2-2' }}</div>
          </div>
        </div>

        <div class="section">
          <h2>Revenue</h2>
          @if (dashboard()?.history?.length) {
            <div class="bars chart">
              @for (point of dashboard()?.history ?? []; track point.label) {
                <div class="bar-row">
                  <span class="bar-label">{{ point.label }}</span>
                  <div class="bar-track"><span class="bar-fill" [style.width.%]="barWidth(point.value, maxHistoryValue())"></span></div>
                  <strong>{{ point.value | currency : 'USD' : 'symbol' : '1.0-0' }}</strong>
                </div>
              }
            </div>
          } @else {
            <p class="muted">Revenue history appears after delivered orders.</p>
          }
        </div>

        <div class="section">
          <h2>Product sales</h2>
          @if (analyticsError()) {
            <p class="muted">{{ analyticsError() }}</p>
          } @else if (bestSellers().length === 0) {
            <p class="muted">Revenue appears here after delivered orders.</p>
          } @else {
            <div class="list">
              @for (row of bestSellers(); track row.productId) {
                <div class="item">
                  <div class="analytics-product">
                    @if (row.imageUrl) {
                      <img class="thumb" [src]="row.imageUrl" [alt]="row.productName" />
                    } @else {
                      <div class="thumb thumb--empty">No image</div>
                    }
                    <div>
                      <div class="name">{{ row.productName }}</div>
                      <span class="badge">{{ row.totalQuantity }} sold</span>
                    </div>
                  </div>
                  <strong>{{ row.totalSpent | currency : 'USD' : 'symbol' : '1.2-2' }}</strong>
                </div>
                <div class="bar-track product-track"><span class="bar-fill" [style.width.%]="barWidth(row.totalQuantity, maxProductQuantity())"></span></div>
              }
            </div>
          }
        </div>

        <div class="section">
          <h2>Recent products</h2>
          @if (myProducts().length === 0) {
            <p class="muted">Create your first product to get started.</p>
          } @else {
            <div class="list">
              @for (p of recent(); track p.id) {
                <a class="item" [routerLink]="['/seller/products', p.id, 'edit']">
                  <div class="name">{{ p.name }}</div>
                  <div class="muted small">Qty {{ p.quantity }}</div>
                </a>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .page {
        padding: 18px;
      }
      .top {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        align-items: flex-start;
      }
      h1 {
        margin: 0;
        letter-spacing: -0.03em;
      }
      h2 {
        margin: 0;
        font-size: 16px;
        letter-spacing: -0.02em;
      }
      .btn {
        display: inline-flex;
        align-items: center;
        height: 40px;
        padding: 0 14px;
        border-radius: 12px;
        background: var(--primary);
        color: white;
        border: 1px solid rgba(15, 118, 110, 0.25);
        font-weight: 650;
      }
      .btn:hover {
        background: var(--primary-2);
      }
      .center {
        padding: 26px 0;
        display: grid;
        place-items: center;
      }
      .cards {
        margin-top: 14px;
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;
      }
      .card {
        padding: 14px;
        border: 1px solid var(--border);
        border-radius: 14px;
        background: var(--surface-2);
      }
      .cards {
        grid-template-columns: repeat(3, 1fr);
      }
      .num {
        margin-top: 8px;
        font-size: 24px;
        font-weight: 750;
        letter-spacing: -0.03em;
      }
      .section {
        margin-top: 18px;
        padding-top: 18px;
        border-top: 1px solid var(--border);
      }
      .list {
        margin-top: 10px;
        display: grid;
        gap: 8px;
      }
      .item {
        padding: 12px;
        border-radius: 14px;
        border: 1px solid var(--border);
        background: var(--surface);
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
      }
      .chart {
        margin-top: 10px;
      }
      .bar-row {
        display: grid;
        grid-template-columns: minmax(88px, 120px) 1fr minmax(74px, auto);
        gap: 10px;
        align-items: center;
        padding: 8px 0;
      }
      .bar-label {
        color: var(--muted);
        font-size: 12px;
      }
      .bar-track {
        height: 10px;
        border-radius: 999px;
        background: rgba(15, 118, 110, 0.1);
        overflow: hidden;
      }
      .bar-fill {
        display: block;
        height: 100%;
        min-width: 5px;
        border-radius: inherit;
        background: var(--primary);
      }
      .product-track {
        margin: -2px 12px 8px 66px;
      }
      .analytics-product {
        display: flex;
        align-items: center;
        gap: 10px;
        min-width: 0;
      }
      .thumb {
        width: 44px;
        height: 44px;
        border-radius: 8px;
        object-fit: cover;
        border: 1px solid var(--border);
        background: var(--surface-2);
        flex: 0 0 auto;
      }
      .thumb--empty {
        display: grid;
        place-items: center;
        color: var(--muted);
        font-size: 10px;
        text-align: center;
      }
      .badge {
        display: inline-flex;
        margin-top: 4px;
        padding: 3px 7px;
        border-radius: 999px;
        background: rgba(15, 118, 110, 0.1);
        color: var(--primary);
        font-size: 12px;
        font-weight: 650;
      }
      .name {
        font-weight: 650;
        min-width: 0;
        overflow-wrap: anywhere;
        word-break: break-word;
      }
      .small {
        font-size: 12px;
        flex: 0 0 auto;
      }
      @media (max-width: 700px) {
        .cards {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class SellerOverviewPage {
  private readonly productsService = inject(ProductsService);
  private readonly orderService = inject(OrderService);
  private readonly session = inject(SessionStore);

  readonly products = signal<ProductResponse[]>([]);
  readonly dashboard = signal<DashboardAnalytics | null>(null);
  readonly bestSellers = signal<OrderProductAnalytics[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly analyticsError = signal<string | null>(null);

  readonly myProducts = computed(() => this.products().filter((p) => p.sellerId === this.session.userId()));
  readonly totalImages = computed(() => this.myProducts().reduce((acc, p) => acc + (p.imageUrls?.length ?? 0), 0));
  readonly recent = computed(() => this.myProducts().slice(0, 5));
  readonly maxHistoryValue = computed(() => Math.max(...(this.dashboard()?.history.map((point) => point.value) ?? [0]), 0));
  readonly maxProductQuantity = computed(() => Math.max(...this.bestSellers().map((row) => row.totalQuantity), 0));

  constructor() {
    void this.load();
  }

  async load() {
    try {
      this.loading.set(true);
      this.error.set(null);
      const [products, dashboard] = await Promise.all([
        this.productsService.list(),
        this.orderService.sellerDashboard(5).catch((e) => {
          this.analyticsError.set(extractApiErrorMessage(e, 'Could not load seller analytics.'));
          return null;
        }),
      ]);
      this.products.set(products);
      this.dashboard.set(dashboard);
      this.bestSellers.set(dashboard?.topProducts ?? []);
    } catch (e) {
      this.error.set(extractApiErrorMessage(e, 'Could not fetch products.'));
    } finally {
      this.loading.set(false);
    }
  }

  barWidth(value: number, max: number): number {
    return max > 0 ? Math.max(6, Math.round((value / max) * 100)) : 0;
  }
}
