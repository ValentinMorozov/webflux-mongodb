package com.example.mongoReactive.controller;

import com.example.mongoReactive.repository.CheckRepository;
import com.example.mongoReactive.service.ChecksService;
import com.example.mongoReactive.util.ConvertDataException;
import com.example.mongoReactive.util.IllegalObjectIdException;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ApiController {
    private CheckRepository checkRepository;
    private ChecksService checksService;
    public ApiController(CheckRepository checkRepository, ChecksService checksService){
        this.checkRepository = checkRepository;
        this.checksService = checksService;
    }

    @GetMapping("/checks/{id}")
    public Flux<Document> getCheck(@PathVariable String id)  throws IllegalObjectIdException {
        return this.checkRepository.findById(id);
    }

    @GetMapping("/checks")
    public Flux<Document> getChecks(){
      return checkRepository.findAll();
    }

    @PostMapping(path = "/check",
            consumes = {MediaType.APPLICATION_XML_VALUE})
    public Mono<Document> saveCheck(@RequestHeader("Content-Type") String contentType, @RequestBody String body)
            throws IllegalObjectIdException, ConvertDataException, IOException  {
        Document doc = checksService.xml2Document(body, "****", null);
        return checkRepository.save("", doc);
    }
    @PostMapping(path = "/check/{id}",
            consumes = {MediaType.APPLICATION_XML_VALUE})
    public Mono<Document> saveCheck(@RequestHeader("Content-Type") String contentType,
                                    @PathVariable String id,
                                    @RequestBody String body)
            throws  IllegalObjectIdException, ConvertDataException, IOException {
        Document doc = checksService.xml2Document(body, "****", null);
        return checkRepository.save(id, doc);
    }

    @ExceptionHandler(IllegalObjectIdException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Document> handleException1(IllegalObjectIdException e) {
        return Mono.just(new Document("Error", e.getCause().getMessage()));
    }

    @ExceptionHandler(ConvertDataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Document> handleException2(ConvertDataException e) {
        return Mono.just(new Document("Error", e.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Document> handleException3(IOException e) {
        return Mono.just(new Document("Error", e.getMessage()));
    }
}