package com.mydomain.ws.notification.io;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;


@Entity
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@Table(name = "processed_events")
public class ProcessedEventEntity implements Serializable {

    private static final long serialVersionUID = 234343439090900990L;

    @Id
    @GeneratedValue
    private long id;

    @Nonnull
    @Column(nullable = false, unique = true)
    private String messageId;

    @Nonnull
    @Column(nullable = false)
    private String productId;
}
