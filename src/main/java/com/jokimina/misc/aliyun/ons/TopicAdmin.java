package com.jokimina.misc.aliyun.ons;


import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.ons.model.v20170918.OnsTopicListRequest;
import com.aliyuncs.ons.model.v20170918.OnsTopicListResponse;
import com.aliyuncs.ons.model.v20170918.OnsTrendTopicInputTpsRequest;
import com.aliyuncs.ons.model.v20170918.OnsTrendTopicInputTpsResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xiaodong
 * 12/14/2018
 */
public class TopicAdmin {

    static final String ONS_REGION_ID = "mq-internet-access";
//    static final String ONS_REGION_ID = "cn-beijing";

    public static void main(String[] args) {
        String regionId = System.getProperty("regionId");
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        List<String> leisureTopics = new ArrayList<>();

        IClientProfile profile = DefaultProfile.getProfile(regionId, accessKey, secretKey);
        DefaultProfile.getProfile();
        IAcsClient iAcsClient = new DefaultAcsClient(profile);

        List<OnsTopicListResponse.PublishInfoDo> topics = getTopics(iAcsClient);
        topics.forEach(ons -> {
            String topicName = ons.getTopic();
            System.out.println("-- check ons " + topicName);
            List<OnsTrendTopicInputTpsResponse.Data.StatsDataDo> onsTps = getOnsTps(iAcsClient, topicName);
            if (!onsTps.stream().anyMatch(s -> s.getY() > 0)) {
                System.out.println("leisure topic: " + ons.getTopic());
                leisureTopics.add(ons.getTopic());
            }
        });
        System.out.println(leisureTopics + "\n" + leisureTopics.size());
    }

    private static List<OnsTopicListResponse.PublishInfoDo> getTopics(IAcsClient iAcsClient) {
        List<OnsTopicListResponse.PublishInfoDo> publishInfoDoList = null;
        OnsTopicListRequest request = new OnsTopicListRequest();
        request.setOnsRegionId(ONS_REGION_ID);
        request.setPreventCache(System.currentTimeMillis());
//        request.setTopic("XXXXXXXXXXXXX");
        try {
            OnsTopicListResponse response = iAcsClient.getAcsResponse(request);
            publishInfoDoList = response.getData();
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return publishInfoDoList;
    }

    private static List<OnsTrendTopicInputTpsResponse.Data.StatsDataDo> getOnsTps(IAcsClient iAcsClient, String topic) {
        List<OnsTrendTopicInputTpsResponse.Data.StatsDataDo> records = null;

        OnsTrendTopicInputTpsRequest request = new OnsTrendTopicInputTpsRequest();
        request.setOnsRegionId(ONS_REGION_ID);
        request.setPreventCache(System.currentTimeMillis());
        request.setAcceptFormat(FormatType.JSON);
        request.setTopic(topic);
        request.setBeginTime(System.currentTimeMillis() - 7 * 24 * 3600 * 1000);
        request.setEndTime(System.currentTimeMillis());
        request.setPeriod(new Long(10));
        request.setType(0);
        try {
            OnsTrendTopicInputTpsResponse response = iAcsClient.getAcsResponse(request);
            OnsTrendTopicInputTpsResponse.Data data = response.getData();
            records = data.getRecords();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return records;
    }
}
