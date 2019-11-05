package com.jokimina.misc.aliyun.log;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.Project;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.http.client.ClientConfiguration;
import com.aliyun.openservices.log.request.DeleteLogStoreRequest;
import com.aliyun.openservices.log.request.GetLogsRequest;
import com.aliyun.openservices.log.request.ListLogStoresRequest;
import com.aliyun.openservices.log.response.DeleteLogStoreResponse;
import com.aliyun.openservices.log.response.GetLogsResponse;
import com.aliyun.openservices.log.response.ListLogStoresResponse;
import com.aliyun.openservices.log.response.ListProjectResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 *  清理超过90没有日志的logstore
 */
@Slf4j
public class LogAdmin {

    public static void main(String[] args) throws LogException {
        String regionId = System.getProperty("regionId");
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        String endpoint = "cn-beijing.log.aliyuncs.com";
//        String project = "acslog-project-c3f555f50c-rondv";


        Client client = new Client(endpoint, accessKey, secretKey, new ClientConfiguration());
        List<String> projects = listProjects(client);
        for (String project : projects) {
            List<String> logstores = listLogstore(client, project);
            log.info("start process project {}", project);
            for (String logstore : logstores) {
                int line = getLogstore30dayCount(client, project, logstore);
                if (line == 0) {
                    deleteLogstore(client, project, logstore);
                }
            }
            log.info("process project {} done.", project);
        }
    }

    static List<String> listLogstore(Client client, String project) throws LogException {
        ListLogStoresRequest listLogStoresRequest = new ListLogStoresRequest(project, 0, 1000, "");
        ListLogStoresResponse listLogStoresResponse = client.ListLogStores(listLogStoresRequest);
        return listLogStoresResponse.GetLogStores();
    }

    static void deleteLogstore(Client client, String project, String logstore) throws LogException {
        DeleteLogStoreRequest deleteLogStoreRequest = new DeleteLogStoreRequest(project, logstore);
        DeleteLogStoreResponse deleteLogStoreResponse = client.DeleteLogStore(deleteLogStoreRequest);
        log.info("delete logstore {} in project {}", logstore, project);
    }

    static int getLogstore30dayCount(Client client, String project, String logstore) {
        int now = (int) (System.currentTimeMillis() / 1000);
        GetLogsRequest getLogsRequest = new GetLogsRequest(project, logstore, now - 86400 * 90, now, "", "*");
        GetLogsResponse getLogsResponse = null;
        try {
            getLogsResponse = client.GetLogs(getLogsRequest);
        } catch (LogException e) {
            log.error(e.GetErrorMessage(), e.getStackTrace());
            return 1;
        }
        return getLogsResponse.GetCount();
    }

    static List<String> listProjects(Client client) throws LogException {
        ListProjectResponse listProjectResponse = client.ListProject();
        List<String> projects = listProjectResponse.getProjects()
                .stream()
                .map(Project::getProjectName)
                .collect(Collectors.toList());
        return projects;
    }

}
