package com.example.bike_store;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Entity
public class Bike {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;
    private String model;
    private String brand;
    private String type;
    private  String image;
    private Integer basePrice;
    private Integer inventory;
    private Integer quantity = 1; // only used when a rent request is sent by the front end

    @ManyToMany(mappedBy = "bikes")
    Set<Rental> rentals;

    public Bike(){};
    public Bike(String model, String brand, String type,String image,Integer basePrice, Integer inventory){
        this.model = model;
        this.brand = brand;
        this.type = type;
        this.image = image;
        this.basePrice = basePrice;
        this.inventory = inventory;
    };

    public void addRental(Rental rental){ this.rentals.add(rental);}
    public void returnInInventory(Integer inventory) { this.inventory += inventory; }
    public void updateInventory(Integer inventory) { this.inventory = inventory; }

    public Integer getQuantity() { return quantity; }
    public String getImage() { return image; }
    public Integer getInventory() { return inventory; }
    public String getType() { return type; }
    public Long getId() { return id; }
    public String getModel() { return model; }
    public String getBrand() { return brand; }
    public Integer getBasePrice() { return basePrice; }
    public Set<Rental> getRentals() { return rentals; }

    @Override
    public String toString() {
        return model ; // return the corresponding bike's model. Used to build the "quantities rental"
    }

}