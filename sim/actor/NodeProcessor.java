package ajdurant.wsn.actor;

import java.util.HashMap;

import ptolemy.actor.NoTokenException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.Token;
import ptolemy.data.UnionToken;
import ptolemy.data.RecordToken;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.expr.ModelScope;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.expr.Variable;
import ptolemy.data.type.Type;
import ptolemy.data.type.UnionType;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
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
        new SingletonParameter(toMotion, "_showName").setExpression("true");
        
     // Set Type constraints
        toComm.setTypeSameAs(fromComm);
        
     // Internals
        attribute = new HashMap<String, Attribute>();
        attributeVersion = new HashMap<String, Long>();
        
     // Setup node constants
        emptyType = (IntToken) getVariable("emptyType");
        heartbeatType = (IntToken) getVariable("heartbeatType");
        dataType = (IntToken) getVariable("dataType");
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
    
 // Internals
    private IntToken nodeID;
    private HashMap<Integer, Token> neighbours;
    private HashMap<String, Attribute> attribute;
    private HashMap<String, Long> attributeVersion;
    private IntToken heartbeatCount;
    private double heartbeatCheckPeriod;
    
    /**
     * Setup internal variables and state.
     */
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
        NodeProcessor newObject = (NodeProcessor) super.clone(workspace);

        // Set the type constraints.
        newObject.toComm.setTypeSameAs(newObject.fromComm);
        
        newObject.attribute = new HashMap<String, Attribute>();
        newObject.attributeVersion = new HashMap<String, Long>();

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
    
    protected void heartbeat() throws IllegalActionException {
    	
    	Token[] heartbeatTokens = {
    		heartbeatCount,
    		(IntToken) getVariable("hopCount"),
    		(ArrayToken) getVariable("currentLocation"),
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
    
    protected void neighbourCheck() throws IllegalActionException {
    	
    	double currentTime = getDirector().getModelTime().getDoubleValue();
    	
    	neighbours.forEach((node, nodeData) -> {
    		double nodeTime = ((DoubleToken) ((RecordToken) nodeData).get("updateTime")).doubleValue();
    		
    		if (currentTime >= nodeTime + heartbeatCheckPeriod) {
    			RecordToken aliveToken;
				try {
					aliveToken = new RecordToken(new String[] {"alive"}, new Token[] {new BooleanToken(false)});
					nodeData = RecordToken.merge(aliveToken, (RecordToken) nodeData);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	});
    }
    
    /**
     * Send consumption to power module.
     * 
     * @throws IllegalActionException
     */
    protected void consumePower() throws IllegalActionException {
    	DoubleToken consumptionRate = (DoubleToken) getVariable("processorPowerRate");
    	consumption.send(0, consumptionRate);
    }
    
    /**
     * Discard Token from the given port.
     * @param port Port to remove token from.
     * @throws NoTokenException
     * @throws IllegalActionException
     */
    private void clearToken(TypedIOPort port) throws NoTokenException, IllegalActionException {
        port.get(0);
    }
    
    /** Return the (presumably Settable) attribute modified by this
     *  actor.  This is the attribute in the container of this actor
     *  with the name given by the variableName attribute.  If no such
     *  attribute is found, then this method creates a new variable in
     *  the actor's container with the correct name.  This method
     *  gets write access on the workspace.
     *  
     *  Modified from ptolemy.actor.lib.SetVariable
     *  
     *  @exception IllegalActionException If the variable cannot be found.
     *  @return The attribute modified by this actor.
     */
    private Attribute getModifiedVariable(String variableNameValue) throws IllegalActionException {
    	if (attributeVersion.containsKey(variableNameValue)) {
	        if (_workspace.getVersion() == attributeVersion.get(variableNameValue)) {
	            return attribute.get(variableNameValue);
	        }
    	}
        NamedObj container = getContainer();

        if (container == null) {
            throw new IllegalActionException(this, "No container.");
        }

        attribute.put(variableNameValue, null);

        if (!variableNameValue.equals("")) {
            // Look for the variableName anywhere in the hierarchy
        	Attribute _attribute = ModelScope.getScopedAttribute(null, container,
                    variableNameValue);
            if (_attribute == null) {
                try {
                    workspace().getWriteAccess();

                    // container might be null, so create the variable
                    // in the container of this actor.
                    _attribute = new Variable(getContainer(), variableNameValue);
                } catch (IllegalActionException ex) {
                    throw new IllegalActionException(this, ex,
                            "Failed to create Variable \"" + variableNameValue
                            + "\" in " + getContainer().getFullName()
                            + ".");
                } catch (NameDuplicationException ex) {
                    throw new InternalErrorException(ex);
                } finally {
                    workspace().doneWriting();
                }
            }
            attributeVersion.put(variableNameValue, _workspace.getVersion());
            attribute.put(variableNameValue, _attribute);
        }
        return attribute.get(variableNameValue);
    }
    
    /** Set the value of the associated container's variable.
     * 
     * Modified from ptolemy.actor.lib.SetVariable
     * 
     *  @param variableNameValue The name of the variable to set.
     *  @param value The new value.
     *  @throws IllegalActionException
     */
    private void setVariable(String variableNameValue, Token value) throws IllegalActionException {
        Attribute variable = getModifiedVariable(variableNameValue);

        if (variable instanceof Variable) {
            Token oldToken = ((Variable) variable).getToken();

            if (oldToken == null || !oldToken.equals(value)) {
                ((Variable) variable).setToken(value);

                // NOTE: If we don't call validate(), then the
                // change will not propagate to dependents.
                ((Variable) variable).validate();
            }
        } else if (variable instanceof Settable) {
            ((Settable) variable).setExpression(value.toString());

            // NOTE: If we don't call validate(), then the
            // change will not propagate to dependents.
            ((Settable) variable).validate();
        } else {
            throw new IllegalActionException(this,
                    "Cannot set the value of the variable " + "named: "
                            + variableNameValue);
        }
    }
    
    /**
     * 
     * @param variableNameValue The name of the variable to get.
     * @return The value.
     * @throws IllegalActionException
     */
    private Token getVariable(String variableNameValue) throws IllegalActionException {
    	Attribute variable = getModifiedVariable(variableNameValue);
    	
    	return ((Variable) variable).getToken();
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
}


