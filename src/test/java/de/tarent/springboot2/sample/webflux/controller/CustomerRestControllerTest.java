package de.tarent.springboot2.sample.webflux.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import de.tarent.springboot2.sample.webflux.model.Customer;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient
// enables auto configuration of embedded mongo db (through dependency to de.flapdoodle.embed.mongo)
@EnableAutoConfiguration
@TestInstance(Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class CustomerRestControllerTest {
	private WebTestClient webClient;
	@Autowired
	private ReactiveMongoOperations mongoTemplate;
	
	@Autowired
	private ApplicationContext context;
	
	private Customer customer1;
	private Customer customer2;
	
	@BeforeAll
	public void setupWebTestClient() {
		webClient = WebTestClient.bindToApplicationContext(context)
			.apply(springSecurity())
			.configureClient()
			.filter(basicAuthentication())
			.build();
	}
	
	@BeforeAll
	public void insertData() {
		customer1 = new Customer();
		customer1.setId("4711");
		customer1.setFirstName("Robert");
		customer1.setLastName("Baratheon");
		
		customer2 = new Customer();
		customer2.setId("4712");
		customer2.setFirstName("Eddard");
		customer2.setLastName("Stark");
		
		mongoTemplate.insert(customer1).then(mongoTemplate.insert(customer2)).block();
	}
	
	@Test
	@WithMockUser
	void testFindCustomers() {
		webClient.get()
				.uri("/customers/")
				.accept(MediaType.APPLICATION_JSON_UTF8)
			.exchange()
			.expectStatus()
				.isOk()
			.expectBodyList(Customer.class)
				.contains(customer1, customer2);
	}

	@Test
	@WithMockUser(roles = {"ADMIN", "USER"})
	void testCreateCustomer() {
		Customer daenerys = new Customer();
		daenerys.setFirstName("Daenerys");
		daenerys.setLastName("Targaryen");
		
		webClient.mutateWith(csrf()).post()
				.uri("/customers/")
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.body(BodyInserters.fromObject(daenerys))
			.exchange()
			.expectStatus()
				.isCreated()
			.expectBody(Customer.class).consumeWith(exchange -> {
				Customer created = exchange.getResponseBody();
				assertThat(created.getId(), not(isEmptyOrNullString()));
				assertThat(created.getFirstName(), is(daenerys.getFirstName()));
				assertThat(created.getLastName(), is(daenerys.getLastName()));
			});
	}

	@SuppressWarnings("unchecked")
	@Test
	@WithMockUser(roles = "ADMIN")
	void testCreateInvalidCustomer() {
		Customer invalid = new Customer();
		
		webClient.mutateWith(csrf())
			.post()
				.uri("/customers/")
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.body(BodyInserters.fromObject(invalid))
			.exchange()
			.expectStatus()
				.isBadRequest()
			.expectBody(Map.class).consumeWith(exchange -> {
				Map<String, ?> error = exchange.getResponseBody();
				List<Map<String, String>> errors = (List<Map<String, String>>) error.get("errors");
				
				List<String> fields = errors.stream().map(anError -> anError.get("field")).collect(Collectors.toList());
				
				assertThat(fields, containsInAnyOrder("firstName", "lastName"));
			});
	}
	
	@Test
	@WithMockUser
	void testFindCustomer() {
		webClient.get()
				.uri("/customers/{id}", Map.of("id", customer1.getId()))
				.accept(MediaType.APPLICATION_JSON_UTF8)
			.exchange()
			.expectStatus().isOk()
			.expectBody(Customer.class).isEqualTo(customer1);
	}

	@Test
	@WithMockUser
	void testFindCustomerNotFound() {
		webClient.get()
				.uri("/customers/{id}", Map.of("id", "XXX"))
				.accept(MediaType.APPLICATION_JSON_UTF8)
			.exchange()
			.expectStatus().isNotFound();
	}
	
	@Test
	@WithMockUser(roles = "ADMIN")
	void testUpdateCustomer() {
		Customer customer3 = new Customer();
		customer3.setFirstName("Jon");
		customer3.setLastName("Schnee");
		mongoTemplate.insert(customer3).block();
		
		
		Customer customerSpoiled = customer3.clone();
		customerSpoiled.setFirstName("Aegon");
		customerSpoiled.setLastName("Targaryen");
		
		webClient.mutateWith(csrf()).put()
				.uri("/customers/{id}", Map.of("id", customer3.getId()))
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.body(BodyInserters.fromObject(customerSpoiled))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Customer.class).isEqualTo(customerSpoiled);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void testDeleteCustomer() {
		Customer tywinLennister = new Customer();
		tywinLennister.setId("dead");
		tywinLennister.setFirstName("Tywin");
		tywinLennister.setLastName("Lennister");
		
		mongoTemplate.insert(tywinLennister).block();
		
		webClient.mutateWith(csrf()).delete()
			.uri("/customers/{id}", Map.of("id", tywinLennister.getId()))
			.exchange()
			.expectStatus().isNoContent();
	}

	@Test
	@WithMockUser
	void testStreamCustomers() {
		webClient.get().uri("/customers/").accept(MediaType.TEXT_EVENT_STREAM)
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Customer.class).contains(customer1, customer2);
	}

}
