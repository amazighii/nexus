import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, UntypedFormBuilder } from '@angular/forms';
import { BehaviorSubject, catchError, combineLatest, distinctUntilChanged, finalize, map, of, startWith, switchMap } from 'rxjs';

import { CreateOrderRequest, ORDER_STATUSES, type Order, type OrderStatus } from '../../core/models/order.models';
import { OrderService } from '../../core/services/order.service';
import { SessionStore } from '../../core/state/session.store';
import { ToastService } from '../../core/services/toast.service';
import { ProductResponse } from '../../core/models/product.models';
import { CartService } from '../../core/services/cart.service';
import { Router } from '@angular/router';

type OrderView = 'client' | 'seller';
type OrderState = { loading: boolean; orders: Order[]; error: string | null };

@Component({
  standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule],
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
  readonly selectedOrder = signal<Order | null>(null);

  readonly filters = this.fb.nonNullable.group({
    search: [''],
    status: [''],
    date: [''],
  });

  private readonly reload$ = new BehaviorSubject<void>(undefined);
  private readonly view$ = toObservable(this.view);
  private readonly filtersValue = toSignal(this.filters.valueChanges.pipe(startWith(this.filters.getRawValue())), {
    initialValue: this.filters.getRawValue(),
  });

  private readonly serverFilters$ = this.filters.valueChanges.pipe(
    startWith(this.filters.getRawValue()),
    map((filters) => ({
      status: filters.status as OrderStatus | '',
      date: filters.date ?? '',
    })),
    distinctUntilChanged((a, b) => a.status === b.status && a.date === b.date),
  );

  private readonly stateSource$ = combineLatest([this.reload$, this.view$, this.serverFilters$]).pipe(
    switchMap(([, currentView, filters]) => {
      const hasServerFilters = Boolean(filters.status || filters.date);
      const request$ = hasServerFilters
        ? this.orders.searchOrders({ status: filters.status || undefined, date: filters.date || undefined, view: currentView })
        : currentView === 'seller'
          ? this.orders.getSellerOrders()
          : this.orders.getClientOrders();

      return request$.pipe(
        map((orders): OrderState => ({ loading: false, orders, error: null })),
        startWith({ loading: true, orders: [], error: null } as OrderState),
        catchError((error: Error) =>
          of({ loading: false, orders: [], error: error.message })
        )
      );
    })
  );

  readonly state = toSignal(this.stateSource$, {
    initialValue: { loading: true, orders: [], error: null } as OrderState,
  });
  readonly filteredOrders = computed(() => {
    const filters = this.filtersValue();
    const search = (filters.search ?? '').trim().toLowerCase();

    return this.state().orders.filter((order) => {
      const haystack = [
        order.id,
        order.clientId,
        order.firstname,
        order.lastname,
        order.phoneNumber,
        order.address,
        ...order.products.map((item) => item.productName),
      ].join(' ').toLowerCase();
      return !search || haystack.includes(search);
    });
  });

  changeView(newView: OrderView) {
    if (newView === 'seller' && !this.canViewSellerOrders()) return;
    this.view.set(newView);
  }

  applyFilters() {
    this.reload();
  }

  clearFilters() {
    this.filters.reset({ search: '', status: '', date: '' });
    this.reload();
  }

  canCancel(order: Order): boolean {
    return this.view() === 'client' && order.status === 'PENDING';
  }

  openDetails(order: Order) {
    if (this.view() === 'seller') this.selectedOrder.set(order);
  }

  clientName(order: Order): string {
    return `${order.firstname ?? ''} ${order.lastname ?? ''}`.trim() || 'Client';
  }

  cancel(order: Order) {
    if (this.actionInProgress()) return;
    this.actionInProgress.set(order.id);
    this.orders
      .cancelOrder(order.id)
      .pipe(finalize(() => this.actionInProgress.set(null)))
      .subscribe({
        next: (response) => {
          this.toast.show('success', 'Order cancelled', response.message);
          this.reload();
        },
        error: (error: Error) =>
          this.toast.show('error', 'Could not cancel order', error.message),
      });
  }

  confirmRemove() {
    const order = this.pendingRemoval();
    if (!order || this.actionInProgress()) return;

    this.actionInProgress.set(order.id);
    this.orders
      .removeOrder(order.id)
      .pipe(finalize(() => this.actionInProgress.set(null)))
      .subscribe({
        next: (response) => {
          this.pendingRemoval.set(null);
          this.toast.show('success', 'Order removed', response.message);
          this.reload();
        },
        error: (error: Error) =>
          this.toast.show('error', 'Could not remove order', error.message),
      });
  }

  badgeClass(status: OrderStatus): string {
    const classes: Record<OrderStatus, string> = {
      PENDING: 'text-bg-warning',
      PROCESSING: 'text-bg-info',
      SHIPPED: 'text-bg-primary',
      DELIVERED: 'text-bg-success',
      CANCELLED: 'text-bg-secondary',
    };
    return classes[status] || 'text-bg-secondary';
  }

  private reload() {
    this.reload$.next();
  }

  public reOrder(order: Order) {
    if (order.address == undefined || order.firstname == undefined || order.lastname == undefined
      || order.phoneNumber == undefined) {
      this.toast.show('error', 'Could not place order', 'Order fields cannot be undefined');
      return;
    }

    const createOrder: CreateOrderRequest = {
      firstname: order.firstname,
      lastname: order.lastname,
      phoneNumber: order.phoneNumber,
      address: order.address,
      paymentMethod: "PAY_ON_DELIVERY",
      productIds: order.products.map(product => ({
        productId: product.productId,
        quantity: product.quantity
      }))
    };

    this.orders.createOrder(createOrder).subscribe({
      next: (response) => {
        this.toast.show('success', 'Order placed', response.message);
      },
      error: (error: Error) => {
        this.toast.show('error', 'Could not place order', error.message);
      }
    })

  }

}
