package com.telecominfraproject.wlan.opensync.ovsdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Collections;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

@Configuration
public class NettySslContextConfig {

    private static final Logger LOG = LoggerFactory.getLogger(NettySslContextConfig.class);

    @Bean(name="NettySslContextServer")
    public SslContext nettySslContextServer(
            @Value("${connectus.ovsdb.trustStore:/opt/tip-wlan/certs/truststore.jks}")
            String trustStoreFileName,
            
            @Value("${connectus.ovsdb.keyStore:/opt/tip-wlan/certs/server.pkcs12}")
            String keyFileName,

            @Value("${connectus.ovsdb.keyStorePassword:mypassword}")
            String keyStorePassword,
            
            @Value("${connectus.ovsdb.trustStorePassword:mypassword}")
            String trustStorePassword            
            ){
        File trustStoreFile = new File(trustStoreFileName);
        File keyFile = new File(keyFileName);

        SslContext sslContext = null;
        
        try {
            char[] keyStorePwd = keyStorePassword.toCharArray();

            KeyStore ks = KeyStore.getInstance("PKCS12");
            //KeyStore ks = KeyStore.getInstance("JKS");
            try(InputStream is = new FileInputStream(keyFile)){
                ks.load(is, keyStorePwd);
            }
            
            for(String alias: Collections.list(ks.aliases())) {
                LOG.debug("Key Alias: {}", alias);
            }
            
            char[] trustStorePwd = trustStorePassword.toCharArray();
            
            KeyStore trustKs = KeyStore.getInstance("JKS");
            try(InputStream is = new FileInputStream(trustStoreFile)){
                trustKs.load(is, trustStorePwd);
            }

            for(String alias: Collections.list(trustKs.aliases())) {
                LOG.debug("Trust Alias: {}", alias);
            }
            
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(ks, keyStorePwd);
            
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
            trustManagerFactory.init(trustKs);

            SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(keyManagerFactory);
            sslContextBuilder.trustManager(trustManagerFactory);
            sslContextBuilder.clientAuth(ClientAuth.REQUIRE);
            //sslContextBuilder.protocols("TLSv1.2");
            sslContextBuilder.startTls(true);
            
            sslContext = sslContextBuilder.build();
            
            LOG.debug("Built ssl context");
        } catch (KeyStoreException|CertificateException|NoSuchAlgorithmException|IOException|UnrecoverableEntryException e) {
            throw new RuntimeException(e);
        }
        
        return sslContext;
    }
}
