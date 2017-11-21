/**
 * Copyright 2017 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.uploader.handler;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.smoketurner.uploader.config.IpFilterConfiguration;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import io.netty.handler.ipfilter.RuleBasedIpFilter;

public final class AccessControlListFilter extends RuleBasedIpFilter {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AccessControlListFilter.class);

    /**
     * Constructor
     *
     * @param configuration
     *            IP access list configuration
     */
    public AccessControlListFilter(
            @Nonnull final IpFilterConfiguration configuration) {
        super(getRules(configuration.getAccept(), configuration.getReject()));
        LOGGER.info("Loaded {} IP filter(s) ({} accept, {} reject)",
                configuration.count(), configuration.getAccept().size(),
                configuration.getReject().size());
    }

    @Nullable
    @Override
    protected ChannelFuture channelRejected(ChannelHandlerContext ctx,
            InetSocketAddress remoteAddress) {
        LOGGER.warn("Rejected access from {} based on ACL", remoteAddress);
        return null;
    }

    /**
     * Convert accept and reject rules into an array of
     * {@link IpSubnetFilterRule}'s
     * 
     * @param accept
     *            IP addresses to allow access
     * @param reject
     *            IP addresses to deny access
     * @return IP subnet filter rules
     */
    public static IpSubnetFilterRule[] getRules(final Iterable<String> accept,
            final Iterable<String> reject) {
        final List<IpSubnetFilterRule> ipf = new ArrayList<>();

        for (String ip : accept) {
            try {
                ipf.add(convertIp(ip, IpFilterRuleType.ACCEPT));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Ignoring invalid ACL accept IP: {}", ip);
            }
        }

        for (String ip : reject) {
            try {
                ipf.add(convertIp(ip, IpFilterRuleType.REJECT));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Ignoring invalid ACL reject IP: {}", ip);
            }
        }
        return ipf.toArray(new IpSubnetFilterRule[ipf.size()]);
    }

    /**
     * Convert an IP address and a rule type into an {@link IpSubnetFilterRule}
     * 
     * @param ip
     *            IP address (may contain a CIDR range)
     * @param ruleType
     *            rule type, either accept or reject
     * @return ip rule filter
     * @todo this assumes IPv4 default CIDR masks for now
     */
    public static IpSubnetFilterRule convertIp(final String ip,
            final IpFilterRuleType ruleType) {
        if (!ip.contains("/")) {
            return new IpSubnetFilterRule(ip, 32, ruleType);
        }

        final String[] parts = ip.split("/", 2);
        if (parts.length < 2) {
            return new IpSubnetFilterRule(parts[0], 32, ruleType);
        }

        final int cidrPrefix = Integer.parseInt(parts[1]);
        return new IpSubnetFilterRule(parts[0], cidrPrefix, ruleType);
    }
}
