package com.jokimina.misc.apollo;

import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.client.ApolloOpenApiClient;
import com.ctrip.framework.apollo.openapi.client.exception.ApolloOpenApiException;
import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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
        final List<String> apps = Arrays.asList("sc-aql");
        final List<String> envs = Arrays.asList("DEV", "UAT", "PRO");
//        final String env = "PRO";
        final String sClusterName = "indonesia";
        final String dClusterName = "singapore";
        ApolloOpenApiClient client = ApolloOpenApiClient.newBuilder().withPortalUrl(url).withToken(token).build();
        envs.stream().forEach(env ->
                apps.stream().forEach(appId ->
                        compareNamespaceItems(client, env, appId)
                )
        );
//        apps.stream().forEach(appId -> {
//            syncCluster(client, appId, env, sClusterName, dClusterName);
//        });
    }

    static void compareNamespaceItems(ApolloOpenApiClient client, String env, String appid) {
        List<String> applicationItemKeys = new ArrayList<>();
        List<String> otherItemKeys = new ArrayList<>();
        client.getEnvClusterInfo(appid).stream().forEach(instance -> {
                    instance.getClusters().stream().forEach(cluster -> {
                                try {
                                    client.getNamespaces(appid, env, cluster).forEach(ns ->
                                            {
                                                log.info("[{} > {} > {} --> {}] done.....", appid, env, cluster, ns.getNamespaceName());
                                                ns.getItems().forEach(item -> {
                                                    if (StringUtils.isEmpty(item.getKey())) return;
                                                    if (ns.getNamespaceName().equals("application")) {
                                                        applicationItemKeys.add(item.getKey());
                                                    } else {
                                                        otherItemKeys.add(item.getKey());
                                                    }
                                                });
                                            }
                                    );
                                } catch (Exception e) {
                                    if (!e.getCause().toString().contains("namespaces not exist")){
                                        log.error(e.getMessage(), e.getCause());
                                    }
                                } finally {
                                    applicationItemKeys.retainAll(otherItemKeys);
                                    if (applicationItemKeys.size() > 0) {
                                        log.warn("[{} > {} > {}] has repeat keys {}", appid, env, cluster, applicationItemKeys.toString());
                                    } else {
                                        log.info("[{} > {} > {}] done.....", appid, env, cluster);
                                    }
                                    applicationItemKeys.clear();
                                    otherItemKeys.clear();
                                }
                            }
                    );
                }
        );
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
