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
import com.google.protobuf.Empty;

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

    boolean updated = false;

    for (int i = 0; i < inventory.size(); i++) {
        InventoryItem item = inventory.get(i);
        if (item.getName().equalsIgnoreCase(itemName)) {
            int newQty = item.getQuantity() + quantity;
            inventory.set(i, InventoryItem.newBuilder().setName(itemName).setQuantity(newQty).build());
            updated = true;
            break;
        }
    }

    if (!updated) {
        inventory.add(InventoryItem.newBuilder().setName(itemName).setQuantity(quantity).build());
    }

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
