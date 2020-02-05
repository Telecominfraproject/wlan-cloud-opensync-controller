package ai.connectus.opensync.ovsdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

@Configuration
public class NettySslContextConfig {

    private static final Logger LOG = LoggerFactory.getLogger(NettySslContextConfig.class);

    @Bean(name="NettySslContextServer")
    public SslContext nettySslContextServer(
            @Value("${connectus.ovsdb.trustStore:/Users/dtop/Documents/certs_from_device/truststore.jks}")
            String trustStoreFileName,
            
            @Value("${connectus.ovsdb.keyStore:/Users/dtop/Documents/certs_from_device/server.p12}")
            String keyFileName,

            @Value("${connectus.ovsdb.keyStorePassword:mypassword}")
            String keyStorePassword            
            ){
        File trustStoreFile = new File(trustStoreFileName);
        File keyFile = new File(keyFileName);

        SslContext sslContext = null;
        
        try {
            char[] pwd = keyStorePassword.toCharArray();

            KeyStore ks = KeyStore.getInstance("PKCS12");
            try(InputStream is = new FileInputStream(keyFile)){
                ks.load(is, pwd);
            }
            
            for(String alias: Collections.list(ks.aliases())) {
                LOG.debug("Key Alias: {}", alias);
            }

            PrivateKey key = (PrivateKey) ks.getKey("1", pwd);
            Certificate[] chain = ks.getCertificateChain("1");
            X509Certificate[] keyCertChain = new X509Certificate[chain.length];
            int i=0;
            for(Certificate cert : chain) {
                keyCertChain[i] = (X509Certificate) cert;
                i++;
            }

            String keyPassword = null;
            SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(key, keyPassword, keyCertChain );

            KeyStore trustKs = KeyStore.getInstance("JKS");
            try(InputStream is = new FileInputStream(trustStoreFile)){
                trustKs.load(is, pwd);
            }

            List<X509Certificate> trustChain = new ArrayList<>();
            
            for(String alias: Collections.list(trustKs.aliases())) {
                LOG.debug("Trust Alias: {}", alias);
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                trustChain.add(cert);
            }
            

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
            tmf.init(trustKs);
            sslContextBuilder.trustManager(tmf);
            
            sslContext = sslContextBuilder.build();
            LOG.debug("Built ssl context");
        } catch (KeyStoreException|CertificateException|NoSuchAlgorithmException|IOException|UnrecoverableEntryException e) {
            throw new RuntimeException(e);
        }
        
        return sslContext;
    }
}
