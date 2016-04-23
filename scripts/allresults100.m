cd D:/Dev/wsn-project/sim/results;
addpath('D:/Octave/Toolboxes/jsonlab-1.2/');

cd singlefail_100node_1;
results
singlefail_100node_1_averageDistanceTraveled = averageDistanceTraveled;
singlefail_100node_1_averageHopCount = averageHopCount;
singlefail_100node_1_averageNeighbours = averageNeighbours;
singlefail_100node_1_averagePowerUse = averagePowerUse;
singlefail_100node_1_dataMessagePercentage = dataMessagePercentage;
singlefail_100node_1_nodesMoved = length(distances);
singlefail_100node_1_totalDistanceTraveled = totalDistanceTraveled;
singlefail_100node_1_totalMessagesSent = totalMessagesSent;
singlefail_100node_1_totalPowerUse = totalPowerUse;

cd ../singlefail_100node_2;
results
singlefail_100node_2_averageDistanceTraveled = averageDistanceTraveled;
singlefail_100node_2_averageHopCount = averageHopCount;
singlefail_100node_2_averageNeighbours = averageNeighbours;
singlefail_100node_2_averagePowerUse = averagePowerUse;
singlefail_100node_2_dataMessagePercentage = dataMessagePercentage;
singlefail_100node_2_nodesMoved = length(distances);
singlefail_100node_2_totalDistanceTraveled = totalDistanceTraveled;
singlefail_100node_2_totalMessagesSent = totalMessagesSent;
singlefail_100node_2_totalPowerUse = totalPowerUse;
