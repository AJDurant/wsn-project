addpath('D:/Octave/Toolboxes/jsonlab-1.2/');
pkg load statistics;

powerFiles = dir('*PowerLogger.json');
motionFiles = dir('*MotionLogger.json');
txFiles = dir('*TXLogger.json');
commFiles = dir('*fromCommLogger.json');

powerData = {};
motionData = {};
txData = {};
commData = {};

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

for file = commFiles'
    data = loadjson(file.name);
    commData = [commData, {data}];
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
averagePowerUse = mean(powerAvgs);

% Calculate motion stats
distances = [];

for node = motionData
    if (length(node{1}) > 1)
        distance = node{1}{end}.data.totalDistanceTraveled;
    else
        distance = node{1}.data.totalDistanceTraveled;
    end
    distances = [distances, distance];
end;

totalDistanceTraveled = sum(distances);
averageDistanceTraveled = mean(distances);

% Calculate TX stats
messagesSent =  [];
hopAvgs = [];
neighbourAvgs = [];

dataMessageCountTX = 0;

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
        elseif (isfield(element{1}.data.payload.messageData, 'DataMessage'))
            id = element{1}.data.payload.messageData.DataMessage.nodeID;
            src = element{1}.data.src;
            if (id != 1 && id == src)
                dataMessageCountTX++;
            end
        end
    end

    hopAvgs = [hopAvgs, nanmean(hopcounts)];
    neighbourAvgs = [neighbourAvgs, mean(neighbours)];
end;

totalMessagesSent = sum(messagesSent);
averageMessagesSent = mean(messagesSent);

averageHopCount = nanmean(hopAvgs);
averageNeighbours = mean(neighbourAvgs);

% Calculate RX stats
dataMessageCountRX = 0;

for element = commData{1}
    if (isfield(element{1}.data.messageData, 'DataMessage'));
        dataMessageCountRX++;
    end
end

dataMessagePercentage = dataMessageCountRX/dataMessageCountTX * 100;
