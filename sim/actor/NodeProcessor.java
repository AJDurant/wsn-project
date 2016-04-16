package ajdurant.wsn.actor;

import java.util.ArrayList;
import java.util.HashMap;

import ajdurant.wsn.lib.ModelData;
import ptolemy.actor.NoTokenException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.data.UnionToken;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.data.type.UnionType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

public class NodeProcessor extends TypedAtomicActor {

    public NodeProcessor(CompositeEntity container, String name)
        throws NameDuplicationException, IllegalActionException
    {
        super(container, name);
        
     // Inputs
        fromComm = new TypedIOPort(this, "fromComm", true, false);
        fromComm.setTypeEquals(typeRecordComm);
        new SingletonParameter(fromComm, "_showName").setExpression("true");
        
        fromSens = new TypedIOPort(this, "fromSens", true, false);
        new SingletonParameter(fromSens, "_showName").setExpression("true");
        
        triggerHeartBeat = new TypedIOPort(this, "triggerHeartBeat", true, false);
        new SingletonParameter(triggerHeartBeat, "_showName").setExpression("true");
        
        triggerNeighbourCheck = new TypedIOPort(this, "triggerNeighbourCheck", true, false);
        new SingletonParameter(triggerNeighbourCheck, "_showName").setExpression("true");
        
        triggerSensorRead = new TypedIOPort(this, "triggerSensorRead", true, false);
        new SingletonParameter(triggerSensorRead, "_showName").setExpression("true");
        
     // Outputs
        consumption = new TypedIOPort(this, "consumption", false, true);
        consumption.setTypeEquals(BaseType.DOUBLE);
        new SingletonParameter(consumption, "_showName").setExpression("true");
        
        toComm = new TypedIOPort(this, "toComm", false, true);
        toComm.setTypeEquals(typeRecordComm);
        new SingletonParameter(toComm, "_showName").setExpression("true");
        
        toSens = new TypedIOPort(this, "toSens", false, true);
        toSens.setTypeEquals(BaseType.BOOLEAN);
        new SingletonParameter(toSens, "_showName").setExpression("true");
        
        toMotion = new TypedIOPort(this, "toMotion", false, true);
        toMotion.setTypeEquals(typeLocation);
        new SingletonParameter(toMotion, "_showName").setExpression("true");
        
     // Set Type constraints
        toComm.setTypeSameAs(fromComm);
        
        modelData = new ModelData(container, "");
        
     // Setup node constants
        emptyType = (IntToken) modelData.getVariable("emptyType");
        heartbeatType = (IntToken) modelData.getVariable("heartbeatType");
        dataType = (IntToken) modelData.getVariable("dataType");
    }
    
 // Inputs
    public TypedIOPort fromComm;
    public TypedIOPort fromSens;
    public TypedIOPort triggerHeartBeat;
    public TypedIOPort triggerNeighbourCheck;
    public TypedIOPort triggerSensorRead;
 // Outputs
    public TypedIOPort consumption;
    public TypedIOPort toComm;
    public TypedIOPort toSens;
    public TypedIOPort toMotion;
    
 // Node Constants
    private final IntToken emptyType;
    private final IntToken heartbeatType;
    private final IntToken dataType;
    private double heartbeatCheckPeriod;
    
 // Internals
    private IntToken nodeID;
    private HashMap<Integer, Token> neighbours;
    private IntToken heartbeatCount;
    private boolean alreadyMoved;
    
    private ModelData modelData;
    
    
    
    // Should be local to function but silly Java Closure rules prevent them working.
    private int minRank;
    private boolean inRange;
    private ArrayList<ArrayToken> locationSenders;
    
    /**
     * Setup internal variables and state.
     */
    @Override
    public void initialize() throws IllegalActionException {
    	super.initialize();
    	
    	nodeID = (IntToken) modelData.getVariable("nodeID");
    	
    	neighbours = new HashMap<Integer, Token>();
        heartbeatCount = new IntToken(0);
        heartbeatCheckPeriod = ((DoubleToken) modelData.getVariable("heartbeatCheckPeriod")).doubleValue();
        
        alreadyMoved = false;
    }
    
