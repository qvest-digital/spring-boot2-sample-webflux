package de.tarent.springboot2.sample.webflux.controller;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

import javax.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import de.tarent.springboot2.sample.webflux.model.Customer;
import de.tarent.springboot2.sample.webflux.model.Paging;
import de.tarent.springboot2.sample.webflux.repository.CustomerRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/customers")
public class CustomerRestController {
    private CustomerRepository customerRepository;
    
    public CustomerRestController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    @GetMapping("/")
    @PreAuthorize("hasRole('USER')")
    public Flux<Customer> findCustomers(@Valid @ModelAttribute Paging paging) {
    	PageRequest pageRequest = PageRequest.of(paging.getPage(), paging.getSize());
    	
    	// don't do that: customerRepository.findAll().skip(++paging.getPage() * paging.getSize()).take(paging.getSize());
        return customerRepository.findAll(pageRequest);
    }
    
    @PostMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Customer>> createCustomer(
    		@Valid @RequestBody Customer customer, UriComponentsBuilder uriBuilder) {
        return customerRepository.insert(customer)
            .map(createdCustomer -> ResponseEntity.created(
            		uriBuilder.path("{id}")
            			.buildAndExpand(createdCustomer.getId())
            			.toUri())
    		.body(createdCustomer));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<Customer>> findCustomer(@PathVariable String id) {
        return customerRepository.findById(id)
            .map(customer -> ResponseEntity.ok(customer))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    // TODO allow update of own profile
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Customer>> updateCustomer(
            @PathVariable String id, @Valid @RequestBody Customer customer) {
        return customerRepository.findById(id)
            .flatMap(existingCustomer -> customerRepository.save(customer))
            .map(updatedCustomer -> ResponseEntity.ok(updatedCustomer))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> deleteCustomer(@PathVariable String id) {
        return customerRepository.findById(id)
            .flatMap(existingCustomer -> customerRepository.delete(existingCustomer)
                    .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT))))
            .defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping(path = "/stream", produces = TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('USER')")
    public Flux<Customer> streamCustomers() {
        return customerRepository.findAll();
    }
}
