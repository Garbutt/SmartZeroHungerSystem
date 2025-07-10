package com.mycompany.smartzerohungersystem.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.mycompany.smartzerohungersystem.foodinventory.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ClientGUI implements ActionListener {

    private JTextField inputField, responseField;

    public static void main(String[] args) {
        ClientGUI gui = new ClientGUI();
        gui.build();
    }

    public void build() {
        JFrame frame = new JFrame("Smart Zero Hunger - FoodInventory");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);
        panel.setBorder(new EmptyBorder(new Insets(20, 20, 20, 20)));

        // UI Components
        panel.add(getFoodInventoryPanel());

        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }

    private JPanel getFoodInventoryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        panel.add(new JLabel("Food Item:"));
        panel.add(Box.createRigidArea(new Dimension(10, 0)));

        inputField = new JTextField("", 10);
        panel.add(inputField);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton button = new JButton("Check Inventory");
        button.setActionCommand("FoodInventory");
        button.addActionListener(this);
        panel.add(button);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));

        responseField = new JTextField("", 10);
        responseField.setEditable(false);
        panel.add(responseField);

        return panel;
    }

  @Override
public void actionPerformed(ActionEvent e) {
    String foodName = inputField.getText().trim();
    if (foodName.isEmpty()) {
        responseField.setText("Please enter a food item.");
        return;
    }

    ManagedChannel channel = null;
    try {
        channel = ManagedChannelBuilder.forAddress("localhost", 50051)
            .usePlaintext()
            .build();

        FoodInventoryServiceGrpc.FoodInventoryServiceBlockingStub stub = FoodInventoryServiceGrpc.newBlockingStub(channel);

        // Add item request with quantity 10 (example)
        AddItemRequest request = AddItemRequest.newBuilder()
            .setName(foodName)
            .setQuantity(10)
            .build();

        AddItemResponse addResponse = stub.addItem(request);
        responseField.setText(addResponse.getMessage());

        // Optionally get full inventory
        InventoryList inventory = stub.getInventory(Empty.newBuilder().build());

        System.out.println("Current Inventory:");
        for (InventoryItem item : inventory.getItemsList()) {
            System.out.println(item.getName() + ": " + item.getQuantity());
        }
    } catch (Exception ex) {
        ex.printStackTrace();
        responseField.setText("Error: " + ex.getMessage());
    } finally {
        if (channel != null) {
            channel.shutdown();
        }
    }
    }
}
