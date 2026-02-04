package com.org.orders.dao.jpa.entity;

import com.org.common.types.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

@Table(name = "orders_history")
@Entity
@Getter
@Setter
public class OrderHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "order_id")
    private UUID orderId;
    @Column(name = "status")
    private OrderStatus status;
    @Column(name = "created_at")
    private Timestamp createdAt;
}
