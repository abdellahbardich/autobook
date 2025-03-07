package com.autobook.storage.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.autobook.storage.dto.AssetDto;
import com.autobook.storage.exception.ResourceNotFoundException;
import com.autobook.storage.model.Asset;
import com.autobook.storage.repository.AssetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final S3Service s3Service;

    @Override
    @Transactional
    public AssetDto saveAsset(MultipartFile file, String name, String tags, Long userId) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String s3Key = "assets/" + userId + "/" + UUID.randomUUID() + extension;

        s3Service.uploadFile(s3Key, file);

        Asset asset = Asset.builder()
                .name(name)
                .s3Key(s3Key)
                .contentType(file.getContentType())
                .size(file.getSize())
                .tags(tags)
                .userId(userId)
                .build();

        Asset savedAsset = assetRepository.save(asset);
        return mapToAssetDto(savedAsset);
    }

    @Override
    public AssetDto getAssetById(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));
        return mapToAssetDto(asset);
    }

    @Override
    public List<AssetDto> getAssetsByUserId(Long userId) {
        return assetRepository.findByUserId(userId).stream()
                .map(this::mapToAssetDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetDto> getAssetsByUserIdAndTag(Long userId, String tag) {
        return assetRepository.findByUserIdAndTagsContaining(userId, tag).stream()
                .map(this::mapToAssetDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AssetDto updateAsset(Long id, String name, String tags) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));

        asset.setName(name);
        asset.setTags(tags);

        Asset updatedAsset = assetRepository.save(asset);
        return mapToAssetDto(updatedAsset);
    }

    @Override
    @Transactional
    public void deleteAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));

        s3Service.deleteFile(asset.getS3Key());
        assetRepository.deleteById(id);
    }

    @Override
    public byte[] downloadAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));

        return s3Service.downloadFile(asset.getS3Key());
    }

    @Override
    public String getAssetDownloadUrl(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));

        return s3Service.generatePresignedUrl(asset.getS3Key(), 60).toString();  // 1 hour expiration
    }

    private AssetDto mapToAssetDto(Asset asset) {
        return AssetDto.builder()
                .id(asset.getId())
                .name(asset.getName())
                .s3Key(asset.getS3Key())
                .contentType(asset.getContentType())
                .size(asset.getSize())
                .tags(asset.getTags())
                .userId(asset.getUserId())
                .downloadUrl(s3Service.generatePresignedUrl(asset.getS3Key(), 60).toString())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }
}