package ajdurant.wsn.actor;

import java.util.HashMap;

import ptolemy.actor.NoTokenException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.Dictionary;
import ptolemy.data.Token;
import ptolemy.data.UnionToken;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.type.Type;
import ptolemy.data.type.UnionType;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

public class NodeProcessor extends TypedAtomicActor {

    NodeProcessor(CompositeEntity container, String name)
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
        new SingletonParameter(toComm, "_showName").setExpression("true");
        
        toSens = new TypedIOPort(this, "toSens", false, true);
        new SingletonParameter(toSens, "_showName").setExpression("true");
        
        toMotion = new TypedIOPort(this, "toMotion", false, true);
        new SingletonParameter(toMotion, "_showName").setExpression("true");
        
     // Set Type constraints
        toComm.setTypeSameAs(fromComm);
        
        neighbours = new HashMap<String, Token>();
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
    
    private HashMap<String, Token> neighbours;
    
    private String[] labelsDataMessage = {
        "nodeID",
        "sensorData",
        "sensorTime"
    };
    private Type[] typesDataMessage = {
        BaseType.INT,
        BaseType.DOUBLE,
        BaseType.DOUBLE
    };
    private RecordType typeDataMessage = new RecordType(labelsDataMessage, typesDataMessage);
    
    private String[] labelsHeartBeat = {
        "count",
        "hopCount",
        "location",
        "motion",
        "nodeID"
    };
    private Type[] typesHeartBeat = {
        BaseType.INT,
        BaseType.INT,
        new ArrayType(BaseType.DOUBLE, 2),
        BaseType.BOOLEAN,
        BaseType.INT
    };
    private RecordType typeRecordHeartBeat = new RecordType(labelsHeartBeat, typesHeartBeat);
    
    private String[] labelsCommUnion = {
        "DataMessage",
        "HeartBeat"
    };
    private Type[] typesCommUnion = {
        typeDataMessage,
        typeRecordHeartBeat
    };
    
    private UnionType typeUnionCommUnion = new UnionType(labelsCommUnion, typesCommUnion);
    
    private String[] labelsComm = {
        "messageType",
        "messageData"
    };
    private Type[] typesComm = {
        BaseType.INT,
        typeUnionCommUnion
    };
    private RecordType typeRecordComm = new RecordType(labelsComm, typesComm);
    
    
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
        
        // Initialise objects.
        newObject.neighbours = new HashMap<String, Token>();

        return newObject;
    }
    
    @Override
    public void fire() throws IllegalActionException
    {
        super.fire();
        
        if (fromComm.getWidth() > 0 && fromComm.hasToken(0)) {
            
        }
        if (fromSens.getWidth() > 0 && fromSens.hasToken(0)) {
            
        }
        if (triggerHeartBeat.getWidth() > 0 && triggerHeartBeat.hasToken(0)) {
            clearToken(triggerHeartBeat);
            
        }
        if (triggerNeighbourCheck.getWidth() > 0 && triggerNeighbourCheck.hasToken(0)) {
            clearToken(triggerNeighbourCheck);
            
        }
        if (triggerSensorRead.getWidth() > 0 && triggerSensorRead.hasToken(0)) {
            clearToken(triggerSensorRead);
            
        }
        
    }
    
    private void clearToken(TypedIOPort port) throws NoTokenException, IllegalActionException {
        port.get(0);
    }
}


