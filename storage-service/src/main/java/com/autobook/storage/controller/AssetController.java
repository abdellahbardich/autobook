package com.autobook.storage.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.autobook.storage.dto.AssetDto;
import com.autobook.storage.service.AssetService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<AssetDto> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam("userId") Long userId) {

        AssetDto assetDto = assetService.saveAsset(file, name, tags, userId);
        return new ResponseEntity<>(assetDto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetDto> getAsset(@PathVariable Long id) {
        AssetDto assetDto = assetService.getAssetById(id);
        return ResponseEntity.ok(assetDto);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AssetDto>> getAssetsByUserId(@PathVariable Long userId) {
        List<AssetDto> assets = assetService.getAssetsByUserId(userId);
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/user/{userId}/tag/{tag}")
    public ResponseEntity<List<AssetDto>> getAssetsByUserIdAndTag(
            @PathVariable Long userId,
            @PathVariable String tag) {
        List<AssetDto> assets = assetService.getAssetsByUserIdAndTag(userId, tag);
        return ResponseEntity.ok(assets);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetDto> updateAsset(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "tags", required = false) String tags) {

        AssetDto updatedAssetDto = assetService.updateAsset(id, name, tags);
        return ResponseEntity.ok(updatedAssetDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadAsset(@PathVariable Long id) {
        byte[] assetData = assetService.downloadAsset(id);
        AssetDto assetDto = assetService.getAssetById(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(assetDto.getContentType()));
        headers.setContentDispositionFormData("attachment", assetDto.getName());
        headers.setContentLength(assetData.length);

        return new ResponseEntity<>(assetData, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/url")
    public ResponseEntity<String> getAssetUrl(@PathVariable Long id) {
        String url = assetService.getAssetDownloadUrl(id);
        return ResponseEntity.ok(url);
    }
}