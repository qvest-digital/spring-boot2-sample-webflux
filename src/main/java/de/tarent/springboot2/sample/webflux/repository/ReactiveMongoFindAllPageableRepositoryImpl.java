package de.tarent.springboot2.sample.webflux.repository;

import java.io.Serializable;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository;

import reactor.core.publisher.Flux;

public class ReactiveMongoFindAllPageableRepositoryImpl<T, ID extends Serializable>
		extends SimpleReactiveMongoRepository<T, ID>
		implements ReactiveMongoFindAllPageableRepository<T, ID> {
	private MongoEntityInformation<T, ID> entityInformation;
	private ReactiveMongoOperations mongoOperations;
	
	public ReactiveMongoFindAllPageableRepositoryImpl(
			MongoEntityInformation<T, ID> entityInformation, ReactiveMongoOperations mongoOperations) {
		super(entityInformation, mongoOperations);
		this.entityInformation = entityInformation;
		this.mongoOperations = mongoOperations;
	}
	
	@Override
	public Flux<T> findAll(Pageable pageable) {
		return mongoOperations.find(
			new Query().with(pageable),
			entityInformation.getJavaType(),
			entityInformation.getCollectionName());
	}
}
