package com.obzen.stream.consumer;

import com.owlab.restful.weakrest.WeakRestClient;
import com.owlab.restful.weakrest.StreamReader;

public class TweetSamplesReader {
    private static final String endPoint = "https://stream.twitter.com/1.1/statuses/sample.json";

    public static void main(String[] args) {

        try {
        WeakRestClient.get(endPoint)
            .header("Authorization: OAuth oauth_consumer_key","MQcqpToGyepDxGcP2hUhOjJdL")
            .header("oauth_nonce", "c1af95b90634900b8c7334b461e24d3e")
            .header("oauth_signature","V39Nvi2QAmvc2d3wlsZ9%2FIdjPL4%3D")
            .header("oauth_signature_method","HMAC-SHA1")
            .header("oauth_timestamp","1452320364")
            .header("oauth_token","47900709-3b1MfNCvMaRHSjBXHENi6yskHRLvJOoYub7sbpI3B")
            .header("oauth_version","1.0")
            .queryString("delimited", "length")
            .execute(line -> {
                System.out.println(line);
            });

        } catch(Exception e) {
            e.printStackTrace();
        }
    }


}
