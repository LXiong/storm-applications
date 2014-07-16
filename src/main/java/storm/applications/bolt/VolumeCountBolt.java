package storm.applications.bolt;

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang3.mutable.MutableLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import storm.applications.constants.LogProcessingConstants.Conf;
import storm.applications.constants.LogProcessingConstants.Field;
import storm.applications.util.DateUtils;

/**
 * This bolt will count number of log events per minute
 */
public class VolumeCountBolt extends AbstractBolt {
    private static final Logger LOG = LoggerFactory.getLogger(VolumeCountBolt.class);
    
    private CircularFifoBuffer buffer;
    private Map<Long, MutableLong> counts;

    @Override
    public void initialize() {
        int windowSize = config.getInt(Conf.VOLUME_COUNTER_WINDOW, 60);
        
        buffer = new CircularFifoBuffer(windowSize);
        counts = new HashMap<>(windowSize);
    }

    @Override
    public void execute(Tuple tuple) {
        long minute = tuple.getLongByField(Field.TIMESTAMP_MINUTES);
        
        MutableLong count = counts.get(minute);
        
        if (count == null) {
            if (buffer.isFull()) {
                long oldMinute = (Long) buffer.remove();
                counts.remove(oldMinute);
            }
            
            count = new MutableLong(1);
            counts.put(minute, count);
            buffer.add(minute);
        } else {
            count.increment();
        }
        
        collector.emit(new Values(minute, count.longValue()));
    }

    @Override
    public Fields getDefaultFields() {
        return new Fields(Field.TIMESTAMP_MINUTES, Field.COUNT);
    }
}
