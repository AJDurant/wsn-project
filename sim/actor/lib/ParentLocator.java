package ajdurant.wsn.actor.lib;

import ptolemy.domains.wireless.lib.Locator;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.Location;
import ptolemy.kernel.util.NameDuplicationException;

public class ParentLocator extends Locator {

	public ParentLocator(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
	}
	
	@Override
	protected double[] _getLocation() throws IllegalActionException {
        Location locationAttribute = (Location) getContainer().getAttribute("_location",
                Location.class);

        if (locationAttribute == null) {
            throw new IllegalActionException(this,
                    "Cannot find a _location attribute of class Location.");
        }
        
        return locationAttribute.getLocation();
    }

}
