package com.jokimina.misc.aliyun.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xiaodong
 */
@Slf4j
public class OssAdmin {
    public static void main(String[] args) {
        String endpoint = "http://oss-cn-hangzhou.aliyuncs.com";
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        final String bucketName = "x";
        final String prefix = "x";
        final int maxKeys = 200;
        String nextMarker = null;
        ObjectListing objectListing;


        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
//        List<Bucket> buckets = ossClient.listBuckets();
//        buckets.forEach(v -> log.info((v.toString())));
        do {
            objectListing = ossClient.listObjects(new ListObjectsRequest(bucketName).withMarker(nextMarker).withMaxKeys(maxKeys).withPrefix(prefix));

            List<String> keys = new ArrayList<>();
            List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
            for (OSSObjectSummary s : sums) {
                keys.add(s.getKey());
            }
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName).withKeys(keys);
            ossClient.deleteObjects(deleteObjectsRequest);
            log.info("delete keys from bucketName={}, keys={}", bucketName, keys);

            nextMarker = objectListing.getNextMarker();

        } while (objectListing.isTruncated());
        ossClient.shutdown();
    }
}
