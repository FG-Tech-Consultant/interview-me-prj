package com.interviewme.exports.service;

public interface FileStorageService {

    String store(Long tenantId, String fileName, byte[] content);

    byte[] retrieve(String fileUrl);

    void delete(String fileUrl);
}
