package com.practise.spring.graphQl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

@RestController
public class PersonController {
	
	
	@Autowired
	PersonRepository personRepository;
	
	@Value("classpath:/graphQl/person.graphqls")
	private Resource schemaResource;
	
	private GraphQL graphQl;
	
	private RuntimeWiring buildWiring() {
		
		DataFetcher<List<Person>> fetcher1 = data -> {
			return (List<Person>) personRepository.findAll();
		};
		
		DataFetcher<Person> fetcher2 = data -> {
			return personRepository.findByEmail(data.getArgument("email"));
		};
		
		return RuntimeWiring.newRuntimeWiring().type("Query", typeWriting -> typeWriting
				.dataFetcher("getAllPeople", fetcher1).dataFetcher("findPerson", fetcher2)).build();
	}
	
	
	@PostConstruct
	public void loadSchema() throws IOException {
		
		File schemaFile = schemaResource.getFile();
		TypeDefinitionRegistry  registry = new SchemaParser().parse(schemaFile);
		RuntimeWiring wiring = buildWiring();
		
		GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(registry, wiring);
		graphQl = new GraphQL.Builder(schema).build();
	}
	
	
	@PostMapping(value = "/getAll")
	public ResponseEntity<Object> getAllPeople(@RequestBody String query) {
		return new ResponseEntity<Object>(graphQl.execute(query), HttpStatus.OK);
	}

	@PostMapping(value = "/getPersonByEmail")
	public ResponseEntity<Object> getPersonByEmail(@RequestBody String query) {
		ExecutionResult executionResult =  graphQl.execute(query);
		return new ResponseEntity<>(executionResult.getData(), HttpStatus.OK);
	}
	
	@PostMapping(value = "/addPeople")
	public String addPeople(@RequestBody List<Person> people) {
		
		personRepository.saveAll(people);
		return "record inserted " + people.size(); 
	}
	
	@GetMapping(value = "/findAllPeople")
	public List<Person> getPeople() {
		return (List<Person>) personRepository.findAll();
	}
}
