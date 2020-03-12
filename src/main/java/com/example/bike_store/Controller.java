package com.example.bike_store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class Controller {

    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private BikeRepository bikeRepository;
    @Autowired
    private RentalRespository rentalRespository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private boolean isGuest(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }
    public Person getAuthPerson(Authentication authentication) {
        return personRepository.findByEmail(authentication.getName());
    }

    @RequestMapping("/return/rental/{id}") // this allow the user to initiate a return. Has duration as paramerer (to simulate the amount of days out) I don't know how to do this based on the date
    public ResponseEntity<Map<String, Object>> returnRental(@PathVariable Long id,@RequestBody Integer duration,Authentication authentication){

        Date return_date = new Date(); // initiate the return date
        Rental repo_rental = rentalRespository.getOne(id); // finds the corresponding rental based on the provided id
//        if(isGuest(authentication)){
//            return new ResponseEntity<>(makeMap("error", "You must be logged to make a return"), HttpStatus.UNAUTHORIZED); // if user is not logged in this will send UNAUTHORIZED
//        }
        repo_rental.returnBike(duration,return_date);
        rentalRespository.save(repo_rental); // save the changes made
        return new ResponseEntity<>(makeMap("ok", "bikes successfully returned"),HttpStatus.ACCEPTED);
    }

    @RequestMapping("/my-rentals") // return all the rentals of the logged Person
    public ResponseEntity<Map<String, Object>> getMyRentals(Authentication authentication) {
        Map<String,Object> dto = new LinkedHashMap<>();
//        if(isGuest(authentication)){
//            return new ResponseEntity<>(makeMap("error", "You must be logged to access your rentals"), HttpStatus.UNAUTHORIZED);
//        }
//        Person customer = getAuthPerson(authentication); // get the corresponding user



        Person customer = personRepository.findByEmail("b.com"); // I use this to get the user that is already registered
        dto.put("rentals", customer.getRentals().stream().map(rental -> RentalDTOforCustomers(rental)).collect(Collectors.toList()));
        return new ResponseEntity<>(makeMap("ok",dto),HttpStatus.CREATED);
    }

    @RequestMapping(value = "/rent/{duration}/days", method = RequestMethod.POST) // create a new renal
    public ResponseEntity<Map<String, Object>> rent(@PathVariable Integer duration, @RequestBody Set<Bike> bikes, Authentication authentication) {
//        if (isGuest(authentication)) {
//            return new ResponseEntity<>(makeMap("error", "You must be logged is to rent a bike"), HttpStatus.UNAUTHORIZED); // check is the user is logged in
//        }
//        Person customer = getAuthPerson(authentication);



        Person customer = personRepository.findByEmail("b.com"); // I use this to get the user that is already registered

        Date start_date = new Date(); // initiate a new start date
        Set<Bike> bikeSet = new HashSet<>(); // create an empty set to store the corresponding bikes that are reserved
        Set<String> quantities = new HashSet<>(); // initiate a set of Sting to collect the bike's model and the quantity
        Integer total_base_price = 0; // initiate the base price for the rental

        for(Bike bike : bikes){ // iterate through the set of bikes passed in the body

            Bike repo_bike = bikeRepository.getOne(bike.getId());  // look in the given set of bikes and finds the corresponding one from the repository

            bikeSet.add(repo_bike); // add the bike in the previousely initiated Set

            quantities.add(repo_bike.toString()+": "+bike.getQuantity().toString()); // create a string with the bike model and the matching quantity and add it in the Set
            if(bike.getQuantity() <= 0){
                return new ResponseEntity<>(makeMap("error ","bike quantity must be higher than 0"),HttpStatus.FORBIDDEN);
            }
            if ((repo_bike.getInventory() - bike.getInventory()) < 0) { // If the requested quantity is bigger than the available stock  returns forbidden RESPONSE
                return new ResponseEntity<>(makeMap("error ","bike is out of stock"),HttpStatus.FORBIDDEN);
            }
            if ((repo_bike.getInventory() - bike.getQuantity()) >= 0){ // if the requested quantity is available in the inventory
                total_base_price += (repo_bike.getBasePrice()*bike.getQuantity()*duration); // multiply the bike's base price for the quantity and sum it to the "total base price"
                repo_bike.updateInventory(repo_bike.getInventory()-bike.getQuantity()); // update the bike's inventory by substracting the quantity to the inventory
            }
        }
        Rental newRental = new Rental(bikeSet, quantities,start_date, total_base_price, duration, customer); // create a new rental
        rentalRespository.save(newRental);

        return new ResponseEntity<>(makeMap("success","Rent accepted"),HttpStatus.CREATED);
    }


    @RequestMapping("/api/customers") // Return an API with all the registered customers
    public Map<String, Object>  getCustomers() {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        Set<Person> customers = new HashSet<>();
        personRepository.findAll().stream().map(person ->{if (person.getRole().contentEquals("customer")){customers.add(person);}return customers;}).collect(Collectors.toList());
        // check in the personRepository which Person as the role of "customer" and add these persons to the Set of Customers
        dto.put("customers", customers.stream().map(customer->CustomerDTO(customer)).collect(Collectors.toList()));
        return dto;
    }

    @RequestMapping("/api/bikes") // return the api of all the bikes
    public Map<String, Object>  getBikes(Authentication authentication) {
        Map<String,Object> dto = new LinkedHashMap<>();

        if(!isGuest(authentication)){
            dto.put("user", loginDTO(authentication)); // for some reasons it's not working online :(
        }
        else{
            dto.put("user", null);
        }
        dto.put("bikes", bikeRepository.findAll().stream().map(bike -> bikeDTO(bike)).collect(Collectors.toList()));
        return dto;
    }

    private Map<String, Object> CustomerDTO(Person customer) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("firstName", customer.getFirstName());
        dto.put("lastName", customer.getLastName());
        dto.put("email", customer.getEmail());
        dto.put("rentals", customer.getRentals().stream().map(rental ->RentalDTOforCustomers(rental)));
        return dto;
    }

    private  Map<String, Object> RentalDTOforCustomers(Rental rental){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", rental.getId());
        dto.put("status", rental.getStatus());
        dto.put("start_date", rental.getStartDate());
        dto.put("return_date", rental.getReturnDate());
        dto.put("start_price", rental.getStartPrice());
        dto.put("final_price", rental.getFinalPrice());
        dto.put("start_duration", rental.getStartDuration());
        dto.put("final_duration", rental.getFinalDuration());
        dto.put("quantities", rental.getQuantities());
        dto.put("rented_bike", rental.getBikes().stream().map(bike -> BikeDTOforRental(bike)));
        return dto;
    }

    private Map<String,Object> BikeDTOforRental(Bike bike){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", bike.getId());
        dto.put("model", bike.getModel());
        dto.put("brand", bike.getBrand());
        dto.put("type", bike.getType());
        dto.put("image", bike.getImage());
        dto.put("base_price", bike.getBasePrice());
        dto.put("quantity", bike.getQuantity());

        return dto;
    };

    private Map<String,Object> bikeDTO(Bike bike){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", bike.getId());
        dto.put("model", bike.getModel());
        dto.put("brand", bike.getBrand());
        dto.put("type", bike.getType());
        dto.put("image", bike.getImage());
        dto.put("base_price", bike.getBasePrice());
        dto.put("inventory", bike.getInventory());
        dto.put("quantity", bike.getQuantity());

        if(bike.getRentals().size()==0){
            dto.put("rentals", null);
        } if(bike.getRentals().size()>0){
            dto.put("rentals", bike.getRentals().stream().map(rental ->RentalDTOforBikes(rental)));
        }
        return dto;
    };
    private  Map<String, Object> RentalDTOforBikes(Rental rental){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", rental.getId());
        dto.put("status", rental.getStatus());
        dto.put("start_date", rental.getStartDate());
        dto.put("return_date", rental.getReturnDate());
        dto.put("start_price", rental.getStartPrice());
        dto.put("final_price", rental.getFinalPrice());
        dto.put("start_duration", rental.getStartDuration());
        dto.put("final_duration", rental.getFinalDuration());
        dto.put("customer",CustomerRentalDTO(rental.getCustomer()));
        return dto;
    }
    private Map<String, Object> CustomerRentalDTO(Person customer) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("firstName", customer.getFirstName());
        dto.put("lastName", customer.getLastName());
        dto.put("email", customer.getEmail());
        return dto;
    }
    private Map<String, Object> loginDTO(Authentication authentication) { // Loging DTO will check which person is logged in and will return the apropriate information
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        Person person = getAuthPerson(authentication);
        if(person != null) {
            dto.put("firstName", personRepository.findByEmail(authentication.getName()).getFirstName());
            dto.put("lastName", personRepository.findByEmail(authentication.getName()).getLastName());
            dto.put("email", personRepository.findByEmail(authentication.getName()).getEmail());
            dto.put("role", personRepository.findByEmail(authentication.getName()).getRole());
        }
        return dto;
    }


    @RequestMapping(value = "/api/signup", method = RequestMethod.POST) // a simple Sign up function
    public ResponseEntity<Map<String, Object>> addPerson(@RequestBody Person person) { // Add a Person
        Person isPerson = personRepository.findByEmail(person.getEmail());

        if (isPerson != null) {
            return new ResponseEntity<>(makeMap("error","Person already exists"), HttpStatus.CONFLICT);
        }
        Person newPerson = new Person(person.getFirstName(), person.getLastName(), person.getEmail(), person.getRole(),passwordEncoder.encode(person.getPassword()));
        personRepository.save(newPerson);
        return new ResponseEntity<>(makeMap("success","Person Added"),HttpStatus.CREATED);
    }
    private Map<String, Object> makeMap(String key, Object value) { // Makes the response sent with the response entity
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}