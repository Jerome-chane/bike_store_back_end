package com.example.bike_store;

import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import java.util.*;

@Entity
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;
    private Date startDate;
    private Date returnDate;
    private Integer startPrice;
    private Integer finalPrice =0;
    private Integer startDuration;
    private Integer finalDuration;
    private String status = "out";

    @ManyToMany
    Set<Bike> bikes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="customer_id")
    private Person customer;

    @ElementCollection
    @OrderColumn(name="quantities_id")
    private Set<String> quantities = new HashSet<>();


    public Rental() {};

    public Rental(Set<Bike> bikes,Set<String> quantities, Date date, Integer startPrice,Integer duration, Person customer) {
        this.customer = customer;
        this.bikes = bikes;
        this.quantities = quantities;
        this.startDate = date;
        this.startPrice = startPrice;
        this.startDuration = duration;
        customer.addRental(this);
    };

    public void returnBike(Integer duration, Date returnDate) {

        Integer extra3PerBike =0; // this variable will represent the extra 3 euros per bike based on the quantity of bikes
        String value = this.quantities.toString();     // Since my Quantities si a set of Strings I use the loops below to convert it to a map<String string> in order to access the bike model and the corresponding quantity

       value = value.substring(1, value.length() - 1);  //remove the set's curly brackets
       String[] keyValuePairs = value.split(","); //split the string to creat key-value pairs
       Map<String, String> map = new HashMap<>();
       for (String pair : keyValuePairs)                 //iterate over the pairs
       {
           String[] entry = pair.split(":");      //split the pairs to get key and value
           map.put(entry[0].trim(), entry[1].trim());  //add them to the hashmap and trim whitespaces
       }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            for (Bike bike : this.bikes){             // iterate through the bike set of this rental
                if(bike.getModel().contentEquals(entry.getKey())){ // find the matching bike model
                    bike.returnInInventory(Integer.parseInt(entry.getValue())); // when matching model is found, the bike's inventory is updated by the corresponding quantity
                    extra3PerBike += Integer.parseInt(entry.getValue());  // this simply adds up quantity for all the bikes in the rental
                }
            }
        }

        this.returnDate = returnDate;
        if(this.startDuration == duration){// check if the return duration is equal to the original one. If equal, there is no extra charge
            this.finalPrice = this.startPrice; }

        if(this.startDuration<duration){ // If the return duration is higher, this will calculate the new total at + 3 euros per bike per day

            this.finalPrice += this.startPrice +((duration - this.startDuration)*(3*extra3PerBike));
            this.finalDuration = duration; // update the duration
        }
        this.status = "returned"; // change the status to returned
        ;}

    public Set<String> getQuantities() { return quantities; }
    public Integer getFinalPrice() { return finalPrice; }
    public void setCustomer(Person customer) { this.customer = customer; }
    public void setStatus(String status) { this.status = status; }
    public Long getId() { return id; }
    public Set<Bike> getBikes() { return bikes; }
    public Integer getStartDuration() { return startDuration; }
    public Integer getFinalDuration() { return finalDuration; }
    public Person getCustomer() { return customer; }
    public Integer getStartPrice() { return startPrice; }
    public Date getStartDate() { return startDate; }
    public Date getReturnDate() { return returnDate; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return "Rental{" +
                "id=" + id +
                ", startDate=" + startDate +
                ", returnDate=" + returnDate +
                ", startPrice=" + startPrice +
                ", status='" + status + '\'' +
                ", bike=" + bikes +
                ", customer=" + customer +
                '}';
    }
}