    /** Clone the actor into the specified workspace.
     *  @param workspace The workspace for the new object.
     *  @return A new actor.
     *  @exception CloneNotSupportedException If a derived class contains
     *   an attribute that cannot be cloned.
     */
    @Override
    public Object clone(Workspace workspace) throws CloneNotSupportedException {
        NodeProcessor newObject = (NodeProcessor) super.clone(workspace);

        // Set the type constraints.
        newObject.toComm.setTypeSameAs(newObject.fromComm);

        return newObject;
    }
    
    @Override
    public void fire() throws IllegalActionException
    {
        super.fire();
        
        if (fromComm.getWidth() > 0 && fromComm.hasToken(0)) {
            receiveCommunication();
        }
        if (fromSens.getWidth() > 0 && fromSens.hasToken(0)) {
            receiveSensor();
        }
        if (triggerHeartBeat.getWidth() > 0 && triggerHeartBeat.hasToken(0)) {
            modelData.clearToken(triggerHeartBeat);
            heartbeat();
        }
        if (triggerNeighbourCheck.getWidth() > 0 && triggerNeighbourCheck.hasToken(0)) {
            modelData.clearToken(triggerNeighbourCheck);
            neighbourCheck();
        }
        if (triggerSensorRead.getWidth() > 0 && triggerSensorRead.hasToken(0)) {
            modelData.clearToken(triggerSensorRead);
            readSensor();
        }
        
    }
    
    protected void receiveCommunication() throws IllegalActionException {
    	if (fromComm.getWidth() > 0 && fromComm.hasToken(0)) {
            RecordToken messageToken = (RecordToken) fromComm.get(0);
            
            IntToken messageType = (IntToken) messageToken.get("messageType");
            UnionToken messageData = (UnionToken) messageToken.get("messageData");
            
            if (messageType.equals(heartbeatType)) {
            	RecordToken heartbeatMessage = (RecordToken) messageData.value();
            	parentUpdate(heartbeatMessage);
            	neighbourUpdate(heartbeatMessage);
            	
            } else if (messageType.equals(dataType)) {
            	// Forward message
            	sendMessage(messageType, messageData);
            }
        }
    	consumePower();
    }
    
    /**
     * Updates the parent node if it has a shorter route the the root node.
     * 
     * @param messageData
     * @throws IllegalActionException
     */
    protected void parentUpdate(RecordToken heartbeatMessage) throws IllegalActionException {
    	int nodeID = ((IntToken) heartbeatMessage.get("nodeID")).intValue();
    	int hopCount = ((IntToken) heartbeatMessage.get("hopCount")).intValue();
    	
    	int nodeHopCount = ((IntToken) modelData.getVariable("hopCount")).intValue();
    	
    	if (hopCount < nodeHopCount) {
    		IntToken parentToken = new IntToken(nodeID);
    		IntToken newHopCount = new IntToken(hopCount + 1);
    		
    		modelData.setVariable("parentNode", parentToken);
    		modelData.setVariable("hopCount", newHopCount);
    	}	
    }
    
    /**
     * Adds a neighbour to the HashMap when receiving a HeartBeat.
     * 
     * @param heartbeatMessage
     * @throws IllegalActionException
     */
    protected void neighbourUpdate(RecordToken heartbeatMessage) throws IllegalActionException {
    	Integer nodeID = new Integer(((IntToken) heartbeatMessage.get("nodeID")).intValue());
    	
    	DoubleToken updateTime = new DoubleToken(getDirector().getModelTime().getDoubleValue());
    	BooleanToken alive = new BooleanToken(true);
    	RecordToken neighbourState = new RecordToken(labelsNeighbourState, new Token[] {updateTime, alive});
    	
    	RecordToken neighbourData = RecordToken.merge(heartbeatMessage, neighbourState);
    	
    	neighbours.put(nodeID, neighbourData);
    }
    
