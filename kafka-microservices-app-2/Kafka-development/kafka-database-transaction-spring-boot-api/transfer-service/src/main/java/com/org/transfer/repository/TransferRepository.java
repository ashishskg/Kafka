package com.org.transfer.repository;

import com.org.transfer.entity.TransferEntity;
import org.h2.value.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<TransferEntity, Long> {
}
