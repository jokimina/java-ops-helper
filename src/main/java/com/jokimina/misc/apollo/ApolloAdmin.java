package com.jokimina.misc.apollo;

import com.ctrip.framework.apollo.openapi.client.ApolloOpenApiClient;
import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * @author xiaodong
 * 12/14/2018
 */
@Slf4j
public class ApolloAdmin {
    public static void main(String[] args) {
        String url = System.getProperty("url");
        String token = System.getProperty("token");
        final List<String> apps = Arrays.asList("sc-collection");
        final String env = "PRO";
        final String sClusterName = "indonesia";
        final String dClusterName = "singapore";
        ApolloOpenApiClient client = ApolloOpenApiClient.newBuilder().withPortalUrl(url).withToken(token).build();
        apps.stream().forEach(appId -> {
            syncCluster(client, appId, env, sClusterName, dClusterName);
        });
    }

    static void syncCluster(ApolloOpenApiClient client, String appId, String env, String sClusterName, String dClusterName) {
        List<OpenNamespaceDTO> sNamespaces = client.getNamespaces(appId, env, sClusterName);
        sNamespaces.stream().forEach(sNamespace -> {
            OpenNamespaceDTO dNamespace = client.getNamespace(appId, env, dClusterName, sNamespace.getNamespaceName());
            log.info(String.format("Sync env %s app %s cluster %s namespace %s -> Sync env %s app %s cluster %s namespace %s", env, appId, sClusterName, sNamespace.getNamespaceName(), env, appId, dClusterName, dNamespace.getNamespaceName()));
            sNamespace.getItems().stream().forEach(item -> {
                try {
                    log.info(item.getKey());
                    if (!Strings.isNullOrEmpty(item.getKey()) && !Strings.isNullOrEmpty(item.getValue())) {
                        try {
                            client.createItem(appId, env, dClusterName, dNamespace.getNamespaceName(), item);
                        } catch (Exception e) {
                            client.updateItem(appId, env, dClusterName, dNamespace.getNamespaceName(), item);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            NamespaceReleaseDTO namespaceReleaseDTO = new NamespaceReleaseDTO();
            namespaceReleaseDTO.setReleaseTitle("Release by java openapi");
            namespaceReleaseDTO.setReleaseComment("Initial for " + dClusterName);
            namespaceReleaseDTO.setReleasedBy("apollo");
            client.publishNamespace(appId, env, dClusterName, dNamespace.getNamespaceName(), namespaceReleaseDTO);
        });
    }
}
