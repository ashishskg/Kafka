package com.org.transfer.service;

import com.org.common.events.DepositRequestedEvent;
import com.org.common.events.WithdrawalRequestedEvent;
import com.org.transfer.entity.TransferEntity;
import com.org.transfer.error.TransferServiceException;
import com.org.transfer.model.TransferRestModel;
import com.org.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Environment environment;
    private final RestTemplate restTemplate;
    private final TransferRepository transferRepository;


    @Transactional("transactionManager")
    @Override
    public boolean transfer(TransferRestModel transferRestModel) {
        WithdrawalRequestedEvent withdrawalEvent = new WithdrawalRequestedEvent(transferRestModel.getSenderId(),
                transferRestModel.getRecepientId(), transferRestModel.getAmount());
        DepositRequestedEvent depositEvent = new DepositRequestedEvent(transferRestModel.getSenderId(),
                transferRestModel.getRecepientId(), transferRestModel.getAmount());

        // Create transfer entity object
        TransferEntity transferEntity = new TransferEntity();
        BeanUtils.copyProperties(transferRestModel, transferEntity);
        transferEntity.setTransferId(UUID.randomUUID().toString());
        try {

            // Save record to a database table
            transferRepository.save(transferEntity);

            kafkaTemplate.send(environment.getProperty("withdraw-money-topic", "withdraw-money-topic"),
                    withdrawalEvent);
            log.info("Sent event to withdrawal topic.");

            // Business logic that causes and error
            callRemoteServce();

            kafkaTemplate.send(environment.getProperty("deposit-money-topic", "deposit-money-topic"), depositEvent);
            log.info("Sent event to deposit topic");

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new TransferServiceException(ex);
        }
        return true;
    }

    private ResponseEntity<String> callRemoteServce() throws Exception {
        String requestUrl = "http://localhost:9504/response/200";
        ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);

        if (response.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            throw new Exception("Destination Microservice not availble");
        }

        if (response.getStatusCode().value() == HttpStatus.OK.value()) {
            log.info("Received response from mock service: " + response.getBody());
        }
        return response;
    }
   } 
