package com.mycompany.smartzerohungersystem.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.mycompany.smartzerohungersystem.foodinventory.*;
import com.mycompany.smartzerohungersystem.demandtracker.*;
import com.mycompany.smartzerohungersystem.distributionoptimizer.*;
import com.mycompany.smartzerohungersystem.distributionoptimizer.DistributionOptimizer.*;

import com.google.protobuf.Empty;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.net.InetAddress;
import java.io.IOException;

import java.util.Iterator;

public class ClientGUI implements ActionListener {

    private JTextField inputField, responseField, regionField, countField, planResultField, quantityField, optimizerRegionField, optimizationSummaryField;
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
        panel.add(getDemandTrackerPanel());
        panel.add(getDistributionOptimizerPanel());

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
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Food Inventory"));

        // Input + Button Panel
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        inputPanel.add(new JLabel("Food Item:"));
        inputField = new JTextField(10);
        inputPanel.add(inputField);

        inputPanel.add(new JLabel("Quantity:"));
        quantityField = new JTextField(5);
        inputPanel.add(quantityField);

        JButton addButton = new JButton("Add to Inventory");
        addButton.addActionListener(e -> addToInventory());
        inputPanel.add(addButton);

        JButton viewButton = new JButton("View Inventory");
        viewButton.addActionListener(e -> viewInventory());
        inputPanel.add(viewButton);

        panel.add(inputPanel);

        // Response Field
        JPanel responsePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        responseField = new JTextField(30);
        responseField.setEditable(false);
        responsePanel.add(responseField);
        panel.add(responsePanel);

