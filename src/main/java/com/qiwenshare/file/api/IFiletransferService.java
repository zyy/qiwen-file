package com.qiwenshare.file.api;

import com.qiwenshare.file.domain.FileBean;
import com.qiwenshare.file.domain.StorageBean;
import com.qiwenshare.file.dto.DownloadFileDTO;
import com.qiwenshare.file.dto.UploadFileDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IFiletransferService {





    /**
     * 上传文件
     * @param request 请求
     * @param UploadFileDto 文件信息
     */
    void uploadFile(HttpServletRequest request, UploadFileDTO UploadFileDto, Long userId);

    void downloadFile(HttpServletResponse httpServletResponse, DownloadFileDTO downloadFileDTO);
    void deleteFile(FileBean fileBean);
    StorageBean selectStorageBean(StorageBean storageBean);

    void insertStorageBean(StorageBean storageBean);

    void updateStorageBean(StorageBean storageBean);

    StorageBean selectStorageByUser(StorageBean storageBean);
    Long selectStorageSizeByUserId(Long userId);
}