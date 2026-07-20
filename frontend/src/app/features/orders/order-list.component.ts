import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, finalize, map, of, shareReplay, startWith, switchMap } from 'rxjs';

import { ORDER_STATUSES, type Order, type OrderStatus } from '../../core/models/order.models';
import { OrderService } from '../../core/services/order.service';
import { SessionStore } from '../../core/state/session.store';
import { ToastService } from '../../core/services/toast.service';

type OrderView = 'client' | 'seller';
type OrderState = { loading: boolean; orders: Order[]; error: string | null };

@Component({
  standalone: true,
  imports: [AsyncPipe, CurrencyPipe, DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './order-list.component.html',
})
export class OrderListComponent {
  private readonly orders = inject(OrderService);
  private readonly session = inject(SessionStore);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly statuses = ORDER_STATUSES;
  readonly canViewSellerOrders = computed(() => this.session.isSeller());
  readonly view = signal<OrderView>('client');
  readonly actionInProgress = signal<string | null>(null);
  readonly pendingRemoval = signal<Order | null>(null);
  readonly filters = this.fb.nonNullable.group({
    status: [''],
    date: [''],
  });

  private readonly reload$ = new BehaviorSubject<void>(undefined);
  private readonly filters$ = this.filters.valueChanges.pipe(startWith(this.filters.getRawValue()));

  readonly state$ = combineLatest([this.reload$, this.filters$]).pipe(
    switchMap(([, filters]) => {
      const status = filters.status as OrderStatus | '';
      const hasFilters = Boolean(status || filters.date);
      const request$ = this.view() === 'client' && hasFilters
        ? this.orders.searchOrders({ status: status || undefined, date: filters.date || undefined })
        : this.view() === 'seller'
          ? this.orders.getSellerOrders()
          : this.orders.getClientOrders();

      return request$.pipe(
        map((orders): OrderState => ({ loading: false, orders, error: null })),
        startWith({ loading: true, orders: [], error: null } as OrderState),
        catchError((error: Error) => of({ loading: false, orders: [], error: error.message })),
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  changeView(view: OrderView) {
    if (view === 'seller' && !this.canViewSellerOrders()) return;
    this.view.set(view);
    this.reload();
  }

  applyFilters() {
    this.reload();
  }

  clearFilters() {
    this.filters.reset({ status: '', date: '' });
    this.reload();
  }

  canCancel(order: Order): boolean {
    return this.view() === 'client' && order.status === 'PENDING';
  }

  cancel(order: Order) {
    if (this.actionInProgress()) return;
    this.actionInProgress.set(order.id);
    this.orders.cancelOrder(order.id).pipe(finalize(() => this.actionInProgress.set(null))).subscribe({
      next: (response) => {
        this.toast.show('success', 'Order cancelled', response.message);
        this.reload();
      },
      error: (error: Error) => this.toast.show('error', 'Could not cancel order', error.message),
    });
  }

  confirmRemove() {
    const order = this.pendingRemoval();
    if (!order || this.actionInProgress()) return;

    this.actionInProgress.set(order.id);
    this.orders.removeOrder(order.id).pipe(finalize(() => this.actionInProgress.set(null))).subscribe({
      next: (response) => {
        this.pendingRemoval.set(null);
        this.toast.show('success', 'Order removed', response.message);
        this.reload();
      },
      error: (error: Error) => this.toast.show('error', 'Could not remove order', error.message),
    });
  }

  badgeClass(status: OrderStatus): string {
    return {
      PENDING: 'text-bg-warning', PROCESSING: 'text-bg-info', SHIPPED: 'text-bg-primary',
      DELIVERED: 'text-bg-success', CANCELLED: 'text-bg-secondary',
    }[status];
  }

  private reload() {
    this.reload$.next();
  }
}
