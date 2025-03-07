package com.autobook.storage.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.autobook.storage.dto.AssetDto;

public interface AssetService {
    AssetDto saveAsset(MultipartFile file, String name, String tags, Long userId);
    AssetDto getAssetById(Long id);
    List<AssetDto> getAssetsByUserId(Long userId);
    List<AssetDto> getAssetsByUserIdAndTag(Long userId, String tag);
    AssetDto updateAsset(Long id, String name, String tags);
    void deleteAsset(Long id);
    byte[] downloadAsset(Long id);
    String getAssetDownloadUrl(Long id);
}