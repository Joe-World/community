package org.wgd.community.config;


import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;

import java.net.URI;
import java.net.URISyntaxException;


@Configuration
class EsConfig {
    @Value("${spring.elasticsearch.uris}")
    private String esUrl;

    //localhost:9200 写在配置文件中就可以了
//    @Bean
//    RestHighLevelClient client() {
//        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
//                .connectedTo(esUrl)//elasticsearch地址
//                .build();
//
//        return RestClients.create(clientConfiguration).rest();
//    }
    @Bean(name = "restHighLevelClient")
    public RestHighLevelClient client() {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        RestClientBuilder restClientBuilder = getRestClientBuilder(esUrl)
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.disableAuthCaching();
                    return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                });
        return new RestHighLevelClient(restClientBuilder);
    }
    /**
     * Gets rest client builder.
     * @param esUrl the es url
     * @return the rest client builder
     */
    public static RestClientBuilder getRestClientBuilder(String esUrl) {
        return RestClient.builder(createHttpHost(URI.create(esUrl)));
    }
    /**
     * Create http host
     *
     * @param uri the uri
     * @return the http host
     */
    public static HttpHost createHttpHost(URI uri) {
        if (StringUtils.isEmpty(uri.getUserInfo())) {
            return HttpHost.create(uri.toString());
        }
        try {
            return HttpHost.create(new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(),
                    uri.getQuery(), uri.getFragment()).toString());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

