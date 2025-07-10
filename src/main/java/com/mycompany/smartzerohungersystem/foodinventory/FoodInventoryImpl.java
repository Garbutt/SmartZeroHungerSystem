/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.smartzerohungersystem.foodinventory;

import io.grpc.stub.StreamObserver;
import com.mycompany.smartzerohungersystem.foodinventory.AddItemRequest;
import com.mycompany.smartzerohungersystem.foodinventory.AddItemResponse;
import com.mycompany.smartzerohungersystem.foodinventory.InventoryItem;
import com.mycompany.smartzerohungersystem.foodinventory.InventoryList;
import com.mycompany.smartzerohungersystem.foodinventory.Empty;

import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author reece
 */
public class FoodInventoryImpl extends FoodInventoryServiceGrpc.FoodInventoryServiceImplBase {
    
    private final List<InventoryItem> inventory = new ArrayList<>();
    
@Override
public void addItem(AddItemRequest request, StreamObserver<AddItemResponse> responseObserver) {
    String itemName = request.getName();
    int quantity = request.getQuantity();

    inventory.add(InventoryItem.newBuilder().setName(itemName).setQuantity(quantity).build());

    AddItemResponse response = AddItemResponse.newBuilder()
        .setMessage("Added " + quantity + " of " + itemName)
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
}

@Override
public void getInventory(Empty request, StreamObserver<InventoryList> responseObserver) {
    InventoryList response = InventoryList.newBuilder()
        .addAllItems(inventory)
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
}
}
