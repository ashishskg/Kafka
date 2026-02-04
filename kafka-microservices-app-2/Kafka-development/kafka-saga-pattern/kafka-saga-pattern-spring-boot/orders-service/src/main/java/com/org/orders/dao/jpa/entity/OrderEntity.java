package com.org.orders.dao.jpa.entity;

import com.org.common.types.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Table(name = "orders")
@Entity
@Getter
@Setter
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "status")
    private OrderStatus status;
    @Column(name = "customer_id")
    private UUID customerId;
    @Column(name = "product_id")
    private UUID productId;
    @Column(name = "product_quantity")
    private Integer productQuantity;
}
