package ajdurant.wsn.lib;

import java.util.List;

import ptolemy.data.RecordToken;
import ptolemy.domains.wireless.kernel.WirelessIOPort;
import ptolemy.domains.wireless.lib.PowerLossChannel;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class PowerLossChannelNoCache extends PowerLossChannel {

    public PowerLossChannelNoCache(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
    }
    
    @Override
    protected List _receiversInRange(WirelessIOPort sourcePort,
            RecordToken properties) throws IllegalActionException {
        
        // Invalidate cache before it is used
        _receiversInRangeCacheValid = false;
        
        return super._receiversInRange(sourcePort, properties);
    }

}
