package com.org.transfer.controller;

import com.org.transfer.model.TransferRestModel;
import com.org.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/transfers")
public class TransfersController {
    private final TransferService transferService;


    @PostMapping
    public boolean transfer(@RequestBody TransferRestModel transferRestModel) {
        return transferService.transfer(transferRestModel);
    }
}
