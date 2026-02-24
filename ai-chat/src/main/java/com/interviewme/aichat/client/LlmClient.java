package com.interviewme.aichat.client;

public interface LlmClient {
    LlmResponse complete(LlmRequest request);
    String getModel();
    String getProvider();
}
