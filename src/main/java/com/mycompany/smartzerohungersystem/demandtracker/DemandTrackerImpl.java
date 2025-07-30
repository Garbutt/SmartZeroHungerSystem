
package com.mycompany.smartzerohungersystem.demandtracker;


import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Import your generated protobuf classes
import com.mycompany.smartzerohungersystem.demandtracker.DemandTrackerServiceGrpc;
import com.mycompany.smartzerohungersystem.demandtracker.DemandTracker.HungerReport;
import com.mycompany.smartzerohungersystem.demandtracker.DemandTracker.AggregateReport;
import com.mycompany.smartzerohungersystem.demandtracker.DemandTracker.RegionRequest;
import com.mycompany.smartzerohungersystem.demandtracker.DemandTracker.DemandStats;

/**
 *
 * @author reece
 */
public class DemandTrackerImpl extends DemandTrackerServiceGrpc.DemandTrackerServiceImplBase  {
     // Concurrent map to handle updates from multiple threads (i.e., requests)
    private final Map<String, Integer> demandData = new ConcurrentHashMap<>();

    // Handles streaming hunger reports and returns the aggregate result
    @Override
    public StreamObserver<HungerReport> streamReports(StreamObserver<AggregateReport> responseObserver) {
        return new StreamObserver<HungerReport>() {

            @Override
            public void onNext(HungerReport report) {
                String region = report.getRegion();
                int count = report.getCount();

                demandData.merge(region, count, Integer::sum); // aggregate counts
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace(); // log error
            }

            @Override
            public void onCompleted() {
                int totalReports = demandData.size();
                int totalCount = demandData.values().stream().mapToInt(Integer::intValue).sum();

                AggregateReport response = AggregateReport.newBuilder()
                        .setTotalReports(totalReports)
                        .setTotalCount(totalCount)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }

    // Returns stats for a single region
    @Override
    public void getDemandStats(RegionRequest request, StreamObserver<DemandStats> responseObserver) {
        String region = request.getRegion();
        int total = demandData.getOrDefault(region, 0);

        DemandStats response = DemandStats.newBuilder()
            .setRegion(region)
            .setReportCount(total)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    @Override
public void getAllDemandStats(com.google.protobuf.Empty request, StreamObserver<DemandTracker.AllRegionStats> responseObserver) {
    DemandTracker.AllRegionStats.Builder allStatsBuilder = DemandTracker.AllRegionStats.newBuilder();

    for (Map.Entry<String, Integer> entry : demandData.entrySet()) {
        DemandTracker.DemandStats stats = DemandTracker.DemandStats.newBuilder()
            .setRegion(entry.getKey())
            .setReportCount(entry.getValue())
            .build();
        allStatsBuilder.addStats(stats);
    }

    responseObserver.onNext(allStatsBuilder.build());
    responseObserver.onCompleted();
}

}