        return panel;
    }

    private JPanel getDemandTrackerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Demand Tracker"));

        // Submit Hunger Report Panel
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Region:"));
        regionField = new JTextField(10);
        inputPanel.add(regionField);

        inputPanel.add(new JLabel("Count:"));
        countField = new JTextField(5);
        inputPanel.add(countField);

        JButton sendReportButton = new JButton("Send Hunger Report");
        inputPanel.add(sendReportButton);

        panel.add(inputPanel);

        // Request Optimized Plan Panel
        JPanel planPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton getPlanButton = new JButton("Request Optimized Distribution Plan");
        planPanel.add(getPlanButton);

        planResultField = new JTextField(30);
        planResultField.setEditable(false);
        planPanel.add(planResultField);

        panel.add(planPanel);

        // Event handler for sending hunger reports
        sendReportButton.addActionListener(e -> {
            String region = regionField.getText().trim();
            String countText = countField.getText().trim();

            if (region.isEmpty() || countText.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter both region and count.");
                return;
            }

            try {
                int count = Integer.parseInt(countText);
                ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50052).usePlaintext().build();
                DemandTrackerServiceGrpc.DemandTrackerServiceStub stub = DemandTrackerServiceGrpc.newStub(channel);

                StreamObserver<DemandTracker.AggregateReport> responseObserver = new StreamObserver<>() {
                    @Override
                    public void onNext(DemandTracker.AggregateReport report) {
                        JOptionPane.showMessageDialog(null, "Report submitted. Total regions: " + report.getTotalReports() + ", Total count: " + report.getTotalCount());
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error: " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        channel.shutdown();
                    }
                };

                StreamObserver<DemandTracker.HungerReport> requestObserver = stub.streamReports(responseObserver);
                DemandTracker.HungerReport report = DemandTracker.HungerReport.newBuilder()
                        .setRegion(region)
                        .setCount(count)
                        .build();
                requestObserver.onNext(report);
                requestObserver.onCompleted();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Count must be an integer.");
            }
        });

        getPlanButton.addActionListener(e -> {
            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder.forAddress("localhost", 50052)
                        .usePlaintext()
                        .build();

                DemandTrackerServiceGrpc.DemandTrackerServiceBlockingStub stub
                        = DemandTrackerServiceGrpc.newBlockingStub(channel);

                com.google.protobuf.Empty request = com.google.protobuf.Empty.newBuilder().build();
                DemandTracker.AllRegionStats allStats = stub.getAllDemandStats(request);

                // Build a readable result string
                StringBuilder result = new StringBuilder();
                for (DemandTracker.DemandStats stats : allStats.getStatsList()) {
                    result.append("Region: ")
                            .append(stats.getRegion())
                            .append(", Total Food Count: ")
                            .append(stats.getReportCount())
                            .append("\n");
                }

                // Show in a scrollable popup or large field
                JTextArea textArea = new JTextArea(result.toString(), 10, 40);
                textArea.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(textArea);
                JOptionPane.showMessageDialog(null, scrollPane, "All Region Stats", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error fetching stats: " + ex.getMessage());
            } finally {
                if (channel != null) {
                    channel.shutdown();
                }
            }
        });

        // Event handler for requesting optimized distribution plan
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

    private JPanel getDistributionOptimizerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Distribution Optimizer"));

        // --- Region Input + Plan Button ---
        JPanel regionInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        regionInputPanel.add(new JLabel("Region:"));
        optimizerRegionField = new JTextField(10);
        regionInputPanel.add(optimizerRegionField);

        JButton getPlanButton = new JButton("Get Distribution Plan");
        regionInputPanel.add(getPlanButton);
        panel.add(regionInputPanel);

        // --- Result Area for Distribution Plan ---
        JTextArea distributionResultArea = new JTextArea(8, 40);
        distributionResultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(distributionResultArea);
        panel.add(scrollPane);

        // --- Optimization Summary Panel ---
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton summaryButton = new JButton("Run Full Optimization");
        summaryPanel.add(summaryButton);

        optimizationSummaryField = new JTextField(30);
        optimizationSummaryField.setEditable(false);
        summaryPanel.add(optimizationSummaryField);
        panel.add(summaryPanel);

        // --- Add Delivery Panel ---
        JPanel addDeliveryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addDeliveryPanel.setBorder(BorderFactory.createTitledBorder("Add New Delivery"));

        JTextField vehicleField = new JTextField(8);
        JTextField routeField = new JTextField(12);
        JTextField quantityField = new JTextField(5);

        addDeliveryPanel.add(new JLabel("Vehicle:"));
        addDeliveryPanel.add(vehicleField);
        addDeliveryPanel.add(new JLabel("Route:"));
        addDeliveryPanel.add(routeField);
        addDeliveryPanel.add(new JLabel("Quantity:"));
        addDeliveryPanel.add(quantityField);

        JButton addDeliveryButton = new JButton("Add Delivery");
        addDeliveryPanel.add(addDeliveryButton);

        panel.add(addDeliveryPanel);

        // --- Add Delivery Status Display ---
        JTextField addStatusField = new JTextField(30);
        addStatusField.setEditable(false);
        panel.add(addStatusField);

        // --- Event Handlers ---
        getPlanButton.addActionListener(e -> {
            String region = optimizerRegionField.getText().trim();
            if (region.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter a region.");
                return;
            }

            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder.forAddress("localhost", 50053)
                        .usePlaintext()
                        .build();

                DistributionOptimizerServiceGrpc.DistributionOptimizerServiceBlockingStub stub
                        = DistributionOptimizerServiceGrpc.newBlockingStub(channel);

                RegionRequest request = RegionRequest.newBuilder().setRegion(region).build();
                Iterator<DeliveryInstruction> instructions = stub.getDistributionPlan(request);

                StringBuilder result = new StringBuilder();
                boolean hasResults = false;

                while (instructions.hasNext()) {
                    DeliveryInstruction instruction = instructions.next();
                    result.append("Vehicle: ").append(instruction.getVehicle())
                            .append(", Route: ").append(instruction.getRoute())
                            .append(", Quantity: ").append(instruction.getQuantity())
                            .append("\n");
                    hasResults = true;
                }

                if (!hasResults) {
                    result.append("No routes found for region: ").append(region);
                }

                distributionResultArea.setText(result.toString());

            } catch (Exception ex) {
                ex.printStackTrace();
                distributionResultArea.setText("Error: " + ex.getMessage());
            } finally {
                if (channel != null) {
                    channel.shutdown();
                }
            }
        });

        summaryButton.addActionListener(e -> {
            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder.forAddress("localhost", 50053)
                        .usePlaintext()
                        .build();

                DistributionOptimizerServiceGrpc.DistributionOptimizerServiceBlockingStub stub
                        = DistributionOptimizerServiceGrpc.newBlockingStub(channel);

                OptimizationSummary summary = stub.optimizeAll(Empty.newBuilder().build());

                optimizationSummaryField.setText("Total Vehicles: " + summary.getTotalVehicles()
                        + ", Total Deliveries: " + summary.getTotalDeliveries());

            } catch (Exception ex) {
                ex.printStackTrace();
                optimizationSummaryField.setText("Error: " + ex.getMessage());
            } finally {
                if (channel != null) {
                    channel.shutdown();
                }
            }
        });

        addDeliveryButton.addActionListener(e -> {
            String vehicle = vehicleField.getText().trim();
            String route = routeField.getText().trim();
            String quantityStr = quantityField.getText().trim();

            if (vehicle.isEmpty() || route.isEmpty() || quantityStr.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill in all delivery fields.");
                return;
            }

            int quantity;
            try {
                quantity = Integer.parseInt(quantityStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Quantity must be a valid number.");
                return;
            }

            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder.forAddress("localhost", 50053)
                        .usePlaintext()
                        .build();

                DistributionOptimizerServiceGrpc.DistributionOptimizerServiceBlockingStub stub
                        = DistributionOptimizerServiceGrpc.newBlockingStub(channel);

                AddDeliveryRequest request = AddDeliveryRequest.newBuilder()
                        .setVehicle(vehicle)
                        .setRoute(route)
                        .setQuantity(quantity)
                        .build();

                AddDeliveryResponse response = stub.addDelivery(request);
                addStatusField.setText("Status: " + response.getStatus());

                // Optional: clear fields after adding
                vehicleField.setText("");
                routeField.setText("");
                quantityField.setText("");

            } catch (Exception ex) {
                ex.printStackTrace();
                addStatusField.setText("Error: " + ex.getMessage());
            } finally {
                if (channel != null) {
                    channel.shutdown();
                }
            }
        });

        return panel;
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

    private void addToInventory() {
        String foodName = inputField.getText().trim();
        String quantityText = quantityField.getText().trim();

        if (foodName.isEmpty() || quantityText.isEmpty()) {
            responseField.setText("Please enter both food item and quantity.");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            responseField.setText("Quantity must be a valid number.");
            return;
        }

        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                    .usePlaintext()
                    .build();

            FoodInventoryServiceGrpc.FoodInventoryServiceBlockingStub stub
                    = FoodInventoryServiceGrpc.newBlockingStub(channel);

            AddItemRequest request = AddItemRequest.newBuilder()
                    .setName(foodName)
                    .setQuantity(quantity)
                    .build();

            AddItemResponse addResponse = stub.addItem(request);
            responseField.setText(addResponse.getMessage());

        } catch (Exception ex) {
            ex.printStackTrace();
            responseField.setText("Error: " + ex.getMessage());
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
    }

    private void viewInventory() {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                    .usePlaintext()
                    .build();

            FoodInventoryServiceGrpc.FoodInventoryServiceBlockingStub stub
                    = FoodInventoryServiceGrpc.newBlockingStub(channel);

            InventoryList inventory = stub.getInventory(Empty.newBuilder().build());

            StringBuilder result = new StringBuilder("Current Inventory:\n");
            for (InventoryItem item : inventory.getItemsList()) {
                result.append(item.getName()).append(": ").append(item.getQuantity()).append("\n");
            }

            // Display in scrollable popup
            JTextArea textArea = new JTextArea(result.toString(), 10, 30);
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            JOptionPane.showMessageDialog(null, scrollPane, "Food Inventory", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error fetching inventory: " + ex.getMessage());
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
    }

}
