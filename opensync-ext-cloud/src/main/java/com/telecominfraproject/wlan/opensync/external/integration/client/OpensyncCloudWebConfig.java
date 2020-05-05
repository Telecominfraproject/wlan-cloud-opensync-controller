package com.telecominfraproject.wlan.opensync.external.integration.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;


/**
 * @author dtoptygin
 *
 */
@Configuration
//@EnableWebMvc - DTOP: do not use this, it will break mapping for index.html file
// see http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-developing-web-applications.html#boot-features-spring-mvc-auto-configuration
public class OpensyncCloudWebConfig extends WebMvcConfigurerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncCloudWebConfig.class);

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        //this is needed so that servlets can consume and produce JSON objects with (and without) _type parameters
        LOG.info("extending MessageConverters to understand KDC BaseJsonModel and its descendants");
        for(HttpMessageConverter<?> c: converters){
            if(c instanceof MappingJackson2HttpMessageConverter){
                BaseJsonModel.registerAllSubtypes(((MappingJackson2HttpMessageConverter)c).getObjectMapper());
            }
        }
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // This is needed so that @RequestParam annotations in the servlet
        // methods can be used with BaseJsonModel and its descendants and its
        // collections.

        //Use GenericlConverter here, simple one does not work
        LOG.info("Adding custom converters to process KDC BaseJsonModel and its descendants");

        registry.addConverter(new OpensyncCloudWebGenericConverter());
    }

}
