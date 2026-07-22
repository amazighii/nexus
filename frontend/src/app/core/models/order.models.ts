export const ORDER_STATUSES = ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'] as const;

export type OrderStatus = (typeof ORDER_STATUSES)[number];

export interface OrderItem {
  productId: string;
  productName: string;
  sellerId: string;
  price: number;
  quantity: number;
  description?: string;
  imageUrl?: string;
}

export interface Order {
  id: string;
  firstname?: string;
  lastname?: string;
  phoneNumber?: string;
  address?: string;
  clientId: string;
  status: OrderStatus;
  totalPrice: number;
  paymentMethod: string;
  products: OrderItem[];
  date: string;
}

export interface OrderSearchParams {
  status?: OrderStatus;
  /** ISO calendar date (`yyyy-MM-dd`) expected by the Orders API. */
  date?: string;
  view?: 'client' | 'seller';
}

export interface OrderProductRequest {
  productId: string;
  quantity: number;
}

export interface CreateOrderRequest {
  firstname: string;
  lastname: string;
  phoneNumber: string;
  address: string;
  paymentMethod: 'PAY_ON_DELIVERY';
  productIds: OrderProductRequest[];
}

export interface OrderMessage {
  message: string;
}

export interface OrderProductAnalytics {
  productId: string;
  productName: string;
  totalQuantity: number;
  totalSpent: number;
  imageUrl?: string;
}

export interface AnalyticsPoint {
  label: string;
  value: number;
}

export interface DashboardAnalytics {
  totalAmount: number;
  topProducts: OrderProductAnalytics[];
  history: AnalyticsPoint[];
}
