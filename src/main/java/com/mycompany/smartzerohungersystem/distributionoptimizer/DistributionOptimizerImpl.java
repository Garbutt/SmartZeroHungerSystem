package com.mycompany.smartzerohungersystem.distributionoptimizer;

import io.grpc.stub.StreamObserver;
import com.mycompany.smartzerohungersystem.distributionoptimizer.DistributionOptimizer;
import com.mycompany.smartzerohungersystem.distributionoptimizer.DistributionOptimizer.RegionRequest;
import com.mycompany.smartzerohungersystem.distributionoptimizer.DistributionOptimizer.OptimizationSummary;
import com.mycompany.smartzerohungersystem.distributionoptimizer.DistributionOptimizer.DeliveryInstruction;
import com.mycompany.smartzerohungersystem.distributionoptimizer.DistributionOptimizer.AddDeliveryRequest;
import com.mycompany.smartzerohungersystem.distributionoptimizer.DistributionOptimizer.AddDeliveryResponse;

import java.util.*;

import com.google.protobuf.Empty;

public class DistributionOptimizerImpl extends DistributionOptimizerServiceGrpc.DistributionOptimizerServiceImplBase {

    private final List<DeliveryInstruction> deliveryData = new ArrayList<>();

    @Override
    public void getDistributionPlan(RegionRequest request, StreamObserver<DeliveryInstruction> responseObserver) {
        String region = request.getRegion().toLowerCase();

        boolean matchFound = false;

        for (DeliveryInstruction instruction : deliveryData) {
            if (instruction.getRoute().toLowerCase().contains(region)) {
                responseObserver.onNext(instruction);
                matchFound = true;
            }
        }

        responseObserver.onCompleted();
    }

    @Override
    public void optimizeAll(Empty request, StreamObserver<OptimizationSummary> responseObserver) {
        int totalDeliveries = deliveryData.size();

        Set<String> vehicleSet = new HashSet<>();
        for (DeliveryInstruction instruction : deliveryData) {
            vehicleSet.add(instruction.getVehicle());
        }

        int totalVehicles = vehicleSet.size();

        OptimizationSummary summary = OptimizationSummary.newBuilder()
                .setTotalVehicles(totalVehicles)
                .setTotalDeliveries(totalDeliveries)
                .build();

        responseObserver.onNext(summary);
        responseObserver.onCompleted();
    }

    @Override
    public void addDelivery(AddDeliveryRequest request, StreamObserver<AddDeliveryResponse> responseObserver) {
        DeliveryInstruction instruction = DeliveryInstruction.newBuilder()
                .setVehicle(request.getVehicle())
                .setRoute(request.getRoute())
                .setQuantity(request.getQuantity())
                .build();

        deliveryData.add(instruction);

        AddDeliveryResponse response = AddDeliveryResponse.newBuilder()
                .setStatus("Added successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
