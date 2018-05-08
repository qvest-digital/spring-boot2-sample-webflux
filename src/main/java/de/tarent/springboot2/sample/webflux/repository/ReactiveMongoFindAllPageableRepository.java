package de.tarent.springboot2.sample.webflux.repository;

import java.io.Serializable;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

import reactor.core.publisher.Flux;

@NoRepositoryBean
public interface ReactiveMongoFindAllPageableRepository<T, ID extends Serializable>
		extends ReactiveMongoRepository<T, ID>{
	Flux<T> findAll(Pageable pageable);
}
