package de.tarent.springboot2.sample.webflux.repository;

import de.tarent.springboot2.sample.webflux.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerRepository
		extends ReactiveMongoFindAllPageableRepository<Customer, String> {
	Mono<Customer> findByFirstName(String firstName);
	Flux<Customer> findByLastName(String lastName);
}
