import { Component, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import type { CartItem } from '../../core/models/cart.models';
import type { CreateOrderRequest } from '../../core/models/order.models';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './order-create.component.html',
})
export class OrderCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly orders = inject(OrderService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly carts = inject(CartService);
  private readonly toast = inject(ToastService);

  readonly saving = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly checkoutItems = signal<CartItem[]>([]);
  readonly checkoutSource = signal<'manual' | 'buy-now' | 'cart'>('manual');
  readonly form = this.fb.nonNullable.group({
    firstname: ['', [Validators.required, Validators.maxLength(100)]],
    lastname: ['', [Validators.required, Validators.maxLength(100)]],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^[+0-9 ()-]{7,25}$/)]],
    address: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(300)]],
    paymentMethod: ['PAY_ON_DELIVERY' as const, Validators.required],
    productIds: this.fb.nonNullable.array([this.createItem()]),
  });

  get items(): FormArray {
    return this.form.controls.productIds;
  }

  constructor() {
    const productId = this.route.snapshot.queryParamMap.get('productId');
    if (productId) {
      const quantity = Math.max(1, Number(this.route.snapshot.queryParamMap.get('quantity') ?? 1));
      this.checkoutSource.set('buy-now');
      this.checkoutItems.set([{ productId, productName: 'Selected product', price: 0, quantity }]);
      this.replaceItems([{ productId, quantity }]);
    } else if (this.route.snapshot.queryParamMap.get('source') === 'cart') {
      this.checkoutSource.set('cart');
      this.carts.getCart().subscribe({
        next: (cart) => {
          if (!cart.products.length) {
            this.submitError.set('Your cart is empty. Add products before checking out.');
            return;
          }
          this.checkoutItems.set(cart.products);
          this.replaceItems(cart.products);
        },
        error: (error: Error) => this.submitError.set(error.message),
      });
    }
  }

  addItem() {
    this.items.push(this.createItem());
  }

  removeItem(index: number) {
    if (this.items.length > 1) this.items.removeAt(index);
  }

  submit() {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.submitError.set(null);
    const request = this.form.getRawValue() as CreateOrderRequest;
    this.orders.createOrder(request).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: async (response) => {
        if (this.checkoutSource() === 'cart') this.carts.clear().subscribe();
        this.toast.show('success', 'Order placed', response.message);
        await this.router.navigateByUrl('/orders');
      },
      error: (error: Error) => {
        this.submitError.set(error.message);
        this.toast.show('error', 'Could not place order', error.message);
      },
    });
  }

  private createItem() {
    return this.fb.nonNullable.group({
      productId: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1), Validators.pattern(/^[0-9]+$/)]],
    });
  }

  private replaceItems(items: Array<{ productId: string; quantity: number }>) {
    this.form.setControl('productIds', this.fb.nonNullable.array(items.map((item) => this.fb.nonNullable.group({
      productId: [item.productId, Validators.required],
      quantity: [item.quantity, [Validators.required, Validators.min(1)]],
    }))));
  }
}
