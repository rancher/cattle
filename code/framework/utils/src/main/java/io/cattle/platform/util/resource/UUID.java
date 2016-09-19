package io.cattle.platform.util.resource;

import java.util.concurrent.ThreadLocalRandom;

public class UUID {
    public static java.util.UUID randomUUID() {
        byte[] randomBytes = new byte[16];
        ThreadLocalRandom.current().nextBytes(randomBytes);
        randomBytes[6]  &= 0x0f;  /* clear version        */
        randomBytes[6]  |= 0x40;  /* set to version 4     */
        randomBytes[8]  &= 0x3f;  /* clear variant        */
        randomBytes[8]  |= 0x80;  /* set to IETF variant  */

        long msb = 0;
        long lsb = 0;
        for (int i=0; i<8; i++)
            msb = (msb << 8) | (randomBytes[i] & 0xff);
        for (int i=8; i<16; i++)
            lsb = (lsb << 8) | (randomBytes[i] & 0xff);
        return new java.util.UUID(msb, lsb);
    }
}
