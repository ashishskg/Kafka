package com.org.transfer.entity;


import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="transfers")
public class TransferEntity implements Serializable {

    private static final long serialVersionUID = -6202121587624962351L;

    @Id
    @Column(nullable = false)
    private String transferId;

    @Column(nullable = false)
    private String senderId;

    @Column(nullable = false)
    private String recepientId;

    @Column(nullable = false)
    private BigDecimal amount;
}
