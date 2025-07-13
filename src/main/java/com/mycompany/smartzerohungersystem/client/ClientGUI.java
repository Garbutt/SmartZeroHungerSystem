package com.mycompany.smartzerohungersystem.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.mycompany.smartzerohungersystem.foodinventory.*;
import com.mycompany.smartzerohungersystem.demandtracker.DemandTrackerImpl;
import com.mycompany.smartzerohungersystem.distributionoptimizer.DistributionOptimizerImpl;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.net.InetAddress;
import java.io.IOException;

public class ClientGUI implements ActionListener {

    private JTextField inputField, responseField;
    private Runnable onCloseCallback;

    private Server foodServer;
    private Server demandServer;
    private Server distributionServer;

    private JButton foodStartButton, foodStopButton;
    private JButton demandStartButton, demandStopButton;
    private JButton distributionStartButton, distributionStopButton;

    public static void main(String[] args) {
        ClientGUI gui = new ClientGUI();
        gui.build();
    }

    public void build() {
        build(null);
    }

    public void build(Runnable onCloseCallback) {
        this.onCloseCallback = onCloseCallback;
        JFrame frame = new JFrame("Smart Zero Hunger System");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                shutdownAllServers();
                if (ClientGUI.this.onCloseCallback != null) {
                    ClientGUI.this.onCloseCallback.run();
                }
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(new Insets(20, 20, 20, 20)));

        // Add service control buttons
        panel.add(getServiceControlPanel());

        // Add food inventory interaction
        panel.add(getFoodInventoryPanel());

        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }

    private JPanel getServiceControlPanel() {
        JPanel servicePanel = new JPanel();
        servicePanel.setLayout(new GridLayout(3, 3, 10, 10));
        servicePanel.setBorder(BorderFactory.createTitledBorder("Service Controls"));

        // Food Inventory
        foodStartButton = new JButton("Start Food Inventory");
        foodStopButton = new JButton("Stop Food Inventory");
        foodStartButton.addActionListener(e -> startService("FoodInventory", 50051, ServiceType.FOOD));
        foodStopButton.addActionListener(e -> stopService(ServiceType.FOOD));
        servicePanel.add(new JLabel("Food Inventory"));
        servicePanel.add(foodStartButton);
        servicePanel.add(foodStopButton);

        // Demand Tracker
        demandStartButton = new JButton("Start Demand Tracker");
        demandStopButton = new JButton("Stop Demand Tracker");
        demandStartButton.addActionListener(e -> startService("DemandTracker", 50052, ServiceType.DEMAND));
        demandStopButton.addActionListener(e -> stopService(ServiceType.DEMAND));
        servicePanel.add(new JLabel("Demand Tracker"));
        servicePanel.add(demandStartButton);
        servicePanel.add(demandStopButton);

        // Distribution Optimizer
        distributionStartButton = new JButton("Start Distribution Optimizer");
        distributionStopButton = new JButton("Stop Distribution Optimizer");
        distributionStartButton.addActionListener(e -> startService("DistributionOptimizer", 50053, ServiceType.DISTRIBUTION));
        distributionStopButton.addActionListener(e -> stopService(ServiceType.DISTRIBUTION));
        servicePanel.add(new JLabel("Distribution Optimizer"));
        servicePanel.add(distributionStartButton);
        servicePanel.add(distributionStopButton);

        return servicePanel;
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

            AddItemRequest request = AddItemRequest.newBuilder()
                    .setName(foodName)
                    .setQuantity(10)
                    .build();

            AddItemResponse addResponse = stub.addItem(request);
            responseField.setText(addResponse.getMessage());

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

    private enum ServiceType {
        FOOD, DEMAND, DISTRIBUTION
    }

    private void startService(String name, int port, ServiceType type) {
        try {
            Server server = null;
            switch (type) {
                case FOOD:
                    if (foodServer == null) {
                        foodServer = ServerBuilder.forPort(port).addService(new FoodInventoryImpl()).build().start();
                        registerMdns(name, port);
                        System.out.println("Started FoodInventory on port " + port);
                    }
                    break;
                case DEMAND:
                    if (demandServer == null) {
                        demandServer = ServerBuilder.forPort(port).addService(new DemandTrackerImpl()).build().start();
                        registerMdns(name, port);
                        System.out.println("Started DemandTracker on port " + port);
                    }
                    break;
                case DISTRIBUTION:
                    if (distributionServer == null) {
                        distributionServer = ServerBuilder.forPort(port).addService(new DistributionOptimizerImpl()).build().start();
                        registerMdns(name, port);
                        System.out.println("Started DistributionOptimizer on port " + port);
                    }
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to start " + name + ": " + e.getMessage());
        }
    }

    private void stopService(ServiceType type) {
        try {
            switch (type) {
                case FOOD:
                    if (foodServer != null) {
                        foodServer.shutdown();
                        foodServer = null;
                        System.out.println("Stopped FoodInventory");
                    }
                    break;
                case DEMAND:
                    if (demandServer != null) {
                        demandServer.shutdown();
                        demandServer = null;
                        System.out.println("Stopped DemandTracker");
                    }
                    break;
                case DISTRIBUTION:
                    if (distributionServer != null) {
                        distributionServer.shutdown();
                        distributionServer = null;
                        System.out.println("Stopped DistributionOptimizer");
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shutdownAllServers() {
        stopService(ServiceType.FOOD);
        stopService(ServiceType.DEMAND);
        stopService(ServiceType.DISTRIBUTION);
    }

    private void registerMdns(String name, int port) throws IOException {
        JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
        ServiceInfo info = ServiceInfo.create("_grpc._tcp.local.", name, port, "");
        jmdns.registerService(info);
        System.out.println("Registered " + name + " via jmDNS");
    }
}
