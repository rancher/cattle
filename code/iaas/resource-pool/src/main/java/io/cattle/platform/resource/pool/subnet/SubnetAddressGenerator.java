package io.cattle.platform.resource.pool.subnet;

import java.util.Random;

import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.resource.pool.PooledResourceItemGenerator;
import io.cattle.platform.util.net.NetUtils;

public class SubnetAddressGenerator implements PooledResourceItemGenerator {

    private static final Random RANDOM = new Random();

    Subnet subnet;
    long min, max, current, start;
    int length, count = 0;
    boolean first = true;

    public SubnetAddressGenerator(Subnet subnet) {
        this(subnet.getStartAddress(), subnet.getEndAddress());

        this.subnet = subnet;
    }

    public SubnetAddressGenerator(String start, String end) {
        super();

        min = NetUtils.ip2Long(start);
        max = NetUtils.ip2Long(end);
        length = (int)(max - min + 1);
        this.start = current = min + RANDOM.nextInt(length);
    }

    @Override
    public boolean hasNext() {
        return count < length;
    }

    @Override
    public String next() {
        String next = NetUtils.long2Ip(current);

        if ( ++current > max ) {
            current = min;
        }

        count++;
        return next;
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