    /**
     * Send message to the communications module.
     * 
     * @param messageType
     * @param messageData
     */
    protected void sendMessage(IntToken messageType, UnionToken messageData) {
    	try {
			RecordToken message = new RecordToken(labelsComm, new Token[] {messageType, messageData});
			toComm.send(0, message);
		} catch (IllegalActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    /**
     * Send sensor data onto the network.
     * 
     * @throws IllegalActionException
     */
    protected void receiveSensor() throws IllegalActionException {
    	if (fromSens.getWidth() > 0 && fromSens.hasToken(0)) {
    		
    		DoubleToken sensorValue = (DoubleToken) fromSens.get(0);
    		DoubleToken sensorTime = new DoubleToken(getDirector().getModelTime().getDoubleValue());
    		
    		RecordToken dataMessageRecord = new RecordToken(labelsDataMessage, new Token[] {nodeID, sensorValue, sensorTime});
    		UnionToken dataMessageUnion = new UnionToken("DataMessage", dataMessageRecord);
    		
    		sendMessage(dataType, dataMessageUnion);
        }
    	consumePower();
    }
    
    protected void heartbeat() throws IllegalActionException {
    	
    	Token[] heartbeatTokens = {
    		heartbeatCount,
    		(IntToken) modelData.getVariable("hopCount"),
    		(ArrayToken) modelData.getVariable("targetLocation"),
    		(BooleanToken) modelData.getVariable("inMotion"),
    		nodeID
    	};
    	
    	RecordToken heartbeatRecord = new RecordToken(labelsHeartbeat, heartbeatTokens);
    	UnionToken heartbeatUnion = new UnionToken("Heartbeat", heartbeatRecord);
    	
    	sendMessage(heartbeatType, heartbeatUnion);
    	
    	heartbeatCount.add(IntToken.ONE);
    	consumePower();
    }
    
    /**
     * Request a reading from the sensor module.
     * 
     * @throws IllegalActionException
     */
    protected void readSensor() throws IllegalActionException {
    	toSens.send(0, new BooleanToken(true));
    	consumePower();
    }
    
    protected void neighbourCheck() throws IllegalActionException {
    	
    	double currentTime = getDirector().getModelTime().getDoubleValue();
    	
    	neighbours.forEach((node, nodeData) -> {
            RecordToken nodeRecord = (RecordToken) nodeData;
            
            double nodeUpdateTime = ((DoubleToken) nodeRecord.get("updateTime")).doubleValue();
            boolean nodeAlive = ((BooleanToken) nodeRecord.get("alive")).booleanValue();
            boolean nodeInMotion = ((BooleanToken) nodeRecord.get("motion")).booleanValue();
            ArrayToken nodeLocation = (ArrayToken) nodeRecord.get("location");
    		
            if ((currentTime >= nodeUpdateTime + heartbeatCheckPeriod) && nodeAlive) {
                // Higher ranked node has lost connectivity.

                if (nodeInMotion) {
                    // Node has moved away
                    if (alreadyMoved) {
                        return;
                    }
                    
                    ArrayToken newPosition = computeNewPosition();
                    ArrayToken currentPosition = (ArrayToken) modelData.getVariable("currentLocation");
                    
                    if (!currentPosition.equals(newPosition)) {
                        try {
                            modelData.setVariable("inMotion", new BooleanToken(true));
                            toMotion.send(0, newPosition);
                        } catch (IllegalActionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    
                } else {
                    // Node has probably died
                    try {
                        nodeData = setNeighbourLive(nodeRecord, false);
                        modelData.setVariable("inMotion", new BooleanToken(true));
                        toMotion.send(0, computePositionFromDeadNode(nodeLocation));
                    } catch (IllegalActionException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                alreadyMoved = true;
    		}
    	});
    }
    
    /**
     * Send consumption to power module.
     * 
     * @throws IllegalActionException
     */
    protected void consumePower() throws IllegalActionException {
    	DoubleToken consumptionRate = (DoubleToken) modelData.getVariable("processorPowerRate");
    	consumption.send(0, consumptionRate);
    }
    
    private RecordToken setNeighbourLive(RecordToken nodeData, boolean alive) throws IllegalActionException {
        RecordToken aliveToken;
        aliveToken = new RecordToken(new String[] { "alive" }, new Token[] { new BooleanToken(false) });
        return RecordToken.merge(aliveToken, (RecordToken) nodeData);
    }
    
    private ArrayToken computePositionFromDeadNode(ArrayToken nodeLocation) {
        ArrayToken currentLocation = (ArrayToken) modelData.getVariable("currentLocation");
        
        double coordX = ((DoubleToken) currentLocation.getElement(0)).doubleValue();
        double coordY = ((DoubleToken) currentLocation.getElement(1)).doubleValue();
        
        double nodeX = ((DoubleToken) nodeLocation.getElement(0)).doubleValue();
        double nodeY = ((DoubleToken) nodeLocation.getElement(1)).doubleValue();
        
        double distance = Math.sqrt( Math.pow((nodeX - coordX), 2) + Math.pow((nodeY - coordY), 2) );
        double radius = ((DoubleToken) modelData.getVariable("txRange")).doubleValue();
        double ratio = (radius / 2) / distance;
        
        DoubleToken newX = new DoubleToken((1-ratio) * nodeX + ratio * coordX);
        DoubleToken newY = new DoubleToken((1-ratio) * nodeY + ratio * coordY);
        
        try {
            ArrayToken newLocation = new ArrayToken(new Token[] {newX, newY});
            return newLocation;
        } catch (IllegalActionException e) {
            e.printStackTrace();
        }
        
        
        return null;
    }

    private ArrayToken computeNewPosition() {
        ArrayToken currentLocation = (ArrayToken) modelData.getVariable("currentLocation");
        
        // Number of neighbours in motion with lowest rank.
        inRange = true;
        minRank = Integer.MAX_VALUE;
        locationSenders = new ArrayList<ArrayToken>();
        
        neighbours.forEach((node, nodeData) -> {
            RecordToken nodeRecord = (RecordToken) nodeData;
            
            int nodeRank = ((IntToken) nodeRecord.get("nodeID")).intValue();
            boolean nodeInMotion = ((BooleanToken) nodeRecord.get("motion")).booleanValue();
            ArrayToken nodeLocation = (ArrayToken) nodeRecord.get("location");
            
            if (nodeInMotion) {
                if (nodeRank < minRank) {
                    // new lowest rank, reset
                    inRange = true;
                    minRank = nodeRank;
                    locationSenders = new ArrayList<ArrayToken>();
                }
                
                if (nodeRank <= minRank) {
                    if (!checkNodeInRange(nodeLocation)) {
                        inRange = false;
                    }
                    locationSenders.add(nodeLocation);
                }
            }
        });
        
        if (inRange) {
            // Node stays connected with all neighbours with least rank.
            return currentLocation;
        }
        
        if (locationSenders.size() == 1) {
            // Return a point r units away from neighbour, on direct path to neighbour.
            return getOneNodeIntersection(currentLocation, locationSenders.get(0));
        } else if (locationSenders.size() == 2) {
            // Return closest point between two intersection points.
            return getTwoNodeIntersection(currentLocation, locationSenders);
        } else if (locationSenders.size() >= 3) {
            // Return the closest point among intersection points which is located inside all other circles.
            return getMultipleNodeIntersection(currentLocation, locationSenders);
        }

        return null;
    }
    
    private boolean checkNodeInRange(ArrayToken nodeLocation) {
        ArrayToken currentLocation = (ArrayToken) modelData.getVariable("currentLocation");
        double coordX = ((DoubleToken) currentLocation.getElement(0)).doubleValue();
        double coordY = ((DoubleToken) currentLocation.getElement(1)).doubleValue();
        
        double nodeX = ((DoubleToken) nodeLocation.getElement(0)).doubleValue();
        double nodeY = ((DoubleToken) nodeLocation.getElement(1)).doubleValue();
        
        double radius = ((DoubleToken) modelData.getVariable("txRange")).doubleValue();
        
        return checkPointInRadius(coordX, coordY, nodeX, nodeY, radius);
    }
    
    private boolean checkPointInRadius(double Xa, double Ya, double Xb, double Yb, double radius) {
        double distance2 = Math.pow((Xb - Xa), 2) + Math.pow((Yb - Ya), 2);
        double radius2 = Math.pow(radius, 2);
        
        if (distance2 <= radius2) {
            return true;
        } else {
            return false;
        }
    }
    
    private ArrayToken getOneNodeIntersection(ArrayToken currentLocation, ArrayToken nodeLocation) {
        double coordX = ((DoubleToken) currentLocation.getElement(0)).doubleValue();
        double coordY = ((DoubleToken) currentLocation.getElement(1)).doubleValue();
        
        double nodeX = ((DoubleToken) nodeLocation.getElement(0)).doubleValue();
        double nodeY = ((DoubleToken) nodeLocation.getElement(1)).doubleValue();
        
        double distance = Math.sqrt( Math.pow((nodeX - coordX), 2) + Math.pow((nodeY - coordY), 2) );
        double radius = ((DoubleToken) modelData.getVariable("txRange")).doubleValue();
        double ratio = radius / distance;
        
        DoubleToken newX = new DoubleToken((1-ratio) * nodeX + ratio * coordX);
        DoubleToken newY = new DoubleToken((1-ratio) * nodeY + ratio * coordY);
        
        try {
            ArrayToken newLocation = new ArrayToken(new Token[] {newX, newY});
            return newLocation;
        } catch (IllegalActionException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    private ArrayToken getTwoNodeIntersection(ArrayToken currentLocation, ArrayList<ArrayToken> locations) {
        double coordX = ((DoubleToken) currentLocation.getElement(0)).doubleValue();
        double coordY = ((DoubleToken) currentLocation.getElement(1)).doubleValue();
        
        ArrayList<double[]> intersections = getIntersectionPoints(locations);
        
        double[] closestPoint = getClosestPoint(coordX, coordY, intersections);
        
        DoubleToken newX = new DoubleToken(closestPoint[0]);
        DoubleToken newY = new DoubleToken(closestPoint[1]);
        
        try {
            ArrayToken newLocation = new ArrayToken(new Token[] {newX, newY});
            return newLocation;
        } catch (IllegalActionException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    private ArrayToken getMultipleNodeIntersection(ArrayToken currentLocation, ArrayList<ArrayToken> locations) {
        double coordX = ((DoubleToken) currentLocation.getElement(0)).doubleValue();
        double coordY = ((DoubleToken) currentLocation.getElement(1)).doubleValue();
        
        double radius = ((DoubleToken) modelData.getVariable("txRange")).doubleValue();
        ArrayList<double[]> intersections = getIntersectionPoints(locations);
        
        ArrayList<double[]> candidateLocations = new ArrayList<double[]>();
        
        for (int i = 0; i < intersections.size(); i++) {
            double[] aLocation = intersections.get(i);
            double Xa = aLocation[0];
            double Ya = aLocation[1];
            
            boolean pointInRadii = true;
            
            for (int j = 0; j < locations.size(); j++) {
                
                double[] bLocation = intersections.get(j);
                double Xb = bLocation[0];
                double Yb = bLocation[1];
                
                if (!checkPointInRadius(Xa, Ya, Xb, Yb, radius)) {
                    pointInRadii = false;
                }
            }
            
            if (pointInRadii) {
                candidateLocations.add(aLocation);
            }
        }
        
        double[] closestPoint = getClosestPoint(coordX, coordY, candidateLocations);
        
        DoubleToken newX = new DoubleToken(closestPoint[0]);
        DoubleToken newY = new DoubleToken(closestPoint[1]);
        
        try {
            ArrayToken newLocation = new ArrayToken(new Token[] {newX, newY});
            return newLocation;
        } catch (IllegalActionException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    private ArrayList<double[]> getIntersectionPoints(ArrayList<ArrayToken> locations) {
        
        double radius = ((DoubleToken) modelData.getVariable("txRange")).doubleValue();
        ArrayList<double[]> intersections = new ArrayList<double[]>();
        
        for (int i = 0; i < locations.size(); i++) {
            ArrayToken aLocation = locations.get(i);
            double Xa = ((DoubleToken) aLocation.getElement(0)).doubleValue();
            double Ya = ((DoubleToken) aLocation.getElement(1)).doubleValue();
            
            for (int j = 0; j < locations.size(); j++) {
                if (j == i){
                    continue;
                }
                
                ArrayToken bLocation = locations.get(j);

                double Xb = ((DoubleToken) bLocation.getElement(0)).doubleValue();
                double Yb = ((DoubleToken) bLocation.getElement(1)).doubleValue();
                
                double distance2 = Math.pow((Xa - Xb), 2) + Math.pow((Ya - Yb), 2);
                
                double K = 0.25 * Math.sqrt((Math.pow(radius + radius, 2) - distance2) * (distance2));
                // radius is equal so some terms cancel
                double X1 = 0.5 * (Xb + Xa) + 2 * (Yb - Ya) * K / distance2;
                double X2 = 0.5 * (Xb + Xa) - 2 * (Yb - Ya) * K / distance2;
                double Y1 = 0.5 * (Yb + Ya) - 2 * (Xb - Xa) * K / distance2;
                double Y2 = 0.5 * (Yb + Ya) + 2 * (Xb - Xa) * K / distance2;
                
                intersections.add(new double[] {X1, Y1});
                intersections.add(new double[] {X2, Y2});
            }
        }
        
        return intersections;
    }
    
    private double[] getClosestPoint(double coordX, double coordY, ArrayList<double[]> points) {
        
        double minDistance = Double.MAX_VALUE;
        int chosenPoint = 0;
        
        for (int i = 0; i < points.size(); i++) {
            double distance = Math.pow((points.get(i)[0] - coordX), 2) + Math.pow((points.get(i)[1] - coordY), 2);
            if (distance < minDistance) {
                minDistance = distance;
                chosenPoint = i;
            }
        }
        
        return points.get(chosenPoint);
    }
    
 // Types
    private static String[] labelsDataMessage = {
        "nodeID",
        "sensorData",
        "sensorTime"
    };
    private static Type[] typesDataMessage = {
        BaseType.INT,
        BaseType.DOUBLE,
        BaseType.DOUBLE
    };
    private static RecordType typeDataMessage = new RecordType(labelsDataMessage, typesDataMessage);
    
    private static Type locationType = new ArrayType(BaseType.DOUBLE, 2);
    private static String[] labelsHeartbeat = {
        "count",
        "hopCount",
        "location",
        "motion",
        "nodeID"
    };
    private static Type[] typesHeartbeat = {
        BaseType.INT,
        BaseType.INT,
        locationType,
        BaseType.BOOLEAN,
        BaseType.INT
    };
    private static RecordType typeRecordHeartbeat = new RecordType(labelsHeartbeat, typesHeartbeat);
    
    private static String[] labelsCommUnion = {
        "DataMessage",
        "Heartbeat"
    };
    private static Type[] typesCommUnion = {
        typeDataMessage,
        typeRecordHeartbeat
    };
    
    private static UnionType typeUnionCommUnion = new UnionType(labelsCommUnion, typesCommUnion);
    
    private static String[] labelsComm = {
        "messageType",
        "messageData"
    };
    private static Type[] typesComm = {
        BaseType.INT,
        typeUnionCommUnion
    };
    private static RecordType typeRecordComm = new RecordType(labelsComm, typesComm);
    
    private static String[] labelsNeighbourState = {
    	"updateTime",
    	"alive"
    };
    private static Type[] typesNeighbourState = {
        BaseType.DOUBLE,
        BaseType.BOOLEAN
    };
    private static RecordType typeRecordNeighbourState = new RecordType(labelsNeighbourState, typesNeighbourState);
    
    private static ArrayType typeLocation = new ArrayType(BaseType.DOUBLE, 2);
}


