figure;
fig1 = bar([[
singlefail_25node_1_averageDistanceTraveled,
singlefail_25node_1_averageHopCount,
singlefail_25node_1_averageNeighbours,
singlefail_25node_1_averagePowerUse,
singlefail_25node_1_dataMessagePercentage,
singlefail_25node_1_nodesMoved,
],[
singlefail_25node_2_averageDistanceTraveled,
singlefail_25node_2_averageHopCount,
singlefail_25node_2_averageNeighbours,
singlefail_25node_2_averagePowerUse,
singlefail_25node_2_dataMessagePercentage,
singlefail_25node_2_nodesMoved,
],[
singlefail_25node_3_averageDistanceTraveled,
singlefail_25node_3_averageHopCount,
singlefail_25node_3_averageNeighbours,
singlefail_25node_2_averagePowerUse,
singlefail_25node_3_dataMessagePercentage,
singlefail_25node_3_nodesMoved,
]]');

figure;
fig2 = bar([[
singlefail_50node_1_averageDistanceTraveled,
singlefail_50node_1_averageHopCount,
singlefail_50node_1_averageNeighbours,
singlefail_50node_1_averagePowerUse,
singlefail_50node_1_dataMessagePercentage,
singlefail_50node_1_nodesMoved,
],[
singlefail_50node_2_averageDistanceTraveled,
singlefail_50node_2_averageHopCount,
singlefail_50node_2_averageNeighbours,
singlefail_50node_2_averagePowerUse,
singlefail_50node_2_dataMessagePercentage,
singlefail_50node_2_nodesMoved,
],[
singlefail_50node_3_averageDistanceTraveled,
singlefail_50node_3_averageHopCount,
singlefail_50node_3_averageNeighbours,
singlefail_50node_2_averagePowerUse,
singlefail_50node_3_dataMessagePercentage,
singlefail_50node_3_nodesMoved,
]]');

figure;
fig3 = bar([[
singlefail_100node_1_averageDistanceTraveled,
singlefail_100node_1_averageHopCount,
singlefail_100node_1_averageNeighbours,
singlefail_100node_1_averagePowerUse,
singlefail_100node_1_dataMessagePercentage,
singlefail_100node_1_nodesMoved,
],[
singlefail_100node_2_averageDistanceTraveled,
singlefail_100node_2_averageHopCount,
singlefail_100node_2_averageNeighbours,
singlefail_100node_2_averagePowerUse,
singlefail_100node_2_dataMessagePercentage,
singlefail_100node_2_nodesMoved,
]]');

% Other bits
figure;
bar([
singlefail_25node_1_totalDistanceTraveled,
singlefail_25node_2_totalDistanceTraveled,
singlefail_25node_3_totalDistanceTraveled,
]);

figure;
bar([
singlefail_25node_1_totalMessagesSent,
singlefail_25node_2_totalMessagesSent,
singlefail_25node_3_totalMessagesSent,
]);

figure;
bar([
singlefail_25node_1_totalPowerUse,
singlefail_25node_2_totalPowerUse,
singlefail_25node_3_totalPowerUse,
]);

figure;
bar([messagesSent', powerAvgs']);
figure;
bar([hopAvgs', neighbourAvgs']);
figure;
bar([dataMessageCountTX, dataMessageCountRX]);