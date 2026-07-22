import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import type { DashboardAnalytics, OrderProductAnalytics } from '../../core/models/order.models';
import { OrderService } from '../../core/services/order.service';
import { extractApiErrorMessage } from '../../core/utils/http-error';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import { SpinnerComponent } from '../../shared/components/spinner/spinner.component';

@Component({
  standalone: true,
  imports: [CurrencyPipe, RouterLink, SpinnerComponent, ErrorStateComponent],
  template: `
    <main class="container">
      <section class="surface dashboard">
        <div class="top">
          <div>
            <h1>Dashboard</h1>
            <p class="muted">Your spending and purchase patterns from delivered orders.</p>
          </div>
          <a class="btn" routerLink="/products">Browse products</a>
        </div>

        @if (loading()) {
          <div class="center"><app-spinner /></div>
        } @else if (error()) {
          <app-error-state title="Could not load dashboard" [message]="error()" />
        } @else {
          <div class="cards">
            <div class="card">
              <span class="muted">Total spent</span>
              <strong>{{ totalSpent() | currency : 'USD' : 'symbol' : '1.2-2' }}</strong>
            </div>
            <div class="card">
              <span class="muted">Products purchased</span>
              <strong>{{ totalQuantity() }}</strong>
            </div>
            <div class="card">
              <span class="muted">Favorite product</span>
              <strong>{{ favoriteProductName() }}</strong>
            </div>
          </div>

          <section class="section">
            <h2>Spending history</h2>
            @if (dashboard()?.history?.length) {
              <div class="bars">
                @for (point of dashboard()?.history ?? []; track point.label) {
                  <div class="bar-row">
                    <span>{{ point.label }}</span>
                    <div class="bar-track"><i [style.width.%]="barWidth(point.value, maxHistoryValue())"></i></div>
                    <strong>{{ point.value | currency : 'USD' : 'symbol' : '1.0-0' }}</strong>
                  </div>
                }
              </div>
            } @else {
              <p class="muted">Spending history appears after delivered orders.</p>
            }
          </section>

          <section class="section">
            <h2>Most purchased products</h2>
            @if (topProducts().length) {
              <div class="products">
                @for (row of topProducts(); track row.productId) {
                  <article class="product">
                    @if (row.imageUrl) {
                      <img [src]="row.imageUrl" [alt]="row.productName" />
                    } @else {
                      <div class="image-empty">No image</div>
                    }
                    <div class="product-main">
                      <div class="product-head">
                        <strong>{{ row.productName }}</strong>
                        <span>{{ row.totalQuantity }} bought</span>
                      </div>
                      <div class="bar-track"><i [style.width.%]="barWidth(row.totalQuantity, maxProductQuantity())"></i></div>
                    </div>
                    <strong>{{ row.totalSpent | currency : 'USD' : 'symbol' : '1.2-2' }}</strong>
                  </article>
                }
              </div>
            } @else {
              <p class="muted">Your most purchased products appear here after delivered orders.</p>
            }
          </section>
        }
      </section>
    </main>
  `,
  styles: [
    `
      .dashboard {
        padding: 18px;
      }
      .top {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 14px;
      }
      h1 {
        margin: 0;
      }
      h2 {
        margin: 0 0 12px;
        font-size: 16px;
      }
      .btn {
        display: inline-flex;
        align-items: center;
        height: 40px;
        padding: 0 14px;
        border-radius: 8px;
        background: var(--primary);
        color: #fff;
        font-weight: 650;
      }
      .cards {
        margin-top: 16px;
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 12px;
      }
      .card {
        border: 1px solid var(--border);
        border-radius: 8px;
        background: var(--surface-2);
        padding: 14px;
        display: grid;
        gap: 8px;
      }
      .card strong {
        font-size: 22px;
        overflow-wrap: anywhere;
      }
      .section {
        margin-top: 20px;
        padding-top: 18px;
        border-top: 1px solid var(--border);
      }
      .center {
        display: grid;
        place-items: center;
        padding: 34px 0;
      }
      .bars,
      .products {
        display: grid;
        gap: 10px;
      }
      .bar-row {
        display: grid;
        grid-template-columns: minmax(88px, 120px) 1fr minmax(74px, auto);
        gap: 10px;
        align-items: center;
      }
      .bar-row span,
      .product-head span {
        color: var(--muted);
        font-size: 12px;
      }
      .bar-track {
        height: 10px;
        border-radius: 999px;
        background: rgba(15, 118, 110, 0.1);
        overflow: hidden;
      }
      .bar-track i {
        display: block;
        height: 100%;
        min-width: 5px;
        border-radius: inherit;
        background: var(--primary);
      }
      .product {
        display: grid;
        grid-template-columns: 52px 1fr auto;
        gap: 12px;
        align-items: center;
        padding: 12px;
        border: 1px solid var(--border);
        border-radius: 8px;
      }
      img,
      .image-empty {
        width: 52px;
        height: 52px;
        border-radius: 8px;
        border: 1px solid var(--border);
        object-fit: cover;
        background: var(--surface-2);
      }
      .image-empty {
        display: grid;
        place-items: center;
        color: var(--muted);
        font-size: 10px;
        text-align: center;
      }
      .product-main {
        display: grid;
        gap: 8px;
        min-width: 0;
      }
      .product-head {
        display: flex;
        justify-content: space-between;
        gap: 10px;
      }
      .product-head strong {
        overflow-wrap: anywhere;
      }
      @media (max-width: 760px) {
        .top,
        .product-head {
          display: grid;
        }
        .cards,
        .product {
          grid-template-columns: 1fr;
        }
        .bar-row {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class UserDashboardPage {
  private readonly orderService = inject(OrderService);

  readonly dashboard = signal<DashboardAnalytics | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly topProducts = computed<OrderProductAnalytics[]>(() => this.dashboard()?.topProducts ?? []);
  readonly totalSpent = computed(() => this.dashboard()?.totalAmount ?? 0);
  readonly totalQuantity = computed(() => this.topProducts().reduce((sum, row) => sum + row.totalQuantity, 0));
  readonly favoriteProduct = computed(() => this.topProducts()[0] ?? null);
  readonly favoriteProductName = computed(() => this.favoriteProduct()?.productName ?? 'None yet');
  readonly maxHistoryValue = computed(() => Math.max(...(this.dashboard()?.history.map((point) => point.value) ?? [0]), 0));
  readonly maxProductQuantity = computed(() => Math.max(...this.topProducts().map((row) => row.totalQuantity), 0));

  constructor() {
    void this.load();
  }

  async load() {
    try {
      this.loading.set(true);
      this.error.set(null);
      this.dashboard.set(await this.orderService.clientDashboard(5));
    } catch (e) {
      this.error.set(extractApiErrorMessage(e, 'Could not load dashboard.'));
    } finally {
      this.loading.set(false);
    }
  }

  barWidth(value: number, max: number): number {
    return max > 0 ? Math.max(6, Math.round((value / max) * 100)) : 0;
  }
}
