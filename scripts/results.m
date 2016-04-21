addpath('D:/Octave/Toolboxes/jsonlab-1.2/');

powerFiles = dir('*PowerLogger.json');
motionFiles = dir('*MotionLogger.json');
txFiles = dir('*TXLogger.json');

powerData = {};
motionData = {};
txData = {};

for file = powerFiles'
    data = loadjson(file.name);
    powerData = [powerData, {data}];
end;

for file = motionFiles'
    data = loadjson(file.name);
    motionData = [motionData, {data}];
end;

for file = txFiles'
    data = loadjson(file.name);
    txData = [txData, {data}];
end;

% Calculate power stats
powerUses = [];
powerAvgs = [];

for node = powerData
    powerUse = node{1}{1}.data - node{1}{end}.data;
    powerAvg = powerUse / node{1}{end}.time;
    powerUses = [powerUses, powerUse];
    powerAvgs = [powerAvgs, powerAvg];
end;

totalPowerUse = sum(powerUses);
AvgPowerUse = mean(powerAvgs);

% Calculate motion stats
distances = [];

for node = motionData
    distance = node{1}{end}.data.totalDistanceTraveled;
    distances = [distances, distance];
end;

totalDistanceTraveled = sum(distances);
averageDistanceTraveled = mean(distances);

% Calculate TX stats
messagesSent =  [];
hopAvgs = [];
neighbourAvgs = [];

for node = txData
    messages = columns(node{1});
    messagesSent = [messagesSent, messages];

    hopcounts = [];
    neighbours = [];
    for element = node{1}
        if (isfield(element{1}.data.payload.messageData, 'Heartbeat'))
            hop = element{1}.data.payload.messageData.Heartbeat.hopCount;
            if (hop < 500)
                hopcounts = [hopcounts, hop];
            end

            neighbour = element{1}.data.payload.messageData.Heartbeat.aliveNeighbours;
            neighbours = [neighbours, neighbour];
        end
    end

    hopAvgs = [hopAvgs, mean(hopcounts)];
    neighbourAvgs = [neighbourAvgs, mean(neighbours)];
end;

totalMessagesSent = sum(messagesSent);
averageMessagesSent = mean(messagesSent);

averageHopCount = mean(hopAvgs);
averageNeighbours = mean(neighbourAvgs);

bar([messagesSent', powerAvgs']);
figure;
bar([hopAvgs', neighbourAvgs']);
