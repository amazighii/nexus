export interface ProductResponse {
  id: string;
  name: string;
  description: string;
  price: string | number;
  quantity: number | string;
  sellerId: string;
  imageUrls: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  quantity: number;
  imageUrls: string[];
}

export interface ProductAnalytics {
  productId: string;
  productName: string;
  totalQuantity: number;
  totalSpent: number;
  imageUrl?: string;
}
