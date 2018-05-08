package de.tarent.springboot2.sample.webflux.model;

import javax.validation.constraints.Min;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Paging {
	@Min(1)
	private Integer size = Integer.valueOf(10);
	@Min(0)
	private Integer page = Integer.valueOf(0);
}
