package ajdurant.wsn.actor;

import java.util.ArrayList;

import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class NodeProcessorRIM extends NodeProcessor {

    public NodeProcessorRIM(CompositeEntity container, String name)
        throws NameDuplicationException, IllegalActionException
    {
        super(container, name);
    }
    
    private boolean alreadyMoved;
    
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
        
        alreadyMoved = false;
    }
    
    /**
     * Handler for when a periodic neighbour check is triggered.
     * 
     * This function implements RIM.
     * 
     * @throws IllegalActionException
     */
    @Override
    protected void neighbourCheck() throws IllegalActionException {

    	double currentTime = getDirector().getModelTime().getDoubleValue();
    	
    	neighbours.forEach((node, nodeData) -> {
            RecordToken nodeRecord = (RecordToken) nodeData;
            
            double nodeUpdateTime = ((DoubleToken) nodeRecord.get("updateTime")).doubleValue();
            boolean nodeAlive = ((BooleanToken) nodeRecord.get("alive")).booleanValue();
            boolean nodeInMotion = ((BooleanToken) nodeRecord.get("motion")).booleanValue();
            ArrayToken nodeLocation = (ArrayToken) nodeRecord.get("location");
    		
            if ((currentTime >= nodeUpdateTime + heartbeatCheckPeriod)) {
                // Higher ranked node has lost connectivity.
                
                if (nodeInMotion) {
                    // Node has moved away
                    if (alreadyMoved) {
                        return;
                    }
                    
                    ArrayToken newPosition = computeNewPosition();
                    ArrayToken currentPosition = (ArrayToken) getVariable("currentLocation");
                    
                    if (!currentPosition.equals(newPosition)) {
                        try {
                            toMotion.send(0, newPosition);
                        } catch (IllegalActionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    
                } else {
                    // Node has probably died
                    try {
                        neighbours.replace(node, setNeighbourLive(nodeRecord, false));
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
    
    private RecordToken setNeighbourLive(RecordToken nodeData, boolean alive) throws IllegalActionException {
        RecordToken aliveToken;
        aliveToken = new RecordToken(new String[] { "alive" }, new Token[] { new BooleanToken(false) });
        return RecordToken.merge(aliveToken, (RecordToken) nodeData);
    }
    
    private ArrayToken computePositionFromDeadNode(ArrayToken nodeLocation) {
        ArrayToken currentLocation = (ArrayToken) getVariable("currentLocation");
        
        double coordX = ((DoubleToken) currentLocation.getElement(0)).doubleValue();
        double coordY = ((DoubleToken) currentLocation.getElement(1)).doubleValue();
        
        double nodeX = ((DoubleToken) nodeLocation.getElement(0)).doubleValue();
        double nodeY = ((DoubleToken) nodeLocation.getElement(1)).doubleValue();
        
        double distance = Math.sqrt( Math.pow((nodeX - coordX), 2) + Math.pow((nodeY - coordY), 2) );
        double radius = ((DoubleToken) getVariable("txRange")).doubleValue();
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
        ArrayToken currentLocation = (ArrayToken) getVariable("currentLocation");
        
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
        ArrayToken currentLocation = (ArrayToken) getVariable("currentLocation");
        double coordX = ((DoubleToken) currentLocation.getElement(0)).doubleValue();
        double coordY = ((DoubleToken) currentLocation.getElement(1)).doubleValue();
        
        double nodeX = ((DoubleToken) nodeLocation.getElement(0)).doubleValue();
        double nodeY = ((DoubleToken) nodeLocation.getElement(1)).doubleValue();
        
        double radius = ((DoubleToken) getVariable("txRange")).doubleValue();
        
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
        double radius = ((DoubleToken) getVariable("txRange")).doubleValue();
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
        
        double radius = ((DoubleToken) getVariable("txRange")).doubleValue();
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
        
        double radius = ((DoubleToken) getVariable("txRange")).doubleValue();
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
}


