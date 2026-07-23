package com.buy01.product.services;

import java.util.ArrayList;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import com.buy01.product.dtos.CartItemRequest;
import com.buy01.product.dtos.CartProductResponse;
import com.buy01.product.dtos.CartResponse;
import com.buy01.product.exceptions.ProductNotFoundException;
import com.buy01.product.models.Cart;
import com.buy01.product.models.CartProductRef;
import com.buy01.product.models.Product;
import com.buy01.product.repositories.CartRepository;
import com.buy01.product.repositories.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository carts;
    private final ProductRepository products;

    public CartResponse getCart(String authenticatedClientId) {
        ObjectId clientId = asObjectId(authenticatedClientId);
        return carts.findByClientId(clientId).map(this::toResponse)
                .orElseGet(() -> new CartResponse(null, clientId.toHexString(), new ArrayList<>()));
    }

    public CartResponse addItem(String authenticatedClientId, CartItemRequest request) {
        ObjectId clientId = asObjectId(authenticatedClientId);
        Product product = products.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found."));
        if (clientId.toHexString().equals(product.getSellerId())) {
            throw new IllegalArgumentException("Sellers cannot purchase their own products.");
        }

        Cart cart = carts.findByClientId(clientId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setClientId(clientId);
            return newCart;
        });
        if (cart.getProducts() == null) cart.setProducts(new ArrayList<>());

        CartProductRef item = cart.getProducts().stream()
                .filter(existing -> existing.getProductId().equals(product.getId()))
                .findFirst()
                .orElseGet(() -> {
                    CartProductRef added = new CartProductRef();
                    cart.getProducts().add(added);
                    return added;
                });

        long requestedQuantity = item.getQuantity() + request.getQuantity();
        if (requestedQuantity > product.getQuantity()) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock.");
        }
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setPrice(product.getPrice());
        item.setQuantity(requestedQuantity);
        item.setImageUrl(product.getImageUrls() == null || product.getImageUrls().isEmpty() ? null : product.getImageUrls().get(0));

        return toResponse(carts.save(cart));
    }

    /** Removes one product when productId is supplied; otherwise clears the active cart. */
    public void remove(String authenticatedClientId, String productId) {
        ObjectId clientId = asObjectId(authenticatedClientId);
        Cart cart = carts.findByClientId(clientId).orElse(null);
        if (cart == null) return;

        if (productId == null || productId.isBlank()) {
            carts.delete(cart);
            return;
        }
        cart.getProducts().removeIf(product -> productId.equals(product.getProductId()));
        if (cart.getProducts().isEmpty()) carts.delete(cart);
        else carts.save(cart);
    }

    private CartResponse toResponse(Cart cart) {
        return new CartResponse(
                cart.getId() == null ? null : cart.getId().toHexString(),
                cart.getClientId().toHexString(),
                cart.getProducts().stream()
                        .map(product -> new CartProductResponse(product.getProductId(), product.getProductName(), product.getPrice(), product.getQuantity(), product.getImageUrl()))
                        .toList());
    }

    private ObjectId asObjectId(String value) {
        if (!ObjectId.isValid(value)) throw new IllegalArgumentException("Authenticated client ID is invalid.");
        return new ObjectId(value);
    }
}
