export interface CartItem {
  productId: string;
  productName: string;
  price: number;
  quantity: number;
  imageUrl?: string | null;
}

export interface Cart {
  id: string | null;
  clientId: string;
  products: CartItem[];
}

export interface AddCartItemRequest {
  productId: string;
  quantity: number;
}
