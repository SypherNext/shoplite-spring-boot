package com.shoplite.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.shoplite.catalog.Product;
import com.shoplite.common.ConflictException;
import com.shoplite.customers.Customer;

// "order" is an SQL keyword, so the table is "orders".
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many orders belong to one customer. LAZY: the customer row loads only when needed.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // STRING stores "PAID", not 1: reordering the enum later cannot corrupt old rows.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Column(name = "notify_url", length = 500)
    private String notifyUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    // One order has many lines. The order owns them: saving or deleting the order
    // saves or deletes its lines (cascade), and a line removed from the list is deleted (orphanRemoval).
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {
    }

    public Order(Customer customer, String currency, String notifyUrl) {
        this.customer = customer;
        this.currency = currency;
        this.notifyUrl = notifyUrl;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.total = BigDecimal.ZERO;
        this.createdAt = Instant.now();
    }

    /** Adds a line and keeps the total in step. Both sides of the relationship are set here. */
    public void addLine(Product product, int quantity) {
        OrderLine line = new OrderLine(this, product, quantity);
        lines.add(line);
        total = total.add(line.lineTotal());
    }

    public void markPaid(String paymentId) {
        requireStatus(OrderStatus.PENDING_PAYMENT);
        this.paymentId = paymentId;
        this.status = OrderStatus.PAID;
    }

    public void markPaymentFailed() {
        requireStatus(OrderStatus.PENDING_PAYMENT);
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public void changeStatus(OrderStatus next) {
        if (!status.canMoveTo(next)) {
            throw new ConflictException("Order " + id + " cannot move from " + status + " to " + next);
        }
        this.status = next;
    }

    private void requireStatus(OrderStatus expected) {
        if (status != expected) {
            throw new ConflictException("Order " + id + " is " + status + ", expected " + expected);
        }
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

}
