package com.example.bike_store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@RepositoryRestResource
public interface PersonRepository extends JpaRepository<Person, Long> {
    Person findByEmail(@Param("email") String email);

}