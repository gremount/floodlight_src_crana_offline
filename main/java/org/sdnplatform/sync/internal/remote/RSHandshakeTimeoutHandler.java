/**
*    Copyright 2011,2013 Big Switch Networks, Inc. 
* 
*    Licensed under the Apache License, Version 2.0 (the "License"); you may
*    not use this file except in compliance with the License. You may obtain
*    a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*    License for the specific language governing permissions and limitations
*    under the License.
**/

package org.sdnplatform.sync.internal.remote;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

/**
 * Trigger a timeout if the bootstrap process stalls
 */
public class RSHandshakeTimeoutHandler extends ChannelInboundHandlerAdapter {
    
    final Timer timer;
    final long timeoutNanos;
    volatile Timeout timeout;
    final RemoteSyncChannelHandler channelHandler;
    
    public RSHandshakeTimeoutHandler(RemoteSyncChannelHandler channelHandler,
                                     Timer timer,
                                     long timeoutSeconds) {
        super();
        this.channelHandler = channelHandler;
        this.timer = timer;
        this.timeoutNanos = TimeUnit.SECONDS.toNanos(timeoutSeconds);
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx)
            throws Exception {
        if (timeoutNanos > 0) {
            timeout = timer.newTimeout(new HandshakeTimeoutTask(ctx), 
                                       timeoutNanos, TimeUnit.NANOSECONDS);
        }
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx)
            throws Exception {
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
        super.channelInactive(ctx);
    }
    
    private final class HandshakeTimeoutTask implements TimerTask {

        private final ChannelHandlerContext ctx;

        HandshakeTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled()) {
                return;
            }

            if (!ctx.channel().isOpen()) {
                return;
            }
            if (channelHandler.syncManager.ready == false)
                ctx.channel().disconnect();
        }
    }
}
