package com.mycompany.smartzerohungersystem;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.net.InetAddress;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import com.mycompany.smartzerohungersystem.client.ClientGUI;
import com.mycompany.smartzerohungersystem.foodinventory.FoodInventoryImpl;
import com.mycompany.smartzerohungersystem.demandtracker.DemandTrackerImpl;
import com.mycompany.smartzerohungersystem.distributionoptimizer.DistributionOptimizerImpl;


/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws Exception {
    Server food = ServerBuilder.forPort(50051)
      .addService(new FoodInventoryImpl()).build().start();
    registerMdns("FoodInventory", 50051);
    
       System.out.println("Starting GUI...");
    ClientGUI gui = new ClientGUI();
    gui.build();

//    Server demand = ServerBuilder.forPort(50052)
//      .addService(new DemandTrackerImpl()).build().start();
//    registerMdns("DemandTracker", 50052);

//    Server distro = ServerBuilder.forPort(50053)
//      .addService(new DistributionOptimizerImpl()).build().start();
//    registerMdns("DistributionOptimizer", 50053);

    System.out.println("All services up. Ctrl+C to stop");
    Thread.currentThread().join();  // keep running
  }

  private static void registerMdns(String name, int port) throws IOException {
    JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
    ServiceInfo info = ServiceInfo.create("_grpc._tcp.local.", name, port, "");
    jmdns.registerService(info);
    System.out.println("Registered " + name + " via jmDNS");
  }
}

