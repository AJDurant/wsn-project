package ajdurant.wsn.actor;

import java.util.HashMap;

import ajdurant.wsn.lib.WSNActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.data.UnionToken;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

public abstract class NodeProcessor extends WSNActor {

    // Inputs
    public TypedIOPort fromComm;
    public TypedIOPort fromSens;
    public TypedIOPort triggerHeartBeat;
    public TypedIOPort triggerNeighbourCheck;
    public TypedIOPort triggerSensorRead;
    // Outputs
    public TypedIOPort toComm;
    public TypedIOPort toSens;
    public TypedIOPort toMotion;
    
    // Constants
    protected final IntToken emptyType;
    protected final IntToken heartbeatType;
    protected final IntToken dataType;
    protected double heartbeatCheckPeriod;
    
    // Internals
    private IntToken nodeID;
    private IntToken heartbeatCount;
    protected HashMap<Integer, Token> neighbours;

    /**
     * Handler for when a periodic neighbour check is triggered.
     * 
     * This function implements the recovery algorithm.
     * 
     * @throws IllegalActionException
     */
    protected abstract void neighbourCheck() throws IllegalActionException;

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
        
        // Setup node constants
        emptyType = (IntToken) getVariable("emptyType");
        heartbeatType = (IntToken) getVariable("heartbeatType");
        dataType = (IntToken) getVariable("dataType");
    }
    
    /**
     * Setup internal variables and state.
     */
    @Override
    public void initialize() throws IllegalActionException {
    	super.initialize();
    	
    	nodeID = (IntToken) getVariable("nodeID");
    	
    	neighbours = new HashMap<Integer, Token>();
        heartbeatCount = new IntToken(0);
        heartbeatCheckPeriod = ((DoubleToken) getVariable("heartbeatCheckPeriod")).doubleValue();

    }

    /** Clone the actor into the specified workspace.
     *  @param workspace The workspace for the new object.
     *  @return A new actor.
     *  @exception CloneNotSupportedException If a derived class contains
     *   an attribute that cannot be cloned.
     */
    @Override
    public Object clone(Workspace workspace) throws CloneNotSupportedException {
        NodeProcessorRIM newObject = (NodeProcessorRIM) super.clone(workspace);
    
        // Set the type constraints.
        newObject.toComm.setTypeSameAs(newObject.fromComm);
    
        return newObject;
    }

    @Override
    public void fire() throws IllegalActionException {
        super.fire();
        
        if (fromComm.getWidth() > 0 && fromComm.hasToken(0)) {
            receiveCommunication();
        }
        if (fromSens.getWidth() > 0 && fromSens.hasToken(0)) {
            receiveSensor();
        }
        if (triggerHeartBeat.getWidth() > 0 && triggerHeartBeat.hasToken(0)) {
            clearToken(triggerHeartBeat);
            heartbeat();
        }
        if (triggerNeighbourCheck.getWidth() > 0 && triggerNeighbourCheck.hasToken(0)) {
            clearToken(triggerNeighbourCheck);
            neighbourCheck();
        }
        if (triggerSensorRead.getWidth() > 0 && triggerSensorRead.hasToken(0)) {
            clearToken(triggerSensorRead);
            readSensor();
        }
        
    }

    /**
     * Handler for when message received from Comms.
     * 
     * @throws IllegalActionException
     */
    protected void receiveCommunication() throws IllegalActionException {
    	if (fromComm.getWidth() > 0 && fromComm.hasToken(0)) {
            RecordToken messageToken = (RecordToken) fromComm.get(0);
            
            IntToken messageType = (IntToken) messageToken.get("messageType");
            UnionToken messageData = (UnionToken) messageToken.get("messageData");
            
            if (messageType.equals(heartbeatType)) {
            	RecordToken heartbeatMessage = (RecordToken) messageData.value();
            	handleHeartbeat(heartbeatMessage);
            	
            } else if (messageType.equals(dataType)) {
            	// Forward message
            	sendMessage(messageType, messageData);
            }
        }
    	consumePower();
    }
    
    /**
     * Handler for when heartbeat message is received.
     * 
     * @param heartbeatMessage
     * @throws IllegalActionException
     */
    protected void handleHeartbeat(RecordToken heartbeatMessage) throws IllegalActionException {
        parentUpdate(heartbeatMessage);
        neighbourUpdate(heartbeatMessage);
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
        
        int nodeHopCount = ((IntToken) getVariable("hopCount")).intValue();
        
        if (hopCount < nodeHopCount) {
            IntToken parentToken = new IntToken(nodeID);
            IntToken newHopCount = new IntToken(hopCount + 1);
            
            setVariable("parentNode", parentToken);
            setVariable("hopCount", newHopCount);
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

    /**
     * Send heartbeat onto the network.
     * 
     * @throws IllegalActionException
     */
    protected void heartbeat() throws IllegalActionException {
    	
    	Token[] heartbeatTokens = {
    		heartbeatCount,
    		(IntToken) getVariable("hopCount"),
    		(ArrayToken) getVariable("targetLocation"),
    		(BooleanToken) getVariable("inMotion"),
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

    /**
     * Send consumption to power module.
     * 
     * @throws IllegalActionException
     */
    protected void consumePower() throws IllegalActionException {
    	consumePower("processorPowerRate");
    }

    

}
