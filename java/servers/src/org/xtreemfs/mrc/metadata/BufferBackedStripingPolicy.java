/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;

import java.nio.ByteBuffer;

public class BufferBackedStripingPolicy extends BufferBackedMetadata implements StripingPolicy {
    
    private static final int SIZE_INDEX      = 0;

    private static final int WIDTH_INDEX     = 4;

    private static final int PARITY_INDEX    = 8;

    private static final int EC_QUORUM_INDEX = 12;

    private static final int PATTERN_INDEX   = 16;

    
    private String           pattern;
    
    private int              stripeSize;
    
    private int              width;
    
    private int              parity;

    private int              ec_quorum;

    public BufferBackedStripingPolicy(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }
    
    public BufferBackedStripingPolicy(byte[] buffer, int offset, int len) {
        
        super(buffer, offset, len);
        
        this.pattern = new String(buffer, offset + PATTERN_INDEX, len - PATTERN_INDEX);
        
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset + SIZE_INDEX, Integer.SIZE / 8);
        this.stripeSize = tmp.getInt();
        
        tmp = ByteBuffer.wrap(buffer, offset + WIDTH_INDEX, Integer.SIZE / 8);
        this.width = tmp.getInt();

        tmp = ByteBuffer.wrap(buffer, offset + PARITY_INDEX, Integer.SIZE / 8);
        this.parity = tmp.getInt();

        tmp = ByteBuffer.wrap(buffer, offset + EC_QUORUM_INDEX, Integer.SIZE / 8);
        this.ec_quorum = tmp.getInt();
    }
    
    public BufferBackedStripingPolicy(String pattern, int stripeSize, int width) {
        this(pattern, stripeSize, width, 0, 0);
    }

    public BufferBackedStripingPolicy(String pattern, int stripeSize, int width, int parity, int ec_quorum) {
        super(null, 0, 0);
        
        len = pattern.getBytes().length + 16;
        buffer = new byte[len];
        ByteBuffer tmp = ByteBuffer.wrap(buffer);
        tmp.putInt(stripeSize).putInt(width).putInt(parity).putInt(ec_quorum).put(pattern.getBytes());
        
        this.pattern = pattern;
        this.stripeSize = stripeSize;
        this.width = width;
        this.parity = parity;
        this.ec_quorum = ec_quorum;
    }
    
    public boolean equals(StripingPolicy pol) {
        return toString().equals(pol.toString());
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public int getStripeSize() {
        return stripeSize;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getParityWidth() {
        return parity;
    }

    @Override
    public int getECWriteQuorum() {
        return ec_quorum;
    }
}