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
   
 
       System.out.println("Starting GUI...");
 ClientGUI gui = new ClientGUI();
gui.build();

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

