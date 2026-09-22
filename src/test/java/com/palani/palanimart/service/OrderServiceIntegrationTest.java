package com.palani.palanimart.service;

import com.palani.palanimart.dao.BaseDAOTest;
import com.palani.palanimart.dao.CartDAO;
import com.palani.palanimart.dao.OrderDAO;
import com.palani.palanimart.dao.ProductDAO;
import com.palani.palanimart.dao.UserDAO;
import com.palani.palanimart.dao.impl.CartDAOImpl;
import com.palani.palanimart.dao.impl.OrderDAOImpl;
import com.palani.palanimart.dao.impl.ProductDAOImpl;
import com.palani.palanimart.dao.impl.UserDAOImpl;
import com.palani.palanimart.exception.AppException;
import com.palani.palanimart.exception.AuthorizationException;
import com.palani.palanimart.exception.DatabaseException;
import com.palani.palanimart.exception.InsufficientStockException;
import com.palani.palanimart.exception.OrderException;
import com.palani.palanimart.exception.ValidationException;
import com.palani.palanimart.model.Order;
import com.palani.palanimart.model.OrderStatus;
import com.palani.palanimart.model.Product;
import com.palani.palanimart.model.Role;
import com.palani.palanimart.model.User;
import com.palani.palanimart.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceIntegrationTest extends BaseDAOTest {

    private OrderDAO orderDAO;
    private CartDAO cartDAO;
    private ProductDAO productDAO;
    private UserDAO userDAO;

    private OrderService orderService;

    private Long buyerId;
    private Long sellerId;
    private Long productId1;
    private Long productId2;

    @BeforeEach
    void setUp() throws DatabaseException, AppException {
        this.orderDAO = new OrderDAOImpl();
        this.cartDAO = new CartDAOImpl();
        this.productDAO = new ProductDAOImpl();
        this.userDAO = new UserDAOImpl();

        this.orderService = new OrderServiceImpl(orderDAO, cartDAO, productDAO);

        User seller = userDAO.save(new User(null, "Seller", "orderservice_seller@test.com", "hash", Role.SELLER, null));
        User buyer = userDAO.save(new User(null, "Buyer", "orderservice_buyer@test.com", "hash", Role.BUYER, null));
        this.sellerId = seller.getId();
        this.buyerId = buyer.getId();

        Product p1 = productDAO.save(new Product(null, sellerId, "Keyboard", "Mechanical", new BigDecimal("100.00"), 10, "Electronics", null, null));
        Product p2 = productDAO.save(new Product(null, sellerId, "Cable", "USB-C", new BigDecimal("20.00"), 20, "Electronics", null, null));
        this.productId1 = p1.getId();
        this.productId2 = p2.getId();
    }

    @Test
    @DisplayName("Should execute 10-step atomic checkout transaction successfully")
    void testCheckoutAtomicSuccess() throws AppException {
        cartDAO.add(buyerId, productId1, 2); // 200.00
        cartDAO.add(buyerId, productId2, 1); // 20.00

        Order order = orderService.checkout(buyerId);

        assertNotNull(order.getId());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(0, new BigDecimal("220.00").compareTo(order.getTotalAmount()));

        // Check stock reduced
        assertEquals(8, productDAO.findById(productId1).orElseThrow().getStockQty());
        assertEquals(19, productDAO.findById(productId2).orElseThrow().getStockQty());

        // Check cart cleared
        assertTrue(cartDAO.findByUser(buyerId).isEmpty());
    }

    @Test
    @DisplayName("Should reject checkout with empty cart")
    void testCheckoutEmptyCart() {
        assertThrows(ValidationException.class, () -> orderService.checkout(buyerId));
    }

    @Test
    @DisplayName("Should rollback transaction when any item has insufficient stock")
    void testCheckoutInsufficientStockRollback() throws AppException {
        cartDAO.add(buyerId, productId1, 15); // only 10 in stock

        assertThrows(InsufficientStockException.class, () -> orderService.checkout(buyerId));

        // Verify stock is untouched (no partial deductions)
        assertEquals(10, productDAO.findById(productId1).orElseThrow().getStockQty());

        // Cart is NOT cleared
        assertEquals(1, cartDAO.findByUser(buyerId).size());
    }

    @Test
    @DisplayName("Should enforce role-based viewing permissions on orders")
    void testRoleBasedOrderVisibility() throws AppException {
        cartDAO.add(buyerId, productId1, 1);
        Order order = orderService.checkout(buyerId);

        // Buyer can view own order
        Order buyerView = orderService.getOrderById(order.getId(), buyerId, "BUYER");
        assertEquals(order.getId(), buyerView.getId());

        // Other buyer cannot view
        assertThrows(AuthorizationException.class, () -> orderService.getOrderById(order.getId(), 9999L, "BUYER"));

        // Seller of product can view
        Order sellerView = orderService.getOrderById(order.getId(), sellerId, "SELLER");
        assertEquals(order.getId(), sellerView.getId());

        // Admin can view any order
        Order adminView = orderService.getOrderById(order.getId(), 8888L, "ADMIN");
        assertEquals(order.getId(), adminView.getId());
    }

    @Test
    @DisplayName("Should cancel order, update status to CANCELLED, and restore product stock")
    void testCancelOrderAndRestoreStock() throws AppException {
        cartDAO.add(buyerId, productId1, 2);
        Order order = orderService.checkout(buyerId);

        assertEquals(8, productDAO.findById(productId1).orElseThrow().getStockQty());

        orderService.cancelOrder(order.getId(), buyerId, "BUYER");

        Order cancelledOrder = orderService.getOrderById(order.getId());
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());

        // Stock must be restored to 10
        assertEquals(10, productDAO.findById(productId1).orElseThrow().getStockQty());

        // Cannot cancel already cancelled order
        assertThrows(OrderException.class, () -> orderService.cancelOrder(order.getId(), buyerId, "BUYER"));
    }

    @Test
    @DisplayName("Should reject cancellation when order is already SHIPPED or DELIVERED")
    void testCancelOrderDisallowedStates() throws AppException {
        cartDAO.add(buyerId, productId1, 1);
        Order order = orderService.checkout(buyerId);

        orderService.updateOrderStatus(order.getId(), OrderStatus.SHIPPED);

        assertThrows(OrderException.class, () -> orderService.cancelOrder(order.getId(), buyerId, "BUYER"));
    }
}
