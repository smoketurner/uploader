/**
 * Copyright 2016 Smoke Turner, LLC.
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

import java.io.File;
import java.security.cert.CertificateException;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import com.smoketurner.uploader.config.NettyConfiguration;
import com.smoketurner.uploader.core.Uploader;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.timeout.IdleStateHandler;

public class UploadInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(UploadInitializer.class);
    private static final int READER_IDLE_SECONDS = 60;
    private final NettyConfiguration configuration;
    private final UploadHandler uploadHandler;
    private final SslContext sslCtx;
    private final AccessControlListFilter ipFilter;
    private final long maxLength;
    private final long maxUploadBytes;

    /**
     * Constructor
     *
     * @param configuration
     *            Netty configuration
     * @param uploader
     *            AWS S3 uploader
     * @param maxUploadBytes
     *            Maximum size of S3 upload in bytes
     */
    public UploadInitializer(@Nonnull final NettyConfiguration configuration,
            @Nonnull final Uploader uploader, final long maxUploadBytes) {

        this.configuration = Objects.requireNonNull(configuration);
        this.sslCtx = getSslContext();

        this.maxLength = configuration.getMaxLength().toBytes();
        this.maxUploadBytes = maxUploadBytes;

        // handlers
        this.uploadHandler = new UploadHandler(
                Objects.requireNonNull(uploader));

        // filters
        if (configuration.getIpFilters().count() > 0) {
            this.ipFilter = new AccessControlListFilter(
                    configuration.getIpFilters());
        } else {
            this.ipFilter = null;
        }
    }

    @Override
    public void initChannel(final SocketChannel ch) throws Exception {
        final ChannelPipeline p = ch.pipeline();

        // add the IP ACL filter first
        if (ipFilter != null) {
            p.addLast("acl", ipFilter);
        }

        if (sslCtx != null) {
            if (configuration.isClientAuth()) {
                final SSLEngine engine = sslCtx.newEngine(ch.alloc());
                engine.setUseClientMode(false);
                engine.setNeedClientAuth(true);

                p.addLast("ssl", new SslHandler(engine));
            } else {
                p.addLast("ssl", sslCtx.newHandler(ch.alloc()));
            }
        }

        // removes idle connections after 60s
        p.addLast("idleStateHandler",
                new IdleStateHandler(READER_IDLE_SECONDS, 0, 0));

        // authenticate via an ACL and mutual certificates
        p.addLast("auth", new AuthHandler(configuration.isClientAuth()));

        // check to see if the data stream is gzipped or not
        // p.addLast("gzipDetector", new OptionalGzipHandler());

        // break each data chunk by newlines
        p.addLast("line", new LineBasedFrameDecoder(Ints.checkedCast(maxLength),
                true, true));

        // convert each data chunk into a byte array
        p.addLast("decoder", new ByteArrayDecoder());

        // batch and compress chunks of data up to maxUploadBytes
        p.addLast("batcher", new BatchHandler(maxUploadBytes));

        // upload the batch to S3
        p.addLast("uploader", uploadHandler);
    }

    /**
     * Construct an {@link SslContext} from the configuration
     *
     * @return SslContext or null
     */
    private SslContext getSslContext() {
        if (!configuration.isSsl()) {
            LOGGER.warn("SSL DISABLED");
            return null;
        }

        if (configuration.isSelfSignedCert()) {
            try {
                final SelfSignedCertificate ssc = new SelfSignedCertificate();
                final SslContext sslCtx = SslContextBuilder
                        .forServer(ssc.certificate(), ssc.privateKey()).build();
                LOGGER.info("SSL ENABLED (using self-signed certificate)");
                return sslCtx;
            } catch (CertificateException | SSLException e) {
                LOGGER.warn(
                        "SSL DISABLED: Unable to generate self-signed certificate",
                        e);
                return null;
            }
        }

        if (!Strings.isNullOrEmpty(configuration.getKeyCertChainFile())
                && !Strings.isNullOrEmpty(configuration.getKeyFile())) {

            final SslContextBuilder builder = SslContextBuilder.forServer(
                    new File(configuration.getKeyCertChainFile()),
                    new File(configuration.getKeyFile()),
                    configuration.getKeyPassword());

            if (configuration.isClientAuth() && !Strings.isNullOrEmpty(
                    configuration.getTrustCertCollectionFile())) {
                builder.trustManager(
                        new File(configuration.getTrustCertCollectionFile()));
            }

            try {
                final SslContext sslCtx = builder.build();
                LOGGER.info(
                        "SSL ENABLED (certificate: {}, key: {}, trust store: {})",
                        configuration.getKeyCertChainFile(),
                        configuration.getKeyFile(),
                        configuration.getTrustCertCollectionFile());
                return sslCtx;
            } catch (SSLException e) {
                LOGGER.error("SSL DISABLED: Unable to create SSL context", e);
            }
        }
        return null;
    }
}
