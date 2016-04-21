package ajdurant.wsn.lib;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.DoubleToken;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.data.type.UnionType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public abstract class WSNActor extends ModelDataActor {
    
    // Outputs
    public TypedIOPort consumption;
    
    protected abstract void consumePower() throws IllegalActionException;

    public WSNActor(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
        super(container, name);
        
        // Outputs
        consumption = new TypedIOPort(this, "consumption", false, true);
        consumption.setTypeEquals(BaseType.DOUBLE);
        new SingletonParameter(consumption, "_showName").setExpression("true");
    }
    
    /**
     * Send consumption to power module.
     * 
     * @throws IllegalActionException
     */
    protected void consumePower(String rate) throws IllegalActionException {
        DoubleToken consumptionRate = (DoubleToken) getVariable(rate);
        consumePower(consumptionRate);
    }
    
    /**
     * Send consumption to power module.
     * 
     * @throws IllegalActionException
     */
    protected void consumePower(Double rate) throws IllegalActionException {
        DoubleToken consumptionRate = new DoubleToken(rate);
        consumePower(consumptionRate);
    }
    
    /**
     * Send consumption to power module.
     * 
     * @throws IllegalActionException
     */
    protected void consumePower(DoubleToken rate) throws IllegalActionException {
        consumption.send(0, rate);
    }
    
    
    // Types
    protected static ArrayType typeLocation = new ArrayType(BaseType.DOUBLE, 2);
    protected static String[] labelsDataMessage = {
            "nodeID",
            "sensorData",
            "sensorTime"
        };
    protected static Type[] typesDataMessage = {
            BaseType.INT,
            BaseType.DOUBLE,
            BaseType.DOUBLE
        };
    protected static RecordType typeDataMessage = new RecordType(labelsDataMessage, typesDataMessage);
    protected static String[] labelsHeartbeat = {
            "count",
            "hopCount",
            "aliveNeighbours",
            "currentLocation",
            "targetLocation",
            "motion",
            "nodeID"
        };
    protected static Type[] typesHeartbeat = {
            BaseType.INT,
            BaseType.INT,
            BaseType.INT,
            typeLocation,
            typeLocation,
            BaseType.BOOLEAN,
            BaseType.INT
        };
    protected static RecordType typeRecordHeartbeat = new RecordType(labelsHeartbeat, typesHeartbeat);
    protected static String[] labelsCommUnion = {
            "DataMessage",
            "Heartbeat"
        };
    protected static Type[] typesCommUnion = {
            typeDataMessage,
            typeRecordHeartbeat
        };
    protected static UnionType typeUnionCommUnion = new UnionType(labelsCommUnion, typesCommUnion);
    protected static String[] labelsComm = {
            "messageType",
            "messageData"
        };
    protected static Type[] typesComm = {
            BaseType.INT,
            typeUnionCommUnion
        };
    protected static RecordType typeRecordComm = new RecordType(labelsComm, typesComm);
    protected static String[] labelsNeighbourState = {
            "updateTime",
            "alive"
        };
    protected static Type[] typesNeighbourState = {
            BaseType.DOUBLE,
            BaseType.BOOLEAN
        };
    protected static RecordType typeRecordNeighbourState = new RecordType(labelsNeighbourState, typesNeighbourState);

}
